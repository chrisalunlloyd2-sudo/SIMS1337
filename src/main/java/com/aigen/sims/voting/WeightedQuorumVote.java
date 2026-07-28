package com.aigen.sims.voting;

import java.util.*;

/**
 * WeightedQuorumVote — FOW-gated quorum voting with 4D time pulse.
 *
 * Each model has:
 *   - hex position (Q, R, Z)
 *   - time pulse phase (0.0–1.0), oscillates with model cycle
 *   - vote weight = 1.0 + pulse (models in-phase get bonus)
 *
 * Proposals anchored to hex coordinates. Only models within FOW_HOP
 * can see/vote. BLIND votes tracked separately.
 *
 * Quorum: visible votes ≥ quorumMin, approve ≥ approveMin.
 * 4D pulse: models with pulse phase near the proposal's time slot get 1.5× weight.
 */
public class WeightedQuorumVote {
    public enum Vote { APPROVE, REJECT, BLIND }

    public static class ModelPosition {
        public final String name;
        public final HexCoord hex;
        public volatile double pulsePhase; // 0.0–1.0, oscillates

        public ModelPosition(String name, HexCoord hex) {
            this.name = name; this.hex = hex; this.pulsePhase = 0.0;
        }
    }

    public static class Proposal {
        public final String id;
        public final String text;
        public final HexCoord hex;
        public final double timeSlot; // 0.0–1.0 preferred pulse phase
        public final Map<String, Vote> votes = new LinkedHashMap<>();

        public Proposal(String id, String text, HexCoord hex, double timeSlot) {
            this.id = id; this.text = text; this.hex = hex; this.timeSlot = timeSlot;
        }
        public Proposal(String id, String text, HexCoord hex) {
            this(id, text, hex, 0.5);
        }

        public int approveCount() { return (int) votes.values().stream().filter(v -> v == Vote.APPROVE).count(); }
        public int rejectCount()  { return (int) votes.values().stream().filter(v -> v == Vote.REJECT).count(); }
        public int blindCount()   { return (int) votes.values().stream().filter(v -> v == Vote.BLIND).count(); }
        public int totalVotes()   { return votes.size(); }
        public int visibleTotal() { return totalVotes() - blindCount(); }

        /** Weighted approve count: each APPROVE × its model's pulse weight */
        public double weightedApprove(Map<String, ModelPosition> models, Proposal prop) {
            double w = 0;
            for (var e : votes.entrySet()) {
                if (e.getValue() != Vote.APPROVE) continue;
                ModelPosition mp = models.get(e.getKey());
                if (mp == null) { w += 1.0; continue; }
                // Resonance bonus: models in-phase with proposal's time slot get 1.5×
                double phaseDist = Math.abs(mp.pulsePhase - prop.timeSlot);
                if (phaseDist > 0.5) phaseDist = 1.0 - phaseDist;
                double weight = 1.0 + 0.5 * (1.0 - 2.0 * phaseDist); // 1.0–1.5
                w += weight;
            }
            return w;
        }

        public String status(Map<String, ModelPosition> models, int quorumMin, int approveMin) {
            int blind = blindCount(), visible = visibleTotal();
            if (blind > visible && totalVotes() >= quorumMin) return "BLINDED";
            if (visible < quorumMin) return "PENDING";
            double w = weightedApprove(models, this);
            return w >= approveMin ? "APPROVED" : "REJECTED";
        }

        @Override public String toString() {
            return String.format("#%s: %s %s ✓%d ✗%d 🌫%d", id, text, hex, approveCount(), rejectCount(), blindCount());
        }
    }

    // ── Engine ────────────────────────────────────────────────
    private final Map<String, ModelPosition> models = new LinkedHashMap<>();
    private final Map<String, Proposal> proposals = new LinkedHashMap<>();
    private final int fowHop;
    private final int quorumMin;
    private final int approveMin;
    private boolean fowEnabled = true;

    public WeightedQuorumVote() { this(1, 3, 2); }
    public WeightedQuorumVote(int fowHop, int quorumMin, int approveMin) {
        this.fowHop = fowHop; this.quorumMin = quorumMin; this.approveMin = approveMin;
    }

    /** Register a model with its hex position */
    public void setModelPosition(String modelName, int q, int r, int z) {
        models.put(modelName, new ModelPosition(modelName, new HexCoord(q, r, z)));
    }
    public void setModelPosition(String modelName, int q, int r) {
        setModelPosition(modelName, q, r, 0);
    }

    /** Register a proposal anchored to a hex */
    public Proposal registerProposal(String id, String text, HexCoord hex) {
        Proposal p = new Proposal(id, text, hex);
        proposals.put(id, p);
        return p;
    }
    public Proposal registerProposal(String id, String text, int q, int r) {
        return registerProposal(id, text, new HexCoord(q, r));
    }

