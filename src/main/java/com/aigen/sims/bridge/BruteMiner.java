package com.aigen.sims.bridge;

import com.aigen.sims.mining.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * BruteMiner — Wraps Brute Foundry output as SIMS1337 mining suggestions.
 * Brute Foundry generates deterministic AST blocks.
 * This bridge converts them to Suggestion objects for the pipeline.
 */
public class BruteMiner {

    private final String bruteFoundryPath;
    private final SuggestionRegistry registry;

    public BruteMiner(String bruteFoundryPath, SuggestionRegistry registry) {
        this.bruteFoundryPath = bruteFoundryPath;
        this.registry = registry;
    }

    /**
     * Scan Brute Foundry output directory for AST blocks.
     * Each block becomes a Suggestion.
     */
    public int harvestBlocks() {
        int count = 0;
        File blocksDir = new File(bruteFoundryPath, "blocks");
        if (!blocksDir.isDirectory()) return 0;

        File[] blocks = blocksDir.listFiles((dir, name) -> name.endsWith(".ast"));
        if (blocks == null) return 0;

        for (File block : blocks) {
            try {
                String content = Files.readString(block.toPath());
                String name = block.getName().replace(".ast", "");
                
                // Parse the AST block header for metadata
                String repoName = "brute-foundry";
                String filePath = "src/generated/" + name + ".java";
                String insertAfter = "}";
                String modelName = "brute-foundry";
                int[] hex = hashToHex(name);
                
                Suggestion s = new Suggestion(
                    repoName, filePath, insertAfter, content,
                    modelName, hex[0], hex[1],
                    "Brute Foundry AST block: " + name
                );
                
                registry.addSuggestion(s);
                count++;
                
                // Move processed block to archive
                Path archive = Paths.get(bruteFoundryPath, "blocks_processed", block.getName());
                Files.createDirectories(archive.getParent());
                Files.move(block.toPath(), archive);
                
            } catch (Exception e) {
                System.err.println("⚠️ BruteMiner: error processing " + block.getName() + ": " + e.getMessage());
            }
        }
        return count;
    }

    private int[] hashToHex(String name) {
        int hash = Math.abs(name.hashCode());
        return new int[]{(hash % 7) - 3, ((hash / 7) % 7) - 3};
    }
}
