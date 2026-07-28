package com.aigen.sims.deploy;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class GitBackupManager {
    private final String repoPath;
    public GitBackupManager(String repoPath) { this.repoPath = repoPath; }
    public String createBackup(String desc) throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String branch = "backup/" + ts + "-" + desc.replaceAll("[^a-zA-Z0-9_-]","_")
            .substring(0, Math.min(desc.length(), 40));
        String currentBranch = currentBranch();
        execGit("checkout","-b",branch);
        execGit("add","-A");
        execGit("commit","-m","backup: "+desc+" [pre-deploy]");
        execGit("checkout", currentBranch);
        return branch;
    }
    private String currentBranch() throws IOException {
        String out = execGit("rev-parse","--abbrev-ref","HEAD");
        return out.isEmpty() ? "main" : out.trim();
    }
    public List<String> listBackups() throws IOException {
        String out = execGit("branch","--list","backup/*");
        return out.trim().isEmpty() ? new ArrayList<>() : Arrays.asList(out.trim().split("\n"));
    }
    public String getBackupDiff(String branch) throws IOException {
        return execGit("diff","main.."+branch,"--stat");
    }
    private String execGit(String... args) throws IOException {
        List<String> cmd = new ArrayList<>(Arrays.asList("git"));
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(repoPath)); pb.redirectErrorStream(true);
        Process p = pb.start();
        try { String out = new String(p.getInputStream().readAllBytes());
              p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS); return out.trim(); }
        catch (Exception e) { throw new IOException("git failed: "+String.join(" ",args),e); }
    }
}
