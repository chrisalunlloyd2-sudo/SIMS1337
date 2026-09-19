package com.aigen.sims.deploy;
import com.aigen.sims.mining.*;
import java.util.*;
import java.util.stream.Collectors;
public class DeployOrchestrator {
    private final GateKeeper gateKeeper;
    private final String repoPath;
    public DeployOrchestrator(String repoPath) { this.repoPath = repoPath; this.gateKeeper = new GateKeeper(repoPath); }
    // 2026-08-02: additive -- lets a caller (PipelineScheduler) share ONE long-lived GateKeeper across
    // cycles instead of a fresh in-memory one per cycle (GateKeeper has no disk persistence, so a fresh
    // instance every call was silently wiping proposal history each cycle).
    public DeployOrchestrator(String repoPath, GateKeeper gateKeeper) { this.repoPath = repoPath; this.gateKeeper = gateKeeper; }
    public String submitFromSuggestion(Suggestion s, String rp) {
        try {
            java.nio.file.Path fp = java.nio.file.Paths.get(rp, s.filePath);
            String orig = java.nio.file.Files.readString(fp);
            String newC = insertCode(orig, s.insertAfter, s.code);
            String diff = generateDiff(orig, newC);
            DeployProposal p = new DeployProposal(s.repoName, s.filePath, orig, newC, diff,
                s.modelName, s.id, s.hexQ, s.hexR, s.description);
            return gateKeeper.submitProposal(p);
        } catch (Exception e) { return "ERROR: "+e.getMessage(); }
    }
    public DeployCycleReport runDeployCycle(SuggestionRegistry reg, String rp) {
        List<Suggestion> approved = reg.getAllSuggestions().stream()
            .filter(s -> s.status.equals("APPROVED")).collect(Collectors.toList());
        int deployed = 0, errors = 0;
        for (Suggestion s : approved) {
            String pid = submitFromSuggestion(s, rp);
            if (pid.startsWith("ERROR")) { errors++; continue; }
            try {
                // 2026-07-31 (Architect gist 3.3/Step E): this used to call gateKeeper.approveProposal()
                // directly, which meant NyxGate's real symbolic AST check (Step B) never actually ran
                // on the real deploy path -- found while wiring the full pipeline end-to-end.
                com.aigen.sims.gate.NyxGate.NyxResult nyx =
                    com.aigen.sims.gate.NyxGate.verifyAndApprove(gateKeeper, pid);
                if (!nyx.allowed) errors++;
                else { deployed++; reg.markDeployed(s.id); }
            } catch (Exception e) { errors++; }
        }
        return new DeployCycleReport(deployed, errors, gateKeeper.getSummary(), System.currentTimeMillis());
    }
    String insertCode(String content, String marker, String code) {
        int idx = content.lastIndexOf(marker);
        if (idx < 0) return content + "\n" + code;
        int lineEnd = content.indexOf('\n', idx);
        if (lineEnd < 0) lineEnd = content.length();
        return content.substring(0, lineEnd+1) + code + content.substring(lineEnd+1);
    }
    String generateDiff(String orig, String newC) {
        if (orig.equals(newC)) return "no changes";
        String[] o = orig.split("\n"), n = newC.split("\n");
        StringBuilder sb = new StringBuilder("--- original\n+++ new\n @@ -1,"+o.length+" +1,"+n.length+" @@\n");
        for (int i=0; i<Math.max(o.length,n.length); i++) {
            if (i<o.length && i<n.length) {
                if (!o[i].equals(n[i])) { sb.append("-").append(o[i]).append("\n"); sb.append("+").append(n[i]).append("\n"); }
                else sb.append(" ").append(o[i]).append("\n");
            } else if (i<o.length) sb.append("-").append(o[i]).append("\n");
            else sb.append("+").append(n[i]).append("\n");
        }
        return sb.toString();
    }
    public GateKeeper getGateKeeper() { return gateKeeper; }
    public static class DeployCycleReport {
        public final int deployed, errors;
        public final Map<String,Integer> summary;
        public final long timestamp;
        public DeployCycleReport(int d, int e, Map<String,Integer> s, long t) { deployed=d; errors=e; summary=s; timestamp=t; }
        public String toEmailString() {
            return String.format("🚀 Deploy Report\n   Deployed: %d\n   Errors: %d\n   Total: %d | Pending: %d | Approved: %d | Rejected: %d | Merged: %d",
                deployed, errors,
                summary.getOrDefault("total",0), summary.getOrDefault("pending",0),
                summary.getOrDefault("approved",0), summary.getOrDefault("rejected",0),
                summary.getOrDefault("merged",0));
        }
    }
}
