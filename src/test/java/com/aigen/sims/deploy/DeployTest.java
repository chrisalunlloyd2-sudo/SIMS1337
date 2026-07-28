package com.aigen.sims.deploy;
import com.aigen.sims.mining.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
public class DeployTest {
    private static int passed = 0, failed = 0;
    static Path tmpDir;

    public static void main(String[] args) throws Exception {
        tmpDir = Files.createTempDirectory("deploy-test");
        System.out.println("=== Deploy Phase 3 Tests ===\n");

        testAutoGate();
        testQuorumGate();
        testHumanGate();
        testLifecycle();
        testLogging();
        testSummary();
        testInsertCode();
        testDiff();
        testFullCycle();

        deleteRecursive(tmpDir.toFile());
        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String n, boolean c) {
        if (c) { passed++; System.out.println("  ✅ " + n); }
        else { failed++; System.out.println("  ❌ " + n + " FAILED"); }
    }

    static void testAutoGate() {
        System.out.println("testAutoGate:");
        GateKeeper gk = new GateKeeper("/tmp");
        GateKeeper.GateLevel l = gk.determineGateLevel(
            new DeployProposal("r","T.java","c","c2","+m()","m","s1",0,0,"t"));
        check("simple method = AUTO", l == GateKeeper.GateLevel.AUTO);
    }

    static void testQuorumGate() {
        System.out.println("\ntestQuorumGate:");
        GateKeeper gk = new GateKeeper("/tmp");
        GateKeeper.GateLevel l = gk.determineGateLevel(
            new DeployProposal("r","N.java","","public class N{}","new file: N.java\n+class N","m","s2",0,0,"t"));
        check("new class = QUORUM", l == GateKeeper.GateLevel.QUORUM);
    }

    static void testHumanGate() {
        System.out.println("\ntestHumanGate:");
        GateKeeper gk = new GateKeeper("/tmp");
        GateKeeper.GateLevel l = gk.determineGateLevel(
            new DeployProposal("r","pom.xml","<p/>","<p><d/></p>","pom.xml | 2 +-","m","s3",0,0,"t"));
        check("pom.xml = HUMAN", l == GateKeeper.GateLevel.HUMAN);
    }

    static void testLifecycle() {
        System.out.println("\ntestLifecycle:");
        GateKeeper gk = new GateKeeper("/tmp");
        String id = gk.submitProposal(new DeployProposal("r","T.java","c","c2","+m","m","s1",0,0,"t"));
        check("submitted = PENDING", "PENDING".equals(gk.getProposal(id).status));
        check("reject ok", gk.rejectProposal(id));
        check("rejected = REJECTED", "REJECTED".equals(gk.getProposal(id).status));
    }

    static void testLogging() {
        System.out.println("\ntestLogging:");
        GateKeeper gk = new GateKeeper("/tmp");
        String id = gk.submitProposal(new DeployProposal("r","T.java","c","c2","+m","m","s1",0,0,"t"));
        gk.rejectProposal(id);
        check("2 log entries", gk.getLogs(id).size() == 2);
    }

    static void testSummary() {
        System.out.println("\ntestSummary:");
        GateKeeper gk = new GateKeeper("/tmp");
        gk.submitProposal(new DeployProposal("r","T.java","c","c2","+m","m","s1",0,0,"1"));
        String id = gk.submitProposal(new DeployProposal("r","T2.java","c","c2","+m","m","s2",0,0,"2"));
        gk.rejectProposal(id);
        Map<String,Integer> s = gk.getSummary();
        check("total = 2", s.get("total") == 2);
        check("pending = 1", s.get("pending") == 1);
    }

    static void testInsertCode() {
        System.out.println("\ntestInsertCode:");
        DeployOrchestrator o = new DeployOrchestrator("/tmp");
        String r = o.insertCode("class T {\n}\n", "}", "\n    void m() {}\n");
        check("code inserted", r.contains("void m()"));
        check("original preserved", r.contains("class T"));
    }

    static void testDiff() {
        System.out.println("\ntestDiff:");
        DeployOrchestrator o = new DeployOrchestrator("/tmp");
        String d = o.generateDiff("a\nb\n", "a\nc\n");
        check("diff contains +c", d.contains("+c"));
    }

    static void testFullCycle() throws Exception {
        System.out.println("\ntestFullCycle:");
        Path repo = Files.createTempDirectory(tmpDir, "repo");
        new ProcessBuilder("git","init","-b","main").directory(repo.toFile()).start()
            .waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        new ProcessBuilder("git","config","user.email","test@test").directory(repo.toFile())
            .start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        new ProcessBuilder("git","config","user.name","test").directory(repo.toFile())
            .start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        Path src = repo.resolve("src/Test.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "class T {}\n");
        new ProcessBuilder("git","add","-A").directory(repo.toFile()).start()
            .waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        new ProcessBuilder("git","commit","-m","init").directory(repo.toFile()).start()
            .waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

        Path sugDir = Files.createTempDirectory(tmpDir, "sug");
        SuggestionRegistry reg = new SuggestionRegistry(sugDir.toString());
        Suggestion s = new Suggestion("repo","src/Test.java","}","\n    void m() {}\n",
            "qwen2.5:0.5b",0,0,"add m");
        reg.addSuggestion(s); reg.approveSuggestion(s.id);

        DeployOrchestrator o = new DeployOrchestrator(repo.toString());
        DeployOrchestrator.DeployCycleReport r = o.runDeployCycle(reg, repo.toString());
        check("deployed = 1", r.deployed == 1);
        String fileContent = Files.readString(src);
        System.out.println("  [debug] file: " + fileContent.replace("\n","\\n"));
        check("file has void m()", fileContent.contains("void m()"));
        check("file has class T", fileContent.contains("class T"));
    }

    static void deleteRecursive(File f) {
        if (f.isDirectory()) { File[] kids = f.listFiles(); if (kids != null) for (File c : kids) deleteRecursive(c); }
        f.delete();
    }
}
