package com.aigen.sims.phase1;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QuorumVoting — FOW-gated quorum voting engine.
 * Models vote on proposals anchored to hex coordinates.
 * Only models within FOW range can see and vote on a proposal.
 * BLIND votes are tracked separately and don't count toward quorum.
 *
 * Quorum rules:
 *   - Minimum 3 visible votes required to reach quorum
 *   - 2+ APPROVE votes needed to pass (within visible voters)
 *   - If BLIND votes > visible votes → "BLINDED" (can't form quorum)
 */
public class QuorumVoting {
    public enum Vote { APPROVE, REJECT, BLIND }

    public static class Proposal {
        public final String id;
        public final String text;
        public final HexCoord hex;
        public final Map<String, Vote> votes = new ConcurrentHashMap<>();

        public Proposal(String id, String text, HexCoord hex) {
            this.id = id; this.text = text; this.hex = hex;
        }

        public int approveCount() { return (int) votes.values().stream().filter(v -> v == Vote.APPROVE).count(); }
        public int rejectCount()  { return (int) votes.values().stream().filter(v -> v == Vote.REJECT).count(); }
        public int blindCount()   { return (int) votes.values().stream().filter(v -> v == Vote.BLIND).count(); }
        public int totalVotes()   { return votes.size(); }
        public int visibleTotal() { return totalVotes() - blindCount(); }

        public String status() {
            int blind = blindCount(), visible = visibleTotal(), approve = approveCount();
            if (blind > visible && totalVotes() >= 3) return "BLINDED";
            if (visible >= 3 && approve >= 2) return "APPROVED";
            if (visible >= 3 && approve < 2) return "REJECTED";
            return "PENDING";
        }

        @Override public String toString() {
            return String.format("#%s: %s ⬡(%s) ✓%d ✗%d 🌫%d [%s]",
                id, text, hex.key(), approveCount(), rejectCount(), blindCount(), status());
        }
    }

    private final FOWGate fow;
    private final Map<String, Proposal> proposals = new LinkedHashMap<>();
    private final int quorumMin;
    private final int approveMin;

    public QuorumVoting(FOWGate fow) { this(fow, 3, 2); }
    public QuorumVoting(FOWGate fow, int quorumMin, int approveMin) {
        this.fow = fow; this.quorumMin = quorumMin; this.approveMin = approveMin;
    }

    /** Add a proposal anchored to a hex coordinate */
    public Proposal addProposal(String id, String text, HexCoord hex) {
        Proposal p = new Proposal(id, text, hex);
        proposals.put(id, p);
        return p;
    }

    public Proposal addProposal(String id, String text, String hexKey) {
        return addProposal(id, text, HexCoord.fromString(hexKey));
    }

    /** Cast a vote from a model. Returns the Vote actually recorded (BLIND if outside FOW). */
    public Vote castVote(String proposalId, String model, Vote intent) {
        Proposal p = proposals.get(proposalId);
        if (p == null) throw new IllegalArgumentException("Unknown proposal: " + proposalId);

        Vote actual;
        if (!fow.isVisible(p.hex, model)) {
            actual = Vote.BLIND;
        } else {
            actual = intent;
        }
        p.votes.put(model, actual);
        return actual;
    }

    /** Cast a boolean vote (true=APPROVE, false=REJECT) */
    public Vote castVote(String proposalId, String model, boolean approve) {
        return castVote(proposalId, model, approve ? Vote.APPROVE : Vote.REJECT);
    }

    /** Get all models that can see a proposal */
    public List<String> visibleModels(String proposalId) {
        Proposal p = proposals.get(proposalId);
        if (p == null) return List.of();
        return fow.modelNames().stream()
            .filter(m -> fow.isVisible(p.hex, m))
            .toList();
    }

    /** Auto-vote all models on all proposals (FOW-gated) */
    public void autoVote(java.util.function.BiFunction<String, Proposal, Vote> modelDecision) {
        for (Proposal p : proposals.values()) {
            for (String model : fow.modelNames()) {
                if (fow.isVisible(p.hex, model)) {
                    Vote v = modelDecision.apply(model, p);
                    castVote(p.id, model, v);
                } else {
                    castVote(p.id, model, Vote.BLIND);
                }
            }
        }
    }

    public Proposal getProposal(String id) { return proposals.get(id); }
    public Collection<Proposal> allProposals() { return Collections.unmodifiableCollection(proposals.values()); }
    public int proposalCount() { return proposals.size(); }
    public int totalVotesCast() { return proposals.values().stream().mapToInt(Proposal::totalVotes).sum(); }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("QuorumVoting: " + proposals.size() + " proposals\n");
        for (Proposal p : proposals.values()) sb.append("  ").append(p).append("\n");
        return sb.toString();
    }
}
