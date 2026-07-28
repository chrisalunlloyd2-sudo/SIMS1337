package com.aigen.sims.phase1;

/**
 * TestQuorumVoting — Unit tests for the Phase 1 backend.
 * Tests: HexCoord math, FOWGate visibility, QuorumVoting FOW-gating, BLIND tracking.
 */
public class TestQuorumVoting {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("=== SIMS1337 Phase 1 — QuorumVoting Tests ===\n");

        testHexCoord();
        testFOWGate();
        testQuorumVoting();
        testBlindTracking();
        testQuorumRules();
        testAutoVote();

        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("  ✅ " + name); }
        else { failed++; System.out.println("  ❌ " + name + " FAILED"); }
    }

    // ── HexCoord ──────────────────────────────────────────────
    static void testHexCoord() {
        System.out.println("HexCoord:");
        HexCoord a = new HexCoord(0, 0);
        HexCoord b = new HexCoord(3, -2);
        HexCoord c = new HexCoord(-3, 2);
        check("distance a→a = 0", a.distanceTo(a) == 0);
        check("distance a→b = 3", a.distanceTo(b) == 3);
        check("distance a→c = 3", a.distanceTo(c) == 3);
        check("distance b→c = 6", a.distanceTo(b) == 3 && a.distanceTo(c) == 3 && b.distanceTo(c) == 6);
        check("key format", a.key().equals("0,0") && b.key().equals("3,-2"));
        check("neighbors count = 6", a.neighbors().length == 6);
        check("oneHop count = 7", a.oneHop().length == 7);
        check("fromString parse", HexCoord.fromString("1,-1").equals(new HexCoord(1, -1)));
    }

    // ── FOWGate ───────────────────────────────────────────────
    static void testFOWGate() {
        System.out.println("\nFOWGate:");
        FOWGate fow = new FOWGate(1);
        fow.pinAgent("Alpha", new HexCoord(0, 0));
        fow.pinAgent("Beta", new HexCoord(3, -2));
        fow.pinAgent("Gamma", new HexCoord(-3, 2));
        fow.assignModel("qwen2.5:0.5b", "Alpha");
        fow.assignModel("phi:latest", "Beta");
        fow.assignModel("deepseek-r1:1.5b", "Gamma");

        check("agent count = 3", fow.agentCount() == 3);
        check("model count = 3", fow.modelCount() == 3);

        // Alpha at (0,0) sees hexes within 1-hop
        check("Alpha sees (0,0)", fow.isVisible(new HexCoord(0,0), "qwen2.5:0.5b"));
        check("Alpha sees (1,0)", fow.isVisible(new HexCoord(1,0), "qwen2.5:0.5b"));
        check("Alpha sees (-1,1)", fow.isVisible(new HexCoord(-1,1), "qwen2.5:0.5b"));
        check("Alpha NOT see (3,-2)", !fow.isVisible(new HexCoord(3,-2), "qwen2.5:0.5b"));
        check("Alpha NOT see (-3,2)", !fow.isVisible(new HexCoord(-3,2), "qwen2.5:0.5b"));

        // Beta at (3,-2)
        check("Beta sees (3,-2)", fow.isVisible(new HexCoord(3,-2), "phi:latest"));
        check("Beta sees (2,-2)", fow.isVisible(new HexCoord(2,-2), "phi:latest"));
        check("Beta NOT see (0,0)", !fow.isVisible(new HexCoord(0,0), "phi:latest"));

        // Gamma at (-3,2)
        check("Gamma sees (-3,2)", fow.isVisible(new HexCoord(-3,2), "deepseek-r1:1.5b"));
        check("Gamma sees (-2,2)", fow.isVisible(new HexCoord(-2,2), "deepseek-r1:1.5b"));
        check("Gamma NOT see (0,0)", !fow.isVisible(new HexCoord(0,0), "deepseek-r1:1.5b"));

        // sliceLocal
        check("sliceLocal (0,0) for Alpha returns (0,0)",
            fow.sliceLocal(new HexCoord(0,0), "qwen2.5:0.5b") != null);
        check("sliceLocal (3,-2) for Alpha returns null",
            fow.sliceLocal(new HexCoord(3,-2), "qwen2.5:0.5b") == null);

        // Disabled FOW lets everything through
        fow.setEnabled(false);
        check("disabled FOW: Alpha sees (3,-2)", fow.isVisible(new HexCoord(3,-2), "qwen2.5:0.5b"));
        fow.setEnabled(true);

        // Unassigned model sees all
        check("unassigned model sees (0,0)", fow.isVisible(new HexCoord(0,0), "unknown-model"));
    }

    // ── QuorumVoting ──────────────────────────────────────────
    static void testQuorumVoting() {
        System.out.println("\nQuorumVoting:");
        FOWGate fow = makeTestFOW();
        QuorumVoting qv = new QuorumVoting(fow);

        qv.addProposal("1", "Add WebSocket", "1,0");
        qv.addProposal("2", "Deploy production", "0,0");
        qv.addProposal("3", "Mine hex renderer", "-1,-1");
        qv.addProposal("4", "Expand FOW to 2-hop", "-2,0");

        check("proposal count = 4", qv.proposalCount() == 4);

        // Proposal at (1,0): Alpha sees it, Beta and Gamma don't
        QuorumVoting.Vote v1 = qv.castVote("1", "qwen2.5:0.5b", true);
        check("Alpha votes APPROVE on #1", v1 == QuorumVoting.Vote.APPROVE);
        check("#1 approve=1", qv.getProposal("1").approveCount() == 1);

        QuorumVoting.Vote v2 = qv.castVote("1", "phi:latest", true);
        check("Beta votes BLIND on #1", v2 == QuorumVoting.Vote.BLIND);
        check("#1 blind=1", qv.getProposal("1").blindCount() == 1);

        // Proposal at (0,0): Alpha sees it, others don't
        QuorumVoting.Vote v3 = qv.castVote("2", "qwen2.5:0.5b", false);
        check("Alpha votes REJECT on #2", v3 == QuorumVoting.Vote.REJECT);

        QuorumVoting.Vote v4 = qv.castVote("2", "deepseek-r1:1.5b", true);
        check("Gamma votes BLIND on #2", v4 == QuorumVoting.Vote.BLIND);

        // visibleModels
        check("#1 visible models count", qv.visibleModels("1").size() >= 1);
        check("#1 visible includes Alpha", qv.visibleModels("1").contains("qwen2.5:0.5b"));
        check("#1 visible excludes Beta", !qv.visibleModels("1").contains("phi:latest"));
    }

    // ── BLIND Tracking ────────────────────────────────────────
    static void testBlindTracking() {
        System.out.println("\nBLIND Tracking:");
        FOWGate fow = makeTestFOW();
        QuorumVoting qv = new QuorumVoting(fow);
        qv.addProposal("1", "Test proposal", "3,-2");

        // Only Beta can see (3,-2)
        qv.castVote("1", "qwen2.5:0.5b", true);    // BLIND (Alpha at 0,0)
        qv.castVote("1", "tinyllama:1.1b", false);   // BLIND (Alpha at 0,0)
        qv.castVote("1", "phi:latest", true);        // APPROVE (Beta at 3,-2)
        qv.castVote("1", "phi3:mini", true);          // APPROVE (Beta at 3,-2)
        qv.castVote("1", "llama3.2:1b", true);        // BLIND (Gamma at -3,2)
        qv.castVote("1", "deepseek-r1:1.5b", false);  // BLIND (Gamma at -3,2)

        QuorumVoting.Proposal p = qv.getProposal("1");
        check("total votes = 6", p.totalVotes() == 6);
        check("blind count = 4", p.blindCount() == 4);
        check("visible total = 2", p.visibleTotal() == 2);
        check("approve count = 2", p.approveCount() == 2);
        check("status = BLINDED (blind > visible)", "BLINDED".equals(p.status()));
    }

    // ── Quorum Rules ──────────────────────────────────────────
    static void testQuorumRules() {
        System.out.println("\nQuorum Rules:");
        FOWGate fow = makeTestFOW();
        QuorumVoting qv = new QuorumVoting(fow);
        qv.addProposal("1", "Center proposal", "0,0");

        // Alpha (2 models) can see (0,0) — not enough for quorum (need 3)
        qv.castVote("1", "qwen2.5:0.5b", true);
        qv.castVote("1", "tinyllama:1.1b", true);
        check("2 votes: PENDING", "PENDING".equals(qv.getProposal("1").status()));

        // Add a 3rd visible model by moving Agent Beta closer
        fow.pinAgent("Agent Beta", new HexCoord(1, 0)); // was at (3,-2), now can see (0,0)
        qv.castVote("1", "phi:latest", true);
        check("3 votes, 3 approve: APPROVED", "APPROVED".equals(qv.getProposal("1").status()));

        // Test REJECTED — move Beta to (0,-1) so it can see (-1,0)
        qv.addProposal("2", "Rejected proposal", "-1,0");
        fow.pinAgent("Agent Beta", new HexCoord(0, -1)); // can see (-1,0): distance 1
        qv.castVote("2", "qwen2.5:0.5b", false);
        qv.castVote("2", "tinyllama:1.1b", false);
        qv.castVote("2", "phi:latest", false);
        qv.castVote("2", "phi3:mini", false);
        check("4 votes, 0 approve: REJECTED", "REJECTED".equals(qv.getProposal("2").status()));
    }

    // ── AutoVote ──────────────────────────────────────────────
    static void testAutoVote() {
        System.out.println("\nAutoVote:");
        FOWGate fow = makeTestFOW();
        QuorumVoting qv = new QuorumVoting(fow);
        qv.addProposal("1", "Auto test", "0,0");

        // Auto-vote with a deterministic decision function
        qv.autoVote((model, proposal) ->
            model.contains("qwen") ? QuorumVoting.Vote.APPROVE : QuorumVoting.Vote.REJECT);

        QuorumVoting.Proposal p = qv.getProposal("1");
        check("total votes cast = 6", p.totalVotes() == 6);
        // Proposal at (0,0): Alpha's 2 models see it, the other 4 get BLIND
        check("has BLIND votes", p.blindCount() > 0);
        check("has APPROVE votes (qwen)", p.approveCount() > 0);
    }

    // ── Helper ────────────────────────────────────────────────
    static FOWGate makeTestFOW() {
        FOWGate fow = new FOWGate(1);
        fow.pinAgent("Agent Alpha", new HexCoord(0, 0));
        fow.pinAgent("Agent Beta", new HexCoord(3, -2));
        fow.pinAgent("Agent Gamma", new HexCoord(-3, 2));
        fow.assignModel("qwen2.5:0.5b", "Agent Alpha");
        fow.assignModel("tinyllama:1.1b", "Agent Alpha");
        fow.assignModel("phi:latest", "Agent Beta");
        fow.assignModel("phi3:mini", "Agent Beta");
        fow.assignModel("llama3.2:1b", "Agent Gamma");
        fow.assignModel("deepseek-r1:1.5b", "Agent Gamma");
        return fow;
    }
}
