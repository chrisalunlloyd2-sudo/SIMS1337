package com.aigen.sims.mining;
import java.util.UUID;
public class Suggestion {
    public final String id, repoName, filePath, insertAfter, code, modelName;
    public final long timestamp;
    public final String status;
    public final int hexQ, hexR;
    public final String description;
    public Suggestion(String repoName, String filePath, String insertAfter,
                      String code, String modelName, int hexQ, int hexR,
                      String description) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.repoName = repoName; this.filePath = filePath;
        this.insertAfter = insertAfter; this.code = code;
        this.modelName = modelName; this.timestamp = System.currentTimeMillis();
        this.status = "PENDING"; this.hexQ = hexQ; this.hexR = hexR;
        this.description = description;
    }
    private Suggestion(String id, String repoName, String filePath,
                       String insertAfter, String code, String modelName,
                       long timestamp, String status, int hexQ, int hexR,
                       String description) {
        this.id = id; this.repoName = repoName; this.filePath = filePath;
        this.insertAfter = insertAfter; this.code = code;
        this.modelName = modelName; this.timestamp = timestamp;
        this.status = status; this.hexQ = hexQ; this.hexR = hexR;
        this.description = description;
    }
    public Suggestion withStatus(String newStatus) {
        return new Suggestion(id, repoName, filePath, insertAfter, code,
            modelName, timestamp, newStatus, hexQ, hexR, description);
    }
}
