package com.aigen.sims.mining;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
public class SuggestionRegistry {
    private final ConcurrentHashMap<String, Suggestion> suggestions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<StatusChange>> history = new ConcurrentHashMap<>();
    private final String storagePath;
    public SuggestionRegistry(String storagePath) { this.storagePath = storagePath; loadFromDisk(); }
    public String addSuggestion(Suggestion s) {
        suggestions.put(s.id, s); addHistory(s.id, "CREATED", s.modelName); saveToDisk(); return s.id;
    }
    public boolean approveSuggestion(String id) {
        Suggestion s = suggestions.get(id);
        if (s == null || !s.status.equals("PENDING")) return false;
        suggestions.put(id, s.withStatus("APPROVED")); addHistory(id, "APPROVED", "user"); saveToDisk(); return true;
    }
    public boolean rejectSuggestion(String id) {
        Suggestion s = suggestions.get(id);
        if (s == null || !s.status.equals("PENDING")) return false;
        suggestions.put(id, s.withStatus("REJECTED")); addHistory(id, "REJECTED", "user"); saveToDisk(); return true;
    }
    public boolean markDeployed(String id) {
        Suggestion s = suggestions.get(id);
        if (s == null || !s.status.equals("APPROVED")) return false;
        suggestions.put(id, s.withStatus("DEPLOYED")); addHistory(id, "DEPLOYED", "system"); saveToDisk(); return true;
    }
    public List<Suggestion> getPendingSuggestions() {
        return suggestions.values().stream().filter(s -> s.status.equals("PENDING"))
            .sorted((a,b) -> Long.compare(b.timestamp, a.timestamp)).collect(Collectors.toList());
    }
    public List<Suggestion> getSuggestionsByRepo(String repo) {
        return suggestions.values().stream().filter(s -> s.repoName.equals(repo))
            .sorted((a,b) -> Long.compare(b.timestamp, a.timestamp)).collect(Collectors.toList());
    }
    public List<Suggestion> getSuggestionsByModel(String model) {
        return suggestions.values().stream().filter(s -> s.modelName.equals(model))
            .sorted((a,b) -> Long.compare(b.timestamp, a.timestamp)).collect(Collectors.toList());
    }
    public Suggestion getSuggestion(String id) { return suggestions.get(id); }
    public List<Suggestion> getAllSuggestions() { return new ArrayList<>(suggestions.values()); }
    public List<StatusChange> getHistory(String id) { return history.getOrDefault(id, new ArrayList<>()); }
    public Map<String, Integer> getSummary() {
        Map<String, Integer> m = new HashMap<>();
        m.put("total", suggestions.size());
        m.put("pending", (int)suggestions.values().stream().filter(s->s.status.equals("PENDING")).count());
        m.put("approved", (int)suggestions.values().stream().filter(s->s.status.equals("APPROVED")).count());
        m.put("rejected", (int)suggestions.values().stream().filter(s->s.status.equals("REJECTED")).count());
        m.put("deployed", (int)suggestions.values().stream().filter(s->s.status.equals("DEPLOYED")).count());
        return m;
    }
    private void addHistory(String sid, String action, String actor) {
        history.computeIfAbsent(sid, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new StatusChange(action, actor, System.currentTimeMillis()));
    }
    private void saveToDisk() {
        try {
            Files.createDirectories(Paths.get(storagePath));
            for (Suggestion s : suggestions.values()) {
                String json = String.format("{\"id\":\"%s\",\"repoName\":\"%s\",\"filePath\":\"%s\",\"insertAfter\":\"%s\",\"code\":\"%s\",\"modelName\":\"%s\",\"timestamp\":%d,\"status\":\"%s\",\"hexQ\":%d,\"hexR\":%d,\"description\":\"%s\"}",
                    s.id, s.repoName, s.filePath, s.insertAfter,
                    s.code.replace("\"","\\\"").replace("\n","\\n"),
                    s.modelName, s.timestamp, s.status, s.hexQ, s.hexR,
                    s.description.replace("\"","\\\""));
                Files.writeString(Paths.get(storagePath, s.id + ".json"), json);
            }
        } catch (IOException e) { System.err.println("Save error: " + e.getMessage()); }
    }
    private void loadFromDisk() {
        try {
            File dir = new File(storagePath);
            if (!dir.isDirectory()) return;
            File[] files = dir.listFiles((d,n) -> n.endsWith(".json"));
            if (files == null) return;
        } catch (Exception e) {}
    }
    public static class StatusChange {
        public final String action, actor; public final long timestamp;
        public StatusChange(String action, String actor, long timestamp) {
            this.action = action; this.actor = actor; this.timestamp = timestamp;
        }
    }
}
