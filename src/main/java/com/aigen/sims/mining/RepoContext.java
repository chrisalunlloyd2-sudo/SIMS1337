package com.aigen.sims.mining;
import java.util.List;
import java.util.Map;
public class RepoContext {
    public final String repoName;
    public final String repoPath;
    public final String gitDiff;
    public final List<String> recentCommits;
    public final Map<String, String> fileContents;
    public final List<String> patterns;
    public final int hexQ, hexR;
    public final long scanTimestamp;
    public RepoContext(String repoName, String repoPath, String gitDiff,
                       List<String> recentCommits, Map<String, String> fileContents,
                       List<String> patterns, int hexQ, int hexR) {
        this.repoName = repoName; this.repoPath = repoPath; this.gitDiff = gitDiff;
        this.recentCommits = recentCommits; this.fileContents = fileContents;
        this.patterns = patterns; this.hexQ = hexQ; this.hexR = hexR;
        this.scanTimestamp = System.currentTimeMillis();
    }
    public boolean hasChanges() { return gitDiff != null && !gitDiff.trim().isEmpty(); }
    public int getFileCount() { return fileContents.size(); }
}
