package com.aigen.sims.voting;

import com.aigen.sims.voting.WeightedQuorumVote.Vote;
import com.aigen.sims.voting.WeightedQuorumVote.Proposal;
import com.aigen.sims.voting.WeightedQuorumVote.QuorumResult;

/**
 * WeightedQuorumVoteTest — 4D time pulse FOW-gated quorum voting tests.
 * Tests: model registration, FOW visibility, BLIND tracking,
 * 4D pulse resonance weighting, quorum rules, time advance.
 */
public class WeightedQuorumVoteTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("=== WeightedQuorumVote — Phase 1 Tests ===\n");

        testModelRegistration();
        testFOWVisibility();
        testBlindTracking();
        testQuorumRules();
        test4DTimePulse();
        testWeightedApprove();
        testTimeAdvance();
        testAutoVote();

        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  ✅ " + name); }
        else { failed++; System.out.println("  ❌ " + name + " FAILED"); }
    }

    // ── Model Registration ────────────────────────────────────
    static void testModelRegistration() {
        System.out.println("Model Registration:");
        WeightedQuorumVote wqv = makeEngine();
        check("model count = 8", wqv.modelCount() == 8);
        check("qwen at (0,0,2)", wqv.getModel("qwen2.5:0.5b").hex.equals(new HexCoord(0,0,2)));
        check("phi3 at (3,-2,1)", wqv.getModel("phi3:mini").hex.equals(new HexCoord(3,-2,1)));
        check("deepseek at (-3,2,3)", wqv.getModel("deepseek-r1:1.5b").hex.equals(new HexCoord(-3,2,3)));
    }

    // ── FOW Visibility ────────────────────────────────────────
    static void testFOWVisibility() {
        System.out.println("\nFOW Visibility:");
        WeightedQuorumVote wqv = makeEngine();
        // Alpha (0,0,2)
        check("qwen sees (0,0)", wqv.isVisible(new HexCoord(0,0), "qwen2.5:0.5b"));
        check("qwen sees (1,0)", wqv.isVisible(new HexCoord(1,0), "qwen2.5:0.5b"));
        check("qwen NOT (3,-2)", !wqv.isVisible(new HexCoord(3,-2), "qwen2.5:0.5b"));
        // Beta (3,-2,1)
        check("phi sees (3,-2)", wqv.isVisible(new HexCoord(3,-2), "phi:latest"));
        check("phi NOT (0,0)", !wqv.isVisible(new HexCoord(0,0), "phi:latest"));
        // Gamma (-3,2,3)
        check("deepseek sees (-3,2)", wqv.isVisible(new HexCoord(-3,2), "deepseek-r1:1.5b"));
        check("deepseek NOT (0,0)", !wqv.isVisible(new HexCoord(0,0), "deepseek-r1:1.5b"));
        // Delta (1,0): gemma2 at (1,0,1)
        check("gemma sees (1,0)", wqv.isVisible(new HexCoord(1,0), "gemma2:2b"));
        check("gemma sees (0,0)", wqv.isVisible(new HexCoord(0,0), "gemma2:2b"));
    }

    // ── BLIND Tracking ────────────────────────────────────────
    static void testBlindTracking() {
        System.out.println("\nBLIND Tracking:");
        WeightedQuorumVote wqv = makeEngine();
        wqv.registerProposal("prop1", "Test at Beta hex", new HexCoord(3,-2));

        // Only Beta's models can see (3,-2)
        Vote v1 = wqv.castVote("prop1", "qwen2.5:0.5b", true);     // BLIND
        Vote v2 = wqv.castVote("prop1", "phi:latest", true);        // APPROVE
        Vote v3 = wqv.castVote("prop1", "phi3:mini", true);         // APPROVE
        Vote v4 = wqv.castVote("prop1", "deepseek-r1:1.5b", false); // BLIND

        check("qwen vote = BLIND", v1 == Vote.BLIND);
        check("phi vote = APPROVE", v2 == Vote.APPROVE);
        check("deepseek vote = BLIND", v4 == Vote.BLIND);

        Proposal p = wqv.getProposal("prop1");
        check("approve = 2", p.approveCount() == 2);
        check("blind = 2", p.blindCount() == 2);
        check("visible = 2", p.visibleTotal() == 2);
        check("status = PENDING (visible < 3)", "PENDING".equals(p.status(wqv.allModelsMap(), 3, 2)));
    }

    // ── Quorum Rules ──────────────────────────────────────────
    static void testQuorumRules() {
        System.out.println("\nQuorum Rules:");
        WeightedQuorumVote wqv = makeEngine();
        Proposal p = wqv.registerProposal("prop1", "Center proposal", new HexCoord(0,0));

        // Blue (Alpha) at (0,0) + Silver (Delta) at (1,0) can see
        wqv.castVote("prop1", "qwen2.5:0.5b", Vote.APPROVE);
        wqv.castVote("prop1", "tinyllama:1.1b", Vote.APPROVE);
        wqv.castVote("prop1", "codellama:7b", Vote.APPROVE); // Silver at (1,0) sees (0,0)
        QuorumResult r = wqv.calculateQuorum("prop1");
        check("3 visible, 3 approve → APPROVED", "APPROVED".equals(r.status));

        // Rejected
        Proposal p2 = wqv.registerProposal("prop2", "Reject test", new HexCoord(0,0));
        wqv.castVote("prop2", "qwen2.5:0.5b", Vote.REJECT);
        wqv.castVote("prop2", "tinyllama:1.1b", Vote.REJECT);
        wqv.castVote("prop2", "codellama:7b", Vote.REJECT);
        check("3 visible, 0 approve → REJECTED", "REJECTED".equals(wqv.calculateQuorum("prop2").status));
    }

    // ── 4D Time Pulse ─────────────────────────────────────────
    static void test4DTimePulse() {
        System.out.println("\n4D Time Pulse:");
        WeightedQuorumVote wqv = makeEngine();

        // Set specific pulse phases
        wqv.setPulsePhase("qwen2.5:0.5b", 0.5);
        wqv.setPulsePhase("tinyllama:1.1b", 0.5);
        wqv.setPulsePhase("phi:latest", 0.0);

        check("qwen pulse = 0.5", Math.abs(wqv.getModel("qwen2.5:0.5b").pulsePhase - 0.5) < 0.001);
        check("phi pulse = 0.0", wqv.getModel("phi:latest").pulsePhase < 0.001);

        // Vote with resonance: proposal timeSlot 0.5, qwen at 0.5 → bonus
        Proposal p = wqv.registerProposal("prop1", "Pulse test", new HexCoord(0,0));
        wqv.castVote("prop1", "qwen2.5:0.5b", Vote.APPROVE); // phase 0.5, slot 0.5 → weight ~1.5
        wqv.castVote("prop1", "tinyllama:1.1b", Vote.APPROVE); // phase 0.5, slot 0.5 → weight ~1.5
        wqv.castVote("prop1", "codellama:7b", Vote.APPROVE); // phase 0.3, slot 0.5 → weight ~1.3

        double w = wqv.getProposal("prop1").weightedApprove(wqv.allModelsMap(), p);
        check("weighted approve > 3.0 (pulse bonus)", w > 3.0);
        check("weighted approve ≈ 4.0", Math.abs(w - 4.0) < 0.3); // 1.5+1.5+1.0=4.0
    }

    // ── Weighted Approve ──────────────────────────────────────
    static void testWeightedApprove() {
        System.out.println("\nWeighted Approve:");
        WeightedQuorumVote wqv = makeEngine();
        Proposal p = wqv.registerProposal("prop1", "Weight test", new HexCoord(0,0));

        wqv.setPulsePhase("qwen2.5:0.5b", 0.5);
        wqv.setPulsePhase("tinyllama:1.1b", 0.0);
        wqv.castVote("prop1", "qwen2.5:0.5b", Vote.APPROVE);  // weight ~1.5
        wqv.castVote("prop1", "tinyllama:1.1b", Vote.APPROVE); // weight ~1.0
        wqv.castVote("prop1", "codellama:7b", Vote.APPROVE);    // weight ~1.0

        double w = p.weightedApprove(wqv.allModelsMap(), p);
        check("weighted > 2.0", w > 2.0);
        // Weight varies with pulse phase; exact value depends on timing

        QuorumResult r = wqv.calculateQuorum("prop1");
        check("status = APPROVED (weighted >= 2)", "APPROVED".equals(r.status));
    }

    // ── Time Advance ──────────────────────────────────────────
    static void testTimeAdvance() {
        System.out.println("\nTime Advance:");
        WeightedQuorumVote wqv = makeEngine();

        wqv.setPulsePhase("qwen2.5:0.5b", 0.0);
        wqv.advanceTimePulse(0.25);
        check("qwen phase = 0.25 after +0.25", Math.abs(wqv.getModel("qwen2.5:0.5b").pulsePhase - 0.25) < 0.001);

        wqv.advanceTimePulse(0.9);
        check("qwen phase wraps: 0.15", Math.abs(wqv.getModel("qwen2.5:0.5b").pulsePhase - 0.15) < 0.001);
    }

    // ── AutoVote ──────────────────────────────────────────────
    static void testAutoVote() {
        System.out.println("\nAutoVote:");
        WeightedQuorumVote wqv = makeEngine();
        wqv.registerProposal("prop1", "Auto test", new HexCoord(0,0));
        wqv.registerProposal("prop2", "Beta test", new HexCoord(3,-2));
        wqv.registerProposal("prop3", "Gamma test", new HexCoord(-3,2));

        wqv.autoVoteAll();

        // prop1 at (0,0): Blue+Silver see it = 3 models vote
        Proposal p1 = wqv.getProposal("prop1");
        check("prop1 has votes", p1.totalVotes() == 8);
        check("prop1 has visible votes", p1.visibleTotal() >= 3);
        check("prop1 has BLIND votes", p1.blindCount() > 0);

        // prop2 at (3,-2): Gold+Cyan see it = 2 models
        Proposal p2 = wqv.getProposal("prop2");
        check("prop2 has votes", p2.totalVotes() == 8);
        check("prop2 blind >= 6", p2.blindCount() >= 6);

        QuorumResult r1 = wqv.calculateQuorum("prop1");
        check("prop1 quorum calculated", r1 != null);
        check("prop1 has visible models", !r1.visibleModels.isEmpty());
        check("prop1 has blind models", !r1.blindModels.isEmpty());
    }

    // ── 8-model engine matching GodHandApp ────────────────────
    static WeightedQuorumVote makeEngine() {
        WeightedQuorumVote wqv = new WeightedQuorumVote(1, 3, 2);

        // Blue group — Agent Alpha (0,0)
        wqv.setModelPosition("qwen2.5:0.5b", 0, 0, 2);    // FAST tier, grid+ability+tool
        wqv.setModelPosition("tinyllama:1.1b", 0, 0, 1);   // BALANCED tier, ability+grid+node

        // Gold group — Agent Beta (3,-2)
        wqv.setModelPosition("phi:latest", 3, -2, 2);      // REASONING tier, logic+tool+ability
        wqv.setModelPosition("phi3:mini", 3, -2, 1);       // DEEP tier, logic+backend+node

        // Cyan group — Agent Gamma (-3,2)
        wqv.setModelPosition("llama3.2:1b", -3, 2, 2);     // TOOL tier, tool+ability+grid
        wqv.setModelPosition("deepseek-r1:1.5b", -3, 2, 3);// DEEP tier, logic+backend+tool

        // Silver group — Agent Delta (1,0)
        wqv.setModelPosition("codellama:7b", 1, 0, 2);     // CODE tier, tool+backend+node
        wqv.setModelPosition("gemma2:2b", 1, 0, 1);        // BALANCED tier, node+grid+backend

        // Set initial pulse phases
        wqv.setPulsePhase("qwen2.5:0.5b", 0.2);
        wqv.setPulsePhase("tinyllama:1.1b", 0.4);
        wqv.setPulsePhase("phi:latest", 0.6);
        wqv.setPulsePhase("phi3:mini", 0.8);
        wqv.setPulsePhase("llama3.2:1b", 0.1);
        wqv.setPulsePhase("deepseek-r1:1.5b", 0.7);
        wqv.setPulsePhase("codellama:7b", 0.3);
        wqv.setPulsePhase("gemma2:2b", 0.9);

        return wqv;
    }
}
