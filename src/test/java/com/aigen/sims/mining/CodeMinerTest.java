package com.aigen.sims.mining;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class CodeMinerTest {
    private static int passed = 0, failed = 0;
    static Path tmpDir;

    public static void main(String[] args) throws Exception {
        tmpDir = Files.createTempDirectory("miner-test");
        System.out.println("=== CodeMiner Phase 2 Tests ===\n");

        testEmptyDir();
        testRepoDetect();
        testPatterns();
        testLifecycle();
        testReject();
        testPending();
        testByRepo();
        testByModel();
        testSummary();
        testHistory();
        testHexDeterministic();

        // Cleanup
        deleteRecursive(tmpDir.toFile());
        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String n, boolean c) {
        if (c) { passed++; System.out.println("  ✅ " + n); }
        else { failed++; System.out.println("  ❌ " + n + " FAILED"); }
    }

    static Path tempDir() throws Exception {
        Path d = Files.createTempDirectory(tmpDir, "test");
        return d;
    }

    static void testEmptyDir() throws Exception {
        System.out.println("testEmptyDir:");
        Path d = tempDir();
        check("empty dir = 0 repos", new CodeMiner(d.toString()).scanAllRepos().size() == 0);
    }

    static void testRepoDetect() throws Exception {
        System.out.println("\ntestRepoDetect:");
        Path d = tempDir();
        Files.createDirectories(d.resolve("r/.git"));
        check("dir with .git = 1 repo", new CodeMiner(d.toString()).scanAllRepos().size() == 1);
    }

    static void testPatterns() {
        System.out.println("\ntestPatterns:");
        Map<String,String> f = new HashMap<>();
        f.put("T.java","package x;\nimport java.util.List;\npublic class T {\npublic void m() {}\n}\n");
        List<String> p = new CodeMiner("/tmp").extractPatterns(f);
        check("found class:T", p.contains("class:T"));
        check("found method:m", p.contains("method:m"));
        check("found import:java.util.List", p.contains("import:java.util.List"));
    }

    static void testLifecycle() throws Exception {
        System.out.println("\ntestLifecycle:");
        Path d = tempDir();
        SuggestionRegistry r = new SuggestionRegistry(d.toString());
        String id = r.addSuggestion(new Suggestion("r","T.java","}","//c","m",0,0,"t"));
        check("status = PENDING", "PENDING".equals(r.getSuggestion(id).status));
        check("approve ok", r.approveSuggestion(id));
        check("status = APPROVED", "APPROVED".equals(r.getSuggestion(id).status));
        check("mark deploy ok", r.markDeployed(id));
        check("status = DEPLOYED", "DEPLOYED".equals(r.getSuggestion(id).status));
        check("can't re-approve deployed", !r.approveSuggestion(id));
    }

    static void testReject() throws Exception {
        System.out.println("\ntestReject:");
        Path d = tempDir();
        SuggestionRegistry r = new SuggestionRegistry(d.toString());
        String id = r.addSuggestion(new Suggestion("r","T.java","}","//c","m",0,0,"t"));
        check("reject ok", r.rejectSuggestion(id));
        check("status = REJECTED", "REJECTED".equals(r.getSuggestion(id).status));
        check("can't approve rejected", !r.approveSuggestion(id));
    }

    static void testPending() throws Exception {
        System.out.println("\ntestPending:");
        Path d = tempDir();
        SuggestionRegistry r = new SuggestionRegistry(d.toString());
        r.addSuggestion(new Suggestion("r1","A.java","}","c1","m1",0,0,"1"));
        r.addSuggestion(new Suggestion("r2","B.java","}","c2","m2",1,0,"2"));
        check("2 pending", r.getPendingSuggestions().size() == 2);
        r.approveSuggestion(r.getPendingSuggestions().get(0).id);
        check("1 pending after approve", r.getPendingSuggestions().size() == 1);
    }

    static void testByRepo() throws Exception {
        System.out.println("\ntestByRepo:");
        Path d = tempDir();
        SuggestionRegistry r = new SuggestionRegistry(d.toString());
        r.addSuggestion(new Suggestion("r1","A.java","}","c1","m1",0,0,"1"));
        r.addSuggestion(new Suggestion("r1","B.java","}","c2","m2",0,0,"2"));
        r.addSuggestion(new Suggestion("r2","C.java","}","c3","m3",1,0,"3"));
        check("r1 has 2", r.getSuggestionsByRepo("r1").size() == 2);
        check("r2 has 1", r.getSuggestionsByRepo("r2").size() == 1);
    }

    static void testByModel() throws Exception {
        System.out.println("\ntestByModel:");
        Path d = tempDir();
        SuggestionRegistry r = new SuggestionRegistry(d.toString());
        r.addSuggestion(new Suggestion("r1","A.java","}","c1","qwen2.5:0.5b",0,0,"1"));
        r.addSuggestion(new Suggestion("r2","B.java","}","c2","deepseek-r1:1.5b",1,0,"2"));
        check("qwen has 1", r.getSuggestionsByModel("qwen2.5:0.5b").size() == 1);
    }

    static void testSummary() throws Exception {
        System.out.println("\ntestSummary:");
        Path d = tempDir();
        SuggestionRegistry r = new SuggestionRegistry(d.toString());
        r.addSuggestion(new Suggestion("r1","A.java","}","c1","m1",0,0,"1"));
        String id = r.addSuggestion(new Suggestion("r2","B.java","}","c2","m2",1,0,"2"));
        r.approveSuggestion(id);
        Map<String,Integer> s = r.getSummary();
        check("total = 2", s.get("total") == 2);
        check("pending = 1", s.get("pending") == 1);
        check("approved = 1", s.get("approved") == 1);
    }

    static void testHistory() throws Exception {
        System.out.println("\ntestHistory:");
        Path d = tempDir();
        SuggestionRegistry r = new SuggestionRegistry(d.toString());
        String id = r.addSuggestion(new Suggestion("r","A.java","}","c","m",0,0,"t"));
        r.approveSuggestion(id); r.markDeployed(id);
        check("3 history entries", r.getHistory(id).size() == 3);
    }

    static void testHexDeterministic() {
        System.out.println("\ntestHexDeterministic:");
        CodeMiner m = new CodeMiner("/tmp");
        int[] a = m.hashToHex("SIMS1337");
        int[] b = m.hashToHex("SIMS1337");
        check("hex deterministic", a[0] == b[0] && a[1] == b[1]);
    }

    static void deleteRecursive(File f) {
        if (f.isDirectory()) for (File c : f.listFiles()) deleteRecursive(c);
        f.delete();
    }
}
