package com.aigen.sims.lora;
import java.util.UUID;
public class LoRAAdapter {
    public final String id, modelName, taskType, adapterPath;
    public final double score;
    public final int voteCount;
    public final long createdAt;
    public final String status;
    public LoRAAdapter(String modelName, String taskType, String adapterPath) {
        this.id = UUID.randomUUID().toString().substring(0,8);
        this.modelName=modelName; this.taskType=taskType; this.adapterPath=adapterPath;
        this.score=0.0; this.voteCount=0; this.createdAt=System.currentTimeMillis(); this.status="TESTING";
    }
    private LoRAAdapter(String id, String m, String t, String p, double s, int v, long c, String st) {
        this.id=id; this.modelName=m; this.taskType=t; this.adapterPath=p;
        this.score=s; this.voteCount=v; this.createdAt=c; this.status=st;
    }
    public LoRAAdapter withScore(double s) { return new LoRAAdapter(id,modelName,taskType,adapterPath,s,voteCount,createdAt,status); }
    public LoRAAdapter withVoteCount(int v) { return new LoRAAdapter(id,modelName,taskType,adapterPath,score,v,createdAt,status); }
    public LoRAAdapter withStatus(String s) { return new LoRAAdapter(id,modelName,taskType,adapterPath,score,voteCount,createdAt,s); }
    public boolean isActive() { return status.equals("ACTIVE"); }
    public boolean isTesting() { return status.equals("TESTING"); }
    public boolean isDeprecated() { return status.equals("DEPRECATED"); }
}
