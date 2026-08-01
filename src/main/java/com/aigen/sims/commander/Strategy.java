package com.aigen.sims.commander;

/**
 * Strategy -- an immutable strategic decision AegisCommander derives from real
 * SuggestionRegistry outcomes (never fabricated: pendingCount/approvedCount/rejectedCount
 * are the actual counts it read, and focusRepo/focusModel are null when there isn't
 * enough real data to justify a pick).
 */
public class Strategy {
    public final String focusRepo;
    public final String focusModel;
    public final String reason;
    public final long generatedAt;
    public final int pendingCount, approvedCount, rejectedCount, deployedCount;

    public Strategy(String focusRepo, String focusModel, String reason, long generatedAt,
                    int pendingCount, int approvedCount, int rejectedCount, int deployedCount) {
        this.focusRepo = focusRepo;
        this.focusModel = focusModel;
        this.reason = reason;
        this.generatedAt = generatedAt;
        this.pendingCount = pendingCount;
        this.approvedCount = approvedCount;
        this.rejectedCount = rejectedCount;
        this.deployedCount = deployedCount;
    }

    public String toJson() {
        return String.format(
            "{\"focusRepo\":%s,\"focusModel\":%s,\"reason\":\"%s\",\"generatedAt\":%d," +
            "\"pendingCount\":%d,\"approvedCount\":%d,\"rejectedCount\":%d,\"deployedCount\":%d}",
            focusRepo == null ? "null" : "\"" + focusRepo + "\"",
            focusModel == null ? "null" : "\"" + focusModel + "\"",
            reason.replace("\"", "\\\""), generatedAt,
            pendingCount, approvedCount, rejectedCount, deployedCount);
    }
}
