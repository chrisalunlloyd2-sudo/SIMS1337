package com.aigen.sims.commander;

import com.aigen.sims.mining.SuggestionRegistry;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * PipelineMonitor -- Architect gist 3.1: "Monitor SIMS1337 pipeline progress and report back."
 * Real delta tracking: persists the last-seen SuggestionRegistry summary to disk and reports the
 * ACTUAL change since the previous check (never a guessed trend).
 */
public class PipelineMonitor {
    private final SuggestionRegistry registry;
    private final String statePath;

    public PipelineMonitor(SuggestionRegistry registry, String statePath) {
        this.registry = registry;
        this.statePath = statePath;
    }

    private Map<String, Integer> loadLast() {
        Map<String, Integer> m = new HashMap<>();
        try {
            for (String line : Files.readAllLines(Paths.get(statePath))) {
                String[] kv = line.split("=", 2);
                if (kv.length == 2) m.put(kv[0], Integer.parseInt(kv[1].trim()));
            }
        } catch (Exception ignored) { }
        return m;
    }

    private void saveNow(Map<String, Integer> summary) {
        try {
            Path target = Paths.get(statePath);
            Path tmp = Paths.get(statePath + ".tmp");
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : summary.entrySet()) sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
            Files.writeString(tmp, sb.toString());
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) { }
    }

    /** Real, honest delta report: what actually changed since the last check. */
    public String report() {
        Map<String, Integer> prev = loadLast();
        Map<String, Integer> now = registry.getSummary();
        StringBuilder sb = new StringBuilder("Pipeline status: ");
        boolean any = false;
        for (String k : new String[]{"pending", "approved", "rejected", "deployed"}) {
            int before = prev.getOrDefault(k, 0), after = now.getOrDefault(k, 0);
            int delta = after - before;
            if (delta != 0) {
                sb.append(String.format("%s %s%d (now %d)  ", k, delta > 0 ? "+" : "", delta, after));
                any = true;
            }
        }
        if (!any) sb.append("no change since last check (").append(now).append(")");
        saveNow(now);
        return sb.toString().trim();
    }
}
