package com.aigen.sims.lora;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class LoRATuner {
    private final AdapterRegistry registry;
    private final ConcurrentHashMap<String, TaskPerf> perf = new ConcurrentHashMap<>();
    public LoRATuner(AdapterRegistry r) { this.registry = r; }
    public void recordResult(String model, String task, boolean ok, long ms) {
        String key = model + ":" + task;
        perf.compute(key, (k, v) -> {
            if (v == null) v = new TaskPerf(model, task);
            v.total++; if (ok) v.success++; v.duration += ms; v.lastRun = System.currentTimeMillis(); return v;
        });
    }
    public LoRAAdapter getBestAdapter(String task) {
        String best = null; double bestRate = 0;
        for (TaskPerf tp : perf.values()) {
            if (!tp.taskType.equals(task)) continue;
            double rate = tp.total > 0 ? (double) tp.success / tp.total : 0;
            if (rate > bestRate) { bestRate = rate; best = tp.modelName; }
        }
        return best != null ? registry.getActiveAdapter(best, task) : null;
    }
    public TuningReport runTuningCycle() {
        int elections = 0, activated = 0;
        for (String t : Arrays.asList("codegen","review","mining","voting","deploy")) {
            String w = registry.runElection(t);
            if (w != null) { elections++; activated++; }
        }
        return new TuningReport(elections, activated, registry.getSummary(), System.currentTimeMillis());
    }
    public String getPerfReport() {
        StringBuilder sb = new StringBuilder("📊 LoRA Performance\n");
        for (TaskPerf tp : perf.values()) {
            double rate = tp.total > 0 ? (double) tp.success / tp.total * 100 : 0;
            sb.append(String.format("   %s/%s: %d/%d (%.0f%%) avg %dms\n", tp.modelName, tp.taskType, tp.success, tp.total, rate, tp.total > 0 ? tp.duration / tp.total : 0));
        }
        return sb.toString();
    }
    public static class TaskPerf { public final String modelName, taskType; public int total, success; public long duration, lastRun;
        public TaskPerf(String m, String t) { modelName=m; taskType=t; } }
    public static class TuningReport {
        public final int elections, activated; public final Map<String,Integer> summary; public final long timestamp;
        public TuningReport(int e, int a, Map<String,Integer> s, long t) { elections=e; activated=a; summary=s; timestamp=t; }
        public String toEmailString() { return String.format("🔧 LoRA Tuning\n   Elections: %d\n   Activated: %d\n   Total: %d | Active: %d | Testing: %d | Deprecated: %d",
            elections, activated, summary.getOrDefault("total",0), summary.getOrDefault("active",0), summary.getOrDefault("testing",0), summary.getOrDefault("deprecated",0)); }
    }
}
