package com.aigen.sims.mining;

import java.util.*;

/**
 * CodeMiner — FOW-gated code mining pipeline orchestrator.
 *
 * Pipeline: scan repos → extract patterns → deduplicate →
 * model-generate suggestions → FOW-gate by model visibility →
 * queue for voting → apply approved patterns.
 *
 * Integrates with:
 *   - RepoWatcher (repo registry + scanning)
 *   - PatternMatcher (regex extraction)
 *   - FOW voting (models only mine repos they can see)
 */
public class CodeMiner {
    private final RepoWatcher watcher;
    private final PatternMatcher matcher;
    private final List<MiningResult> history = new ArrayList<>();
    private int suggestionsApproved;
    private int suggestionsRejected;

    public CodeMiner() {
        this.watcher = new RepoWatcher();
        this.matcher = new PatternMatcher();
        initDefaultRepos();
    }

    /** Seed the default watched repos with FOW hex anchors */
    private void initDefaultRepos() {
        watcher.addRepo("MatrixWinCE",
            "/data/data/com.termux/files/home/MatrixWinCE",
            "0,0", ".java", ".py", ".sh");

        watcher.addRepo("SIMS1337",
            "/data/data/com.termux/files/home/sims1337",
            "1,-1", ".java");

        watcher.addRepo("brute-foundry",
            "/data/data/com.termux/files/home/MatrixWinCE/modules/brute-foundry",
            "-1,0", ".py");

        watcher.addRepo("openrouter_manager",
            "/data/data/com.termux/files/home/MatrixWinCE/modules/openrouter_manager",
            "2,-1", ".py", ".java");

        watcher.addRepo("heartbeat_server",
            "/data/data/com.termux/files/home/MatrixWinCE/modules/heartbeat_server",
            "-1,-1", ".py");

        watcher.addRepo("kai-proxy",
            "/data/data/com.termux/files/home",
            "1,1", ".py", ".sh");
    }

    /** Run full mining pipeline: scan → deduplicate → suggest */
    public MiningResult mine() {
        MiningResult result = new MiningResult();
        result.timestamp = System.currentTimeMillis();

        // Phase 1: Scan all repos via watcher (adds actionable patterns as suggestions)
        result.patternsFound = watcher.scan(matcher);

        // Phase 2: Deduplicate
        result.patternsFound = matcher.deduplicate(result.patternsFound);

        // Phase 3: Count suggestions generated
        result.suggestionsGenerated = (int) result.patternsFound.stream()
            .filter(PatternMatch::isActionable).count();

        // Phase 4: Drain pending for voting
        result.pendingSuggestions = watcher.drainSuggestions();
        history.add(result);
        return result;
    }

    /** Model generates code suggestions and queues them for voting */
    public List<PatternMatch> modelMine(String modelName, String repoName, String prompt) {
        return watcher.modelSuggest(modelName, repoName, prompt);
    }

    /** Approve a pattern suggestion (after model voting passes) */
    public void approveSuggestion(PatternMatch pm) {
        suggestionsApproved++;
        // In production: apply the pattern (write to file, create PR, etc.)
    }

    /** Reject a pattern suggestion */
    public void rejectSuggestion(PatternMatch pm) {
        suggestionsRejected++;
    }

    /** Get mining stats */
    public MiningStats getStats() {
        return new MiningStats(
            watcher.getRepos().size(),
            watcher.totalPatternsFound(),
            suggestionsApproved,
            suggestionsRejected,
            history.size(),
            watcher.pendingCount()
        );
    }

    public RepoWatcher getWatcher() { return watcher; }
    public List<MiningResult> getHistory() { return Collections.unmodifiableList(history); }

    // ── Inner types ───────────────────────────────────────────

    public static class MiningResult {
        public long timestamp;
        public List<PatternMatch> patternsFound = new ArrayList<>();
        public List<PatternMatch> pendingSuggestions = new ArrayList<>();
        public int suggestionsGenerated;

        @Override public String toString() {
            return String.format("MiningResult[%d patterns, %d suggestions, %d pending]",
                patternsFound.size(), suggestionsGenerated, pendingSuggestions.size());
        }
    }

    public static class MiningStats {
        public final int reposWatched;
        public final int patternsFound;
        public final int approved;
        public final int rejected;
        public final int miningRuns;
        public final int pending;

        public MiningStats(int repos, int found, int approved, int rejected, int runs, int pending) {
            this.reposWatched = repos; this.patternsFound = found;
            this.approved = approved; this.rejected = rejected;
            this.miningRuns = runs; this.pending = pending;
        }

        @Override public String toString() {
            return String.format("Mining: %d repos, %d patterns, %d✓/%d✗, %d runs, %d pending",
                reposWatched, patternsFound, approved, rejected, miningRuns, pending);
        }
    }
}
