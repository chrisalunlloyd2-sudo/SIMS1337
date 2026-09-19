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
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (Suggestion s : suggestions.values()) {
                // Use Jackson for proper JSON escaping (handles backslashes, quotes, unicode)
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", s.id);
                map.put("repoName", s.repoName);
                map.put("filePath", s.filePath);
                map.put("insertAfter", s.insertAfter);
                map.put("code", s.code);
                map.put("modelName", s.modelName);
                map.put("timestamp", s.timestamp);
                map.put("status", s.status);
                map.put("hexQ", s.hexQ);
                map.put("hexR", s.hexR);
                map.put("description", s.description);
                String json = mapper.writeValueAsString(map);
                Files.writeString(Paths.get(storagePath, s.id + ".json"), json);
            }
        } catch (IOException e) { System.err.println("Save error: " + e.getMessage()); }
    }
    private void loadFromDisk() {
        // 2026-07-31: this used to list the .json files and do NOTHING with them -- suggestions
        // never survived a JVM restart, which silently breaks anything (AegisCommander) that needs
        // to read a PRIOR run's outcomes. Actually parse and rehydrate each one.
        try {
            File dir = new File(storagePath);
            if (!dir.isDirectory()) return;
            File[] files = dir.listFiles((d,n) -> n.endsWith(".json"));
            if (files == null) return;
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (File f : files) {
                try {
                    com.fasterxml.jackson.databind.JsonNode n = mapper.readTree(f);
                    Suggestion s = Suggestion.fromDisk(
                        n.path("id").asText(), n.path("repoName").asText(), n.path("filePath").asText(),
                        n.path("insertAfter").asText(), n.path("code").asText(), n.path("modelName").asText(),
                        n.path("timestamp").asLong(), n.path("status").asText("PENDING"),
                        n.path("hexQ").asInt(), n.path("hexR").asInt(), n.path("description").asText());
                    suggestions.put(s.id, s);
                } catch (Exception e) {
                    System.err.println("Rehydrate skip " + f.getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {}
    }
    public static class StatusChange {
        public final String action, actor; public final long timestamp;
        public StatusChange(String action, String actor, long timestamp) {
            this.action = action; this.actor = actor; this.timestamp = timestamp;
        }
    }
}
