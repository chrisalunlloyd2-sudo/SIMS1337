package com.aigen.sims.deploy;
import java.util.UUID;
public class DeployProposal {
    public final String id, repoName, filePath, originalContent, newContent, diff;
    public final String modelName, suggestionId;
    public final long timestamp;
    public final String status;
    public final int hexQ, hexR;
    public final String description;
    public DeployProposal(String repoName, String filePath,
                          String originalContent, String newContent, String diff,
                          String modelName, String suggestionId,
                          int hexQ, int hexR, String description) {
        this.id = UUID.randomUUID().toString().substring(0,8);
        this.repoName=repoName; this.filePath=filePath;
        this.originalContent=originalContent; this.newContent=newContent; this.diff=diff;
        this.modelName=modelName; this.suggestionId=suggestionId;
        this.timestamp=System.currentTimeMillis(); this.status="PENDING";
        this.hexQ=hexQ; this.hexR=hexR; this.description=description;
    }
    private DeployProposal(String id, String repoName, String filePath,
                           String originalContent, String newContent, String diff,
                           String modelName, String suggestionId,
                           long timestamp, String status,
                           int hexQ, int hexR, String description) {
        this.id=id; this.repoName=repoName; this.filePath=filePath;
        this.originalContent=originalContent; this.newContent=newContent; this.diff=diff;
        this.modelName=modelName; this.suggestionId=suggestionId;
        this.timestamp=timestamp; this.status=status;
        this.hexQ=hexQ; this.hexR=hexR; this.description=description;
    }
    public DeployProposal withStatus(String s) {
        return new DeployProposal(id,repoName,filePath,originalContent,newContent,
            diff,modelName,suggestionId,timestamp,s,hexQ,hexR,description);
    }
    // 2026-07-31 (NyxGate, Architect gist 3.3): AlgebraicCorrector needs to replace newContent with
    // a repaired version WITHOUT minting a new id -- GateKeeper tracks proposals by id.
    public DeployProposal withNewContent(String repairedContent) {
        return new DeployProposal(id,repoName,filePath,originalContent,repairedContent,
            diff,modelName,suggestionId,timestamp,status,hexQ,hexR,description);
    }
    public boolean isApproved() { return status.equals("APPROVED"); }
    public boolean isRejected() { return status.equals("REJECTED"); }
    public boolean isMerged() { return status.equals("MERGED"); }
    public boolean isPending() { return status.equals("PENDING"); }
}
