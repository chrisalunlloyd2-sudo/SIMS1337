package com.aigen.sims.bridge;

import com.aigen.sims.mining.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * BruteMiner -- wraps Brute Foundry (the real, deterministic, no-LLM code
 * generator at foundry.py) as a SIMS1337 mining source (2026-07-28 gist,
 * Step 3). Reads mining requests (name/params/checks/doc) from a requests/
 * directory, shells out to the real, verified `python foundry.py mine-json`
 * CLI (brute-foundry commit 7c37ee4), and on a real win registers the
 * ACTUAL generated code as a Suggestion -- not a stub placeholder.
 */
public class BruteMiner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String bruteFoundryPath;
    private final SuggestionRegistry registry;

    public BruteMiner(String bruteFoundryPath, SuggestionRegistry registry) {
        this.bruteFoundryPath = bruteFoundryPath;
        this.registry = registry;
    }

    /**
     * Scan requests/ for pending mining specs, run each through the real
     * Brute Foundry mine-json CLI, and register a Suggestion for every
     * real win. Returns the number of suggestions harvested.
     */
    public int harvestBlocks() {
        int count = 0;
        File requestsDir = new File(bruteFoundryPath, "requests");
        if (!requestsDir.isDirectory()) return 0;

        File[] requests = requestsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (requests == null) return 0;

        for (File req : requests) {
            try {
                JsonNode spec = MAPPER.readTree(req);
                String name = spec.path("name").asText("");
                String params = spec.path("params").asText("");
                String checks = spec.path("checks").asText("");
                String doc = spec.path("doc").asText("");
                if (name.isEmpty() || checks.isEmpty()) {
                    archive(req, "requests_rejected");
                    continue;
                }

                JsonNode result = runMineJson(name, params, checks, doc);
                if (result != null && result.path("mined").asBoolean(false)) {
                    String code = result.path("code").asText("");
                    double health = result.path("health").asDouble(0.0);
                    int[] hex = hashToHex(name);
                    Suggestion s = new Suggestion(
                        spec.path("repoName").asText("brute-foundry"),
                        spec.path("filePath").asText("src/generated/" + name + ".py"),
                        spec.path("insertAfter").asText(""),
                        code, "brute-foundry", hex[0], hex[1],
                        "Brute Foundry mined block: " + name + " (health=" + health + ")"
                    );
                    registry.addSuggestion(s);
                    count++;
                    archive(req, "requests_processed");
                } else {
                    archive(req, "requests_processed");
                }
            } catch (Exception e) {
                System.err.println("BruteMiner: error processing " + req.getName() + ": " + e.getMessage());
            }
        }
        return count;
    }

    /** Shell out to the real `python foundry.py mine-json` CLI and parse its JSON stdout. */
    private JsonNode runMineJson(String name, String params, String checks, String doc) throws IOException, InterruptedException {
        List<String> cmd = Arrays.asList("python", "foundry.py", "mine-json", name, params, checks, doc);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(bruteFoundryPath));
        pb.redirectErrorStream(false);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor(60, TimeUnit.SECONDS);
        if (out.isBlank()) return null;
        return MAPPER.readTree(out.trim());
    }

    private void archive(File req, String subdir) throws IOException {
        Path dest = Paths.get(bruteFoundryPath, subdir, req.getName());
        Files.createDirectories(dest.getParent());
        Files.move(req.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
    }

    private int[] hashToHex(String name) {
        int hash = Math.abs(name.hashCode());
        return new int[]{(hash % 7) - 3, ((hash / 7) % 7) - 3};
    }
}
