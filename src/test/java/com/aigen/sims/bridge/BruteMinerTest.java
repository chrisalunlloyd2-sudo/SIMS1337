package com.aigen.sims.bridge;

import com.aigen.sims.mining.*;
import java.io.File;
import java.nio.file.*;

public class BruteMinerTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== BruteMiner Tests ===\n");
        testHarvestBlocks();
        testEmptyDir();
        testMultipleBlocks();
        System.out.println("\n=== RESULTS: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }

    static void check(String n, boolean c) {
        if (c) { passed++; System.out.println("  ✅ " + n); }
        else { failed++; System.out.println("  ❌ " + n + " FAILED"); }
    }

    static void testHarvestBlocks() throws Exception {
        System.out.println("testHarvestBlocks:");
        Path d = Files.createTempDirectory("brute-test");
        Path blocksDir = d.resolve("blocks");
        Files.createDirectories(blocksDir);
        Files.writeString(blocksDir.resolve("test_block.ast"),
            "class TestBlock {\n    void run() {}\n}");
        
        SuggestionRegistry reg = new SuggestionRegistry(d.resolve("sug").toString());
        BruteMiner miner = new BruteMiner(d.toString(), reg);
        
        int count = miner.harvestBlocks();
        check("harvested 1 block", count == 1);
        check("registry has 1 suggestion", reg.getAllSuggestions().size() == 1);
        
        deleteRecursive(d.toFile());
    }

    static void testEmptyDir() throws Exception {
        System.out.println("\ntestEmptyDir:");
        Path d = Files.createTempDirectory("brute-empty");
        SuggestionRegistry reg = new SuggestionRegistry(d.resolve("sug").toString());
        BruteMiner miner = new BruteMiner(d.toString(), reg);
        check("empty dir = 0 blocks", miner.harvestBlocks() == 0);
        deleteRecursive(d.toFile());
    }

    static void testMultipleBlocks() throws Exception {
        System.out.println("\ntestMultipleBlocks:");
        Path d = Files.createTempDirectory("brute-multi");
        Path blocksDir = d.resolve("blocks");
        Files.createDirectories(blocksDir);
        Files.writeString(blocksDir.resolve("a.ast"), "class A {}");
        Files.writeString(blocksDir.resolve("b.ast"), "class B {}");
        Files.writeString(blocksDir.resolve("c.ast"), "class C {}");
        
        SuggestionRegistry reg = new SuggestionRegistry(d.resolve("sug").toString());
        BruteMiner miner = new BruteMiner(d.toString(), reg);
        check("harvested 3 blocks", miner.harvestBlocks() == 3);
        deleteRecursive(d.toFile());
    }

    static void deleteRecursive(File f) {
        if (f.isDirectory()) { File[] kids = f.listFiles(); if (kids != null) for (File c : kids) deleteRecursive(c); }
        f.delete();
    }
}