    /** Advance 4D time pulse: oscillate each model's phase by delta */
    public void advanceTimePulse(double delta) {
        for (ModelPosition mp : models.values()) {
            mp.pulsePhase = (mp.pulsePhase + delta) % 1.0;
            if (mp.pulsePhase < 0) mp.pulsePhase += 1.0;
        }
    }

    /** Set a specific model's pulse phase directly */
    public void setPulsePhase(String model, double phase) {
        ModelPosition mp = models.get(model);
        if (mp != null) mp.pulsePhase = ((phase % 1.0) + 1.0) % 1.0;
    }

    /** FOW visibility check: is hex visible to model? */
    public boolean isVisible(HexCoord target, String modelName) {
        if (!fowEnabled) return true;
        ModelPosition mp = models.get(modelName);
        if (mp == null) return true;
        return mp.hex.distanceTo(target) <= fowHop;
    }

    /** Cast a vote. Returns actual Vote recorded (BLIND if outside FOW). */
    public Vote castVote(String proposalId, String modelName, Vote intent) {
        Proposal p = proposals.get(proposalId);
        if (p == null) throw new IllegalArgumentException("Unknown: " + proposalId);
        Vote actual = isVisible(p.hex, modelName) ? intent : Vote.BLIND;
        p.votes.put(modelName, actual);
        return actual;
    }
    public Vote castVote(String proposalId, String modelName, boolean approve) {
        return castVote(proposalId, modelName, approve ? Vote.APPROVE : Vote.REJECT);
    }

    /** Calculate full quorum result for a proposal */
    public QuorumResult calculateQuorum(String proposalId) {
        Proposal p = proposals.get(proposalId);
        if (p == null) return null;
        return new QuorumResult(p, models, quorumMin, approveMin);
    }

    /** Auto-vote all models on all proposals (FOW-gated) */
    public void autoVoteAll() {
        for (Proposal p : proposals.values()) {
            for (ModelPosition mp : models.values()) {
                if (isVisible(p.hex, mp.name)) {
                    // Default: approve with 70% probability
                    castVote(p.id, mp.name, Math.random() > 0.3 ? Vote.APPROVE : Vote.REJECT);
                } else {
                    castVote(p.id, mp.name, Vote.BLIND);
                }
            }
        }
    }

    // Getters
    public Proposal getProposal(String id) { return proposals.get(id); }
    public ModelPosition getModel(String name) { return models.get(name); }
    public Collection<Proposal> allProposals() { return proposals.values(); }
    public Collection<ModelPosition> allModels() { return models.values(); }
    public Map<String, ModelPosition> allModelsMap() { return Collections.unmodifiableMap(models); }
    public int proposalCount() { return proposals.size(); }
    public int modelCount() { return models.size(); }
    public int getFowHop() { return fowHop; }
    public void setFowEnabled(boolean e) { this.fowEnabled = e; }
    public boolean isFowEnabled() { return fowEnabled; }

    /** Result container for quorum calculation */
    public static class QuorumResult {
        public final String proposalId;
        public final String text;
        public final String hexKey;
        public final int approve, reject, blind, visible;
        public final double weightedApprove;
        public final String status;
        public final List<String> visibleModels;
        public final List<String> blindModels;
        public final double avgPulsePhase;

        QuorumResult(Proposal p, Map<String, ModelPosition> models, int qMin, int aMin) {
            this.proposalId = p.id;
            this.text = p.text;
            this.hexKey = p.hex.key();
            this.approve = p.approveCount();
            this.reject = p.rejectCount();
            this.blind = p.blindCount();
            this.total = p.totalVotes();
            this.visible = p.visibleTotal();
            this.weightedApprove = p.weightedApprove(models, p);
            this.status = p.status(models, qMin, aMin);

            this.visibleModels = new ArrayList<>();
            this.blindModels = new ArrayList<>();
            double pulseSum = 0; int pulseCount = 0;
            for (var e : p.votes.entrySet()) {
                ModelPosition mp = models.get(e.getKey());
                if (e.getValue() == Vote.BLIND) blindModels.add(e.getKey());
                else { visibleModels.add(e.getKey()); if (mp != null) { pulseSum += mp.pulsePhase; pulseCount++; } }
            }
            this.avgPulsePhase = pulseCount > 0 ? pulseSum / pulseCount : 0;
        }
        public int total;
        @Override public String toString() {
            return String.format("Quorum[#%s: %s] %s ✓%d ✗%d 🌫%d (w:%.2f) pulse:%.2f visible:%s",
                proposalId, text, status, approve, reject, blind, weightedApprove, avgPulsePhase, visibleModels);
        }
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("WeightedQuorumVote: " + proposals.size() + " proposals, " + models.size() + " models\n");
        for (Proposal p : proposals.values()) sb.append("  ").append(p).append("\n");
        return sb.toString();
    }
}
