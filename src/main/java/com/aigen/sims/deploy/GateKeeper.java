package com.aigen.sims.deploy;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
public class GateKeeper {
    private final ConcurrentHashMap<String, DeployProposal> proposals = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<GateLog>> logs = new ConcurrentHashMap<>();
    private final GitBackupManager backup;
    private final String repoPath;
    public GateKeeper(String repoPath) { this.repoPath = repoPath; this.backup = new GitBackupManager(repoPath); }
    public String submitProposal(DeployProposal p) {
        proposals.put(p.id, p); log(p.id,"SUBMITTED","system",determineGateLevel(p).toString()); return p.id;
    }
    public String approveProposal(String id) throws Exception {
        DeployProposal p = proposals.get(id);
        if (p == null || !p.isPending()) return "ERROR: not found or not pending";
        GateLevel level = determineGateLevel(p);
        switch (level) {
            case AUTO: return executeMerge(p);
            case QUORUM: proposals.put(id, p.withStatus("APPROVED")); log(id,"APPROVED","quorum",""); return executeMerge(p);
            case HUMAN: proposals.put(id, p.withStatus("APPROVED")); log(id,"APPROVED","human",""); return executeMerge(p);
            default: return "ERROR: unknown level";
        }
    }
    // 2026-07-31 (NyxGate, Architect gist 3.3): lets the symbolic gate repair-and-retry a pending
    // proposal's content in place before it's approved -- same id, same log trail.
    public boolean updateProposalContent(String id, String repairedContent) {
        DeployProposal p = proposals.get(id);
        if (p == null || !p.isPending()) return false;
        proposals.put(id, p.withNewContent(repairedContent));
        log(id, "CONTENT_REPAIRED", "nyx-gate", "");
        return true;
    }
    public boolean rejectProposal(String id) {
        DeployProposal p = proposals.get(id);
        if (p == null || !p.isPending()) return false;
        proposals.put(id, p.withStatus("REJECTED")); log(id,"REJECTED","user",""); return true;
    }
    private String executeMerge(DeployProposal p) throws Exception {
        String backupBranch = backup.createBackup("pre-deploy-"+p.id);
        java.nio.file.Files.writeString(java.nio.file.Paths.get(repoPath, p.filePath), p.newContent);
        execGit("add","-A"); execGit("commit","-m","deploy: "+p.description+" [proposal "+p.id+"]");
        proposals.put(p.id, p.withStatus("MERGED"));
        log(p.id,"MERGED","system","backup: "+backupBranch);
        return "MERGED (backup: "+backupBranch+")";
    }
    public GateLevel determineGateLevel(DeployProposal p) {
        String d = p.diff != null ? p.diff.toLowerCase() : "";
        if (d.contains("pom.xml")||d.contains("build.gradle")||d.contains("application.properties")||
            d.contains("docker")||d.contains(".github/")||d.contains("workflows/")) return GateLevel.HUMAN;
        if (d.contains("new file")&&(d.contains("class ")||d.contains("interface ")||d.contains("enum ")))
            return GateLevel.QUORUM;
        return GateLevel.AUTO;
    }
    public List<DeployProposal> getProposals() { return new ArrayList<>(proposals.values()); }
    public List<DeployProposal> getProposalsByStatus(String s) {
        return proposals.values().stream().filter(p->p.status.equals(s)).collect(Collectors.toList());
    }
    public DeployProposal getProposal(String id) { return proposals.get(id); }
    public List<GateLog> getLogs(String id) { return logs.getOrDefault(id,new ArrayList<>()); }
    public Map<String,Integer> getSummary() {
        Map<String,Integer> m = new HashMap<>();
        m.put("total",proposals.size()); m.put("pending",(int)proposals.values().stream().filter(p->p.isPending()).count());
        m.put("approved",(int)proposals.values().stream().filter(p->p.isApproved()).count());
        m.put("rejected",(int)proposals.values().stream().filter(p->p.isRejected()).count());
        m.put("merged",(int)proposals.values().stream().filter(p->p.isMerged()).count()); return m;
    }
    private void log(String id, String a, String actor, String d) {
        logs.computeIfAbsent(id,k->Collections.synchronizedList(new ArrayList<>()))
            .add(new GateLog(a,actor,d,System.currentTimeMillis()));
    }
    private String execGit(String... args) throws Exception {
        List<String> cmd = new ArrayList<>(Arrays.asList("git"));
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(repoPath)); pb.redirectErrorStream(true);
        Process p = pb.start(); p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        return new String(p.getInputStream().readAllBytes()).trim();
    }
    public enum GateLevel { AUTO, QUORUM, HUMAN }
    public static class GateLog {
        public final String action, actor, detail; public final long timestamp;
        public GateLog(String a, String actor, String d, long t) { this.action=a; this.actor=actor; this.detail=d; this.timestamp=t; }
    }
}
