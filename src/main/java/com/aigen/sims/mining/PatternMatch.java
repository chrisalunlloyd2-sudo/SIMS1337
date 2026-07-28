package com.aigen.sims.mining;

/**
 * PatternMatch — A code pattern found by the miner with FOW hex anchoring.
 */
public class PatternMatch {
    public final String repoName;
    public final String filePath;
    public final String patternType;   // "function", "class", "import", "comment", "test"
    public final String signature;     // e.g. "public void heal()" or "def mine():"
    public final String snippet;       // first 200 chars of the matched code
    public final int lineNumber;
    public final double confidence;    // 0.0–1.0 how confident this is a reusable pattern
    public final String hexKey;        // FOW hex anchor from the repo's hex position

    public PatternMatch(String repo, String file, String type, String sig,
                        String snip, int line, double conf, String hex) {
        this.repoName = repo; this.filePath = file; this.patternType = type;
        this.signature = sig; this.snippet = snip; this.lineNumber = line;
        this.confidence = conf; this.hexKey = hex;
    }

    /** Is this confident enough to suggest? */
    public boolean isActionable() { return confidence >= 0.6; }

    @Override public String toString() {
        return String.format("[%s] %s:%d %s → %s (%.0f%%)",
            patternType, filePath, lineNumber, signature, hexKey, confidence * 100);
    }
}
