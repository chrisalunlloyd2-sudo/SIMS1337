package com.aigen.sims.engine;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KVStore — simple file-backed key-value store for agent memory persistence.
 * Each key maps to a JSON value string; writes are atomic (tmp + rename).
 * Mirrors the SOV KV pattern: typed keys, history kept in .bak files.
 */
public class KVStore {
    private final Path dir;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public KVStore(String directory) throws IOException {
        this.dir = Paths.get(directory);
        Files.createDirectories(dir);
        load();
    }

    private void load() throws IOException {
        if (!Files.exists(dir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : ds) {
                String key = p.getFileName().toString().replace(".json", "");
                cache.put(key, new String(Files.readAllBytes(p), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
    }

    public synchronized void put(String key, String value) throws IOException {
        cache.put(key, value);
        Path p = dir.resolve(key + ".json");
        Path tmp = dir.resolve(key + ".json.tmp");
        Files.write(tmp, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    public String getOr(String key, String def) { return cache.getOrDefault(key, def); }

    public Set<String> keys() { return new HashSet<>(cache.keySet()); }

    public synchronized void delete(String key) throws IOException {
        cache.remove(key);
        Files.deleteIfExists(dir.resolve(key + ".json"));
    }

    public int size() { return cache.size(); }
}
