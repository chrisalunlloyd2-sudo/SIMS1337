package com.aigen.sims.lora;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
public class AdapterRegistry {
    private final ConcurrentHashMap<String, LoRAAdapter> adapters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<AdapterVote>> votes = new ConcurrentHashMap<>();
    public String registerAdapter(LoRAAdapter a) { adapters.put(a.id, a); return a.id; }
    public void castVote(String adapterId, String voter, double weight) {
        LoRAAdapter a = adapters.get(adapterId);
        if (a == null) return;
        votes.computeIfAbsent(adapterId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new AdapterVote(voter, weight, System.currentTimeMillis()));
        adapters.put(adapterId, a.withVoteCount(a.voteCount + 1));
    }
    public String runElection(String taskType) {
        List<LoRAAdapter> candidates = getAdaptersByTask(taskType);
        if (candidates.isEmpty()) return null;
        LoRAAdapter winner = candidates.stream().max((a,b) -> Double.compare(
            a.score + a.voteCount * 0.1, b.score + b.voteCount * 0.1)).orElse(null);
        if (winner == null) return null;
        for (LoRAAdapter a : candidates) {
            adapters.put(a.id, a.id.equals(winner.id) ? a.withStatus("ACTIVE") : a.withStatus("TESTING"));
        }
        return winner.id;
    }
    public void updateScore(String id, double s) {
        LoRAAdapter a = adapters.get(id);
        if (a != null) adapters.put(id, a.withScore(s));
    }
    public LoRAAdapter getActiveAdapter(String model, String task) {
        return adapters.values().stream().filter(a -> a.modelName.equals(model) && a.taskType.equals(task) && a.isActive()).findFirst().orElse(null);
    }
    public List<LoRAAdapter> getAdaptersByTask(String t) { return adapters.values().stream().filter(a -> a.taskType.equals(t)).collect(Collectors.toList()); }
    public List<LoRAAdapter> getAdaptersByModel(String m) { return adapters.values().stream().filter(a -> a.modelName.equals(m)).collect(Collectors.toList()); }
    public ElectionResult getElectionResult(String t) {
        List<LoRAAdapter> c = getAdaptersByTask(t);
        LoRAAdapter w = c.stream().filter(LoRAAdapter::isActive).findFirst().orElse(null);
        return new ElectionResult(t, c, w, c.stream().mapToDouble(a -> a.score + a.voteCount * 0.1).max().orElse(0));
    }
    public Map<String,Integer> getSummary() {
        Map<String,Integer> m = new HashMap<>();
        m.put("total",adapters.size()); m.put("active",(int)adapters.values().stream().filter(LoRAAdapter::isActive).count());
        m.put("testing",(int)adapters.values().stream().filter(LoRAAdapter::isTesting).count());
        m.put("deprecated",(int)adapters.values().stream().filter(LoRAAdapter::isDeprecated).count()); return m;
    }
    public static class AdapterVote { public final String voter; public final double weight; public final long timestamp;
        public AdapterVote(String v, double w, long t) { voter=v; weight=w; timestamp=t; } }
    public static class ElectionResult {
        public final String taskType; public final List<LoRAAdapter> candidates; public final LoRAAdapter winner; public final double winningScore;
        public ElectionResult(String t, List<LoRAAdapter> c, LoRAAdapter w, double s) { taskType=t; candidates=c; winner=w; winningScore=s; }
        public String toString() { return String.format("Election[%s] winner=%s score=%.2f", taskType, winner!=null?winner.modelName:"none", winningScore); }
    }
}
