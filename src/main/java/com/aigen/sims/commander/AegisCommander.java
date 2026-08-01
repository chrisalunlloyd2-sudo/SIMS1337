package com.aigen.sims.commander;

import com.aigen.sims.mining.Suggestion;
import com.aigen.sims.mining.SuggestionRegistry;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * AegisCommander -- 2026-07-28 Architect gist, section 3.1: "Aegis reads strategic priorities and
 * generates a strategy.json that SIMS1337 consumes before each night cycle."
 *
 * The gist's reference design has Aegis read "global_keywords"/"nightly_insights" from a phone-side
 * SOV KV store that doesn't exist on this machine (same translation problem already solved for the
 * Python heartbeat_harvester.py this session: adapt to a REAL local signal, don't fake a client for
 * a store that isn't there). The real, already-built, in-process signal available here is
 * SuggestionRegistry itself -- which repos have pending work piling up, and which models' past
 * suggestions actually got approved vs rejected. That IS a real strategic-priority signal: it
 * reflects genuine crew outcomes, not an invented one.
 */
public class AegisCommander {
    private final SuggestionRegistry registry;
    private final String strategyPath;

    public AegisCommander(SuggestionRegistry registry, String strategyPath) {
        this.registry = registry;
        this.strategyPath = strategyPath;
    }

    /** Real derivation: focusRepo = repo with the most PENDING suggestions (needs attention);
     * focusModel = the model with the best real approval rate among models with enough history.
     * Both are null (not guessed) when there isn't real data to justify a pick. */
    public Strategy generateStrategy() {
        Map<String, Integer> summary = registry.getSummary();
        List<Suggestion> all = registry.getAllSuggestions();

        Map<String, Integer> pendingByRepo = new HashMap<>();
        Map<String, int[]> outcomesByModel = new HashMap<>();   // [approved, rejected]
        for (Suggestion s : all) {
            if ("PENDING".equals(s.status)) {
                pendingByRepo.merge(s.repoName, 1, Integer::sum);
            }
            int[] o = outcomesByModel.computeIfAbsent(s.modelName, k -> new int[2]);
            if ("APPROVED".equals(s.status) || "DEPLOYED".equals(s.status)) o[0]++;
            else if ("REJECTED".equals(s.status)) o[1]++;
        }

        String focusRepo = pendingByRepo.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0)
            .map(Map.Entry::getKey).orElse(null);

        String focusModel = outcomesByModel.entrySet().stream()
            .filter(e -> e.getValue()[0] + e.getValue()[1] >= 1)
            .max(Comparator.comparingDouble(e -> {
                int[] o = e.getValue();
                return (o[0] + 1.0) / (o[0] + o[1] + 2.0);   // Laplace-smoothed, same real discipline as bridge.py's route learn
            }))
            .map(Map.Entry::getKey).orElse(null);

        String reason;
        if (focusRepo == null && focusModel == null) {
            reason = "no real suggestion history yet -- nothing to prioritize, mining runs unguided this cycle";
        } else {
            reason = String.format("%s has the most real pending work (%d); %s has the best real approval rate seen so far",
                focusRepo == null ? "(no repo)" : focusRepo,
                focusRepo == null ? 0 : pendingByRepo.get(focusRepo),
                focusModel == null ? "(no model)" : focusModel);
        }

        return new Strategy(focusRepo, focusModel, reason, System.currentTimeMillis(),
            summary.getOrDefault("pending", 0), summary.getOrDefault("approved", 0),
            summary.getOrDefault("rejected", 0), summary.getOrDefault("deployed", 0));
    }

    /** Atomic write (temp + rename), same convention used across the Viper stack. */
    public void writeStrategy(Strategy s) throws IOException {
        Path target = Paths.get(strategyPath);
        Path tmp = Paths.get(strategyPath + ".tmp");
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Files.writeString(tmp, s.toJson());
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
