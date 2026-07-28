package com.aigen.sims.lora;
import java.util.*;
public class LoRATest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("=== LoRA Phase 4 Tests ===\n");
        testRegister();
        testVote();
        testElection();
        testActiveAdapter();
        testRecordResult();
        testBestAdapter();
        testTuningCycle();
        testSummary();
        testElectionResult();
        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String n, boolean c) {
        if (c) { passed++; System.out.println("  ✅ " + n); }
        else { failed++; System.out.println("  ❌ " + n + " FAILED"); }
    }

    static void testRegister() {
        System.out.println("testRegister:");
        AdapterRegistry r = new AdapterRegistry();
        String id = r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/adapters/qwen-code.gguf"));
        check("id not null", id != null);
        check("total = 1", r.getSummary().get("total") == 1);
    }

    static void testVote() {
        System.out.println("\ntestVote:");
        AdapterRegistry r = new AdapterRegistry();
        String id = r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/a/q.gguf"));
        r.castVote(id, "tinyllama:1.1b", 0.5);
        r.castVote(id, "phi:latest", 0.8);
        check("voteCount = 2", r.getAdaptersByTask("codegen").get(0).voteCount == 2);
    }

    static void testElection() {
        System.out.println("\ntestElection:");
        AdapterRegistry r = new AdapterRegistry();
        String id1 = r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/a/q.gguf"));
        String id2 = r.registerAdapter(new LoRAAdapter("deepseek-r1:1.5b", "codegen", "/a/d.gguf"));
        r.updateScore(id2, 0.8);
        String winner = r.runElection("codegen");
        check("winner = id2", id2.equals(winner));
        LoRAAdapter w = r.getAdaptersByTask("codegen").stream()
            .filter(a -> a.id.equals(id2)).findFirst().orElse(null);
        check("winner is active", w != null && w.isActive());
    }

    static void testActiveAdapter() {
        System.out.println("\ntestActiveAdapter:");
        AdapterRegistry r = new AdapterRegistry();
        r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/a/q.gguf"));
        r.runElection("codegen");
        check("active adapter found", r.getActiveAdapter("qwen2.5:0.5b", "codegen") != null);
    }

    static void testRecordResult() {
        System.out.println("\ntestRecordResult:");
        AdapterRegistry r = new AdapterRegistry();
        LoRATuner t = new LoRATuner(r);
        t.recordResult("qwen2.5:0.5b", "codegen", true, 1500);
        t.recordResult("qwen2.5:0.5b", "codegen", true, 1200);
        t.recordResult("qwen2.5:0.5b", "codegen", false, 3000);
        check("perf shows success rate", t.getPerfReport().contains("2/3"));
    }

    static void testBestAdapter() {
        System.out.println("\ntestBestAdapter:");
        AdapterRegistry r = new AdapterRegistry();
        r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/a/q.gguf"));
        r.runElection("codegen");
        LoRATuner t = new LoRATuner(r);
        t.recordResult("qwen2.5:0.5b", "codegen", true, 1000);
        t.recordResult("tinyllama:1.1b", "codegen", false, 2000);
        check("best adapter found", t.getBestAdapter("codegen") != null);
    }

    static void testTuningCycle() {
        System.out.println("\ntestTuningCycle:");
        AdapterRegistry r = new AdapterRegistry();
        r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/a/q.gguf"));
        r.registerAdapter(new LoRAAdapter("deepseek-r1:1.5b", "review", "/a/d.gguf"));
        LoRATuner t = new LoRATuner(r);
        LoRATuner.TuningReport rep = t.runTuningCycle();
        check("elections = 2", rep.elections == 2);
    }

    static void testSummary() {
        System.out.println("\ntestSummary:");
        AdapterRegistry r = new AdapterRegistry();
        r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/a/q.gguf"));
        r.registerAdapter(new LoRAAdapter("deepseek-r1:1.5b", "review", "/a/d.gguf"));
        r.registerAdapter(new LoRAAdapter("phi:latest", "mining", "/a/p.gguf"));
        r.runElection("codegen");
        Map<String,Integer> s = r.getSummary();
        check("total = 3", s.get("total") == 3);
        check("active = 1", s.get("active") == 1);
        check("testing = 2", s.get("testing") == 2);
    }

    static void testElectionResult() {
        System.out.println("\ntestElectionResult:");
        AdapterRegistry r = new AdapterRegistry();
        r.registerAdapter(new LoRAAdapter("qwen2.5:0.5b", "codegen", "/a/q.gguf"));
        r.runElection("codegen");
        AdapterRegistry.ElectionResult er = r.getElectionResult("codegen");
        check("taskType = codegen", "codegen".equals(er.taskType));
        check("winner not null", er.winner != null);
    }
}
