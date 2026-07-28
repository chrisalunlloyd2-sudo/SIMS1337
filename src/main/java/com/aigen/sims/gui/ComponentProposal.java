package com.aigen.sims.gui;
import java.util.UUID;
public class ComponentProposal {
    public final String id, componentType, componentName, insertAfter, code, description, modelName;
    public final long timestamp;
    public final String status;
    public final int hexQ, hexR;
    public ComponentProposal(String componentType, String componentName,
                             String insertAfter, String code,
                             String description, String modelName,
                             int hexQ, int hexR) {
        this.id = UUID.randomUUID().toString().substring(0,8);
        this.componentType=componentType; this.componentName=componentName;
        this.insertAfter=insertAfter; this.code=code;
        this.description=description; this.modelName=modelName;
        this.timestamp=System.currentTimeMillis(); this.status="PENDING";
        this.hexQ=hexQ; this.hexR=hexR;
    }
    private ComponentProposal(String id, String ct, String cn, String ia,
                              String c, String d, String m, long t,
                              String s, int hq, int hr) {
        this.id=id; this.componentType=ct; this.componentName=cn;
        this.insertAfter=ia; this.code=c; this.description=d;
        this.modelName=m; this.timestamp=t; this.status=s;
        this.hexQ=hq; this.hexR=hr;
    }
    public ComponentProposal withStatus(String s) {
        return new ComponentProposal(id,componentType,componentName,insertAfter,
            code,description,modelName,timestamp,s,hexQ,hexR);
    }
    public boolean isPending() { return status.equals("PENDING"); }
    public boolean isApproved() { return status.equals("APPROVED"); }
    public boolean isDeployed() { return status.equals("DEPLOYED"); }
    public boolean isRejected() { return status.equals("REJECTED"); }
}
