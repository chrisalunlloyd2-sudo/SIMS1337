package com.aigen.sims.mining;

/**
 * CodeMinerTest — Phase 2 mining pipeline tests.
 */
public class CodeMinerTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("=== CodeMiner — Phase 2 Tests ===\n");

        testRepoWatcher();
        testPatternMatcher();
        testCodeMinerPipeline();
        testMiningStats();
        testModelMine();
        testHexAnchoring();
        testDeduplication();

        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String n, boolean c) {
        if (c) { passed++; System.out.println("  ✅ " + n); }
        else { failed++; System.out.println("  ❌ " + n + " FAILED"); }
    }

    // ── RepoWatcher ───────────────────────────────────────────
    static void testRepoWatcher() {
        System.out.println("RepoWatcher:");
        RepoWatcher rw = new RepoWatcher();
        rw.addRepo("TestRepo", "/tmp/test", "0,0", ".java", ".py");
        rw.addRepo("AnotherRepo", "/tmp/other", "1,-1", ".sh");
        check("2 repos registered", rw.getRepos().size() == 2);
        check("no pending initially", rw.pendingCount() == 0);
        check("hex anchored", rw.getRepos().get(0).hexKey.equals("0,0"));

        // Add suggestion
        PatternMatch pm = new PatternMatch("TestRepo", "/tmp/test/Foo.java",
            "function", "doStuff()", "void doStuff() { }", 1, 0.85, "0,0");
        rw.addSuggestion(pm);
        check("1 pending after add", rw.pendingCount() == 1);
        check("drain clears pending", rw.drainSuggestions().size() == 1);
        check("pending empty after drain", rw.pendingCount() == 0);
    }

    // ── PatternMatcher ────────────────────────────────────────
    static void testPatternMatcher() {
        System.out.println("\nPatternMatcher:");
        PatternMatcher pm = new PatternMatcher();
        RepoWatcher rw = new RepoWatcher();
        RepoWatcher.WatchedRepo repo = rw.addRepo("Test", "/tmp/test", "0,0", ".java");

        java.util.List<PatternMatch> matches = pm.matchRepo(repo);
        check("found patterns", !matches.isEmpty());
        check("found > 1 pattern", matches.size() > 1);
        check("all have hexKey", matches.stream().allMatch(m -> m.hexKey.equals("0,0")));
        check("all have confidence > 0", matches.stream().allMatch(m -> m.confidence > 0));
    }

    // ── CodeMiner Pipeline ────────────────────────────────────
    static void testCodeMinerPipeline() {
        System.out.println("\nCodeMiner Pipeline:");
        CodeMiner miner = new CodeMiner();

        // Mine once
        CodeMiner.MiningResult r1 = miner.mine();
        check("mine found patterns", r1.patternsFound.size() > 0);
        check("mine generated suggestions", r1.suggestionsGenerated > 0);
        check("pending drained", r1.pendingSuggestions.size() > 0);
        check("history has 1 entry", miner.getHistory().size() == 1);

        // Mine again
        CodeMiner.MiningResult r2 = miner.mine();
        check("history has 2 entries", miner.getHistory().size() == 2);
    }

    // ── Mining Stats ──────────────────────────────────────────
    static void testMiningStats() {
        System.out.println("\nMiningStats:");
        CodeMiner miner = new CodeMiner();
        miner.mine();

        CodeMiner.MiningStats stats = miner.getStats();
        check("repos watched = 6", stats.reposWatched == 6);
        check("patterns found > 0", stats.patternsFound > 0);
        check("mining runs = 1", stats.miningRuns == 1);

        // Approve one
        PatternMatch pm = new PatternMatch("test", "/x.java",
            "function", "f()", "void f(){}", 1, 0.9, "0,0");
        miner.approveSuggestion(pm);
        stats = miner.getStats();
        check("approved = 1", stats.approved == 1);
        check("rejected = 0", stats.rejected == 0);
    }

    // ── Model Mine ────────────────────────────────────────────
    static void testModelMine() {
        System.out.println("\nModelMine:");
        CodeMiner miner = new CodeMiner();
        java.util.List<PatternMatch> suggestions = miner.modelMine(
            "qwen2.5:0.5b", "MatrixWinCE", "health_check");

        check("generated 3 suggestions", suggestions.size() == 3);
        check("all from MatrixWinCE", suggestions.stream().allMatch(m -> m.repoName.equals("MatrixWinCE")));
        check("all actionable", suggestions.stream().allMatch(PatternMatch::isActionable));
        check("pending = 3", miner.getWatcher().pendingCount() == 3);
    }

    // ── Hex Anchoring ─────────────────────────────────────────
    static void testHexAnchoring() {
        System.out.println("\nHex Anchoring:");
        CodeMiner miner = new CodeMiner();
        // Check each repo has a distinct hex
        java.util.Set<String> hexes = new java.util.HashSet<>();
        for (RepoWatcher.WatchedRepo r : miner.getWatcher().getRepos()) {
            hexes.add(r.hexKey);
        }
        check("all repos have hex", miner.getWatcher().getRepos().stream()
            .allMatch(r -> r.hexKey != null && !r.hexKey.isEmpty()));
        check("at least 3 distinct hexes", hexes.size() >= 3);
        check("MatrixWinCE at (0,0)", miner.getWatcher().getRepos().get(0).hexKey.equals("0,0"));
    }

    // ── Deduplication ─────────────────────────────────────────
    static void testDeduplication() {
        System.out.println("\nDeduplication:");
        PatternMatcher pm = new PatternMatcher();
        java.util.List<PatternMatch> input = new java.util.ArrayList<>();
        input.add(new PatternMatch("a", "/a.java", "function", "run()", "...", 1, 0.7, "0,0"));
        input.add(new PatternMatch("b", "/b.java", "function", "run()", "...", 2, 0.9, "1,0"));
        input.add(new PatternMatch("c", "/c.java", "class", "Main", "...", 3, 0.8, "0,0"));

        java.util.List<PatternMatch> dedup = pm.deduplicate(input);
        check("deduplicated to 2", dedup.size() == 2);
        check("kept higher confidence (0.9)", dedup.stream()
            .anyMatch(m -> m.signature.equals("run()") && Math.abs(m.confidence - 0.9) < 0.01));
    }
}
