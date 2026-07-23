package com.aigen.sims;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.file.*;

/**
 * HeadlessOllamaPipeline - No GUI, pure automation.
 * Chain models together for code generation, essay writing, task completion.
 * 
 * Usage: java HeadlessOllamaPipeline <mode> <input>
 * Modes: code, essay, task, pipeline, vote
 */
public class HeadlessOllamaPipeline {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    // Model tiers for different tasks
    private static final String[] FAST_MODELS = {"qwen2.5:0.5b", "tinyllama:1.1b"};
    private static final String[] REASONING_MODELS = {"phi:latest", "llama3.2:1b"};
    private static final String[] DEEP_MODELS = {"phi3:mini", "deepseek-r1:1.5b"};

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("HeadlessOllamaPipeline v1.0");
            System.out.println("Usage: java HeadlessOllamaPipeline <mode> <input>");
            System.out.println("Modes: code | essay | task | pipeline | vote | all");
            System.out.println("Example: java HeadlessOllamaPipeline code \"Write a sorting algorithm\"");
            return;
        }

        String mode = args[0];
        String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        switch (mode) {
            case "code" -> generateCode(input);
            case "essay" -> writeEssay(input);
            case "task" -> completeTask(input);
            case "pipeline" -> runPipeline(input);
            case "vote" -> runVote(input);
            case "all" -> runAll(input);
            default -> System.out.println("Unknown mode: " + mode);
        }
    }

    // ==================== CODE GENERATION ====================
    private static void generateCode(String prompt) throws Exception {
        System.out.println("💻 CODE GENERATION PIPELINE");
        System.out.println("==============================");

        // Step 1: Fast model generates initial code
        String initialCode = callModel("qwen2.5:0.5b",
            "Write code for: " + prompt + ". Output ONLY the code, no explanation.",
            200, 0.1);
        System.out.println("📝 [qwen2.5:0.5b] Initial code:\n" + initialCode);

        // Step 2: Reasoning model reviews and improves
        String reviewedCode = callModel("llama3.2:1b",
            "Review and improve this code. Fix bugs, add comments, optimize. Output ONLY the improved code:\n" + initialCode,
            300, 0.2);
        System.out.println("\n🔍 [llama3.2:1b] Reviewed code:\n" + reviewedCode);

        // Step 3: Deep model finalizes
        String finalCode = callModel("deepseek-r1:1.5b",
            "Finalize this code. Add error handling, edge cases, and docstring. Output ONLY the final code:\n" + reviewedCode,
            400, 0.1);
        System.out.println("\n✅ [deepseek-r1:1.5b] Final code:\n" + finalCode);

        // Save to file
        String filename = prompt.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(30, prompt.length())) + ".py";
        Files.writeString(Path.of(filename), finalCode);
        System.out.println("\n💾 Saved to: " + filename);
    }

    // ==================== ESSAY WRITING ====================
    private static void writeEssay(String topic) throws Exception {
        System.out.println("📝 ESSAY WRITING PIPELINE");
        System.out.println("==========================");

        // Step 1: Outline
        String outline = callModel("tinyllama:1.1b",
            "Create a detailed outline for an essay about: " + topic + ". Use bullet points.",
            200, 0.3);
        System.out.println("📋 [tinyllama:1.1b] Outline:\n" + outline);

        // Step 2: Body paragraphs
        String body = callModel("llama3.2:1b",
            "Write the body paragraphs for this essay outline. Be thorough and detailed:\n" + outline,
            500, 0.5);
        System.out.println("\n📄 [llama3.2:1b] Body:\n" + body);

        // Step 3: Introduction + Conclusion
        String intro = callModel("phi3:mini",
            "Write an engaging introduction and conclusion for this essay:\n" + body,
            300, 0.4);
        System.out.println("\n🎯 [phi3:mini] Intro/Conclusion:\n" + intro);

        // Combine
        String fullEssay = intro + "\n\n" + body;
        String filename = topic.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(30, topic.length())) + "_essay.txt";
        Files.writeString(Path.of(filename), fullEssay);
        System.out.println("\n💾 Saved to: " + filename);
    }

    // ==================== TASK COMPLETION ====================
    private static void completeTask(String task) throws Exception {
        System.out.println("⚡ TASK COMPLETION");
        System.out.println("==================");

        // Step 1: Analyze task
        String analysis = callModel("qwen2.5:0.5b",
            "Analyze this task and break it into steps: " + task,
            150, 0.2);
        System.out.println("🔍 [qwen2.5:0.5b] Analysis:\n" + analysis);

        // Step 2: Generate solution
        String solution = callModel("llama3.2:1b",
            "Complete this task step by step: " + task + "\nAnalysis: " + analysis,
            400, 0.3);
        System.out.println("\n⚡ [llama3.2:1b] Solution:\n" + solution);

        // Step 3: Verify
        String verification = callModel("deepseek-r1:1.5b",
            "Verify this solution is correct and complete. Point out any issues:\nTask: " + task + "\nSolution: " + solution,
            200, 0.1);
        System.out.println("\n✅ [deepseek-r1:1.5b] Verification:\n" + verification);
    }

    // ==================== FULL PIPELINE ====================
    private static void runPipeline(String input) throws Exception {
        System.out.println("🔗 FULL MODEL PIPELINE (6 models chained)");
        System.out.println("=========================================");

        String current = input;
        String[] pipeline = {"qwen2.5:0.5b", "tinyllama:1.1b", "phi:latest",
                             "llama3.2:1b", "phi3:mini", "deepseek-r1:1.5b"};

        for (String model : pipeline) {
            System.out.println("\n--- " + model + " ---");
            current = callModel(model,
                "Process this and improve it. Add your unique perspective:\n" + current,
                200, 0.4);
            System.out.println(current.substring(0, Math.min(150, current.length())) + "...");
        }

        System.out.println("\n✅ Pipeline complete! Final output length: " + current.length() + " chars");
    }

    // ==================== VOTING ====================
    private static void runVote(String proposal) throws Exception {
        System.out.println("🗳️ MODEL VOTING ON: " + proposal);
        System.out.println("==============================");

        int approve = 0, reject = 0;
        String[] voters = {"qwen2.5:0.5b", "tinyllama:1.1b", "llama3.2:1b", "deepseek-r1:1.5b"};

        for (String model : voters) {
            String vote = callModel(model,
                "Vote APPROVE or REJECT on this proposal. Reply with ONLY one word: APPROVE or REJECT.\nProposal: " + proposal,
                5, 0.0);
            boolean isApprove = vote.toUpperCase().contains("APPROVE");
            if (isApprove) approve++; else reject++;
            System.out.println("   " + model + ": " + (isApprove ? "✅ APPROVE" : "❌ REJECT"));
        }

        System.out.println("\n📊 Result: " + approve + "/" + voters.length + " APPROVE");
        System.out.println(approve >= voters.length / 2 ? "✅ PROPOSAL PASSED" : "❌ PROPOSAL REJECTED");
    }

    // ==================== RUN ALL ====================
    private static void runAll(String input) throws Exception {
        System.out.println("🚀 RUNNING ALL PIPELINES\n");
        generateCode(input);
        System.out.println("\n" + "=".repeat(60) + "\n");
        writeEssay(input);
        System.out.println("\n" + "=".repeat(60) + "\n");
        completeTask(input);
        System.out.println("\n" + "=".repeat(60) + "\n");
        runVote("Should we deploy the generated code for: " + input);
    }

    // ==================== OLLAMA API CALL ====================
    private static String callModel(String model, String prompt, int maxTokens, double temp) throws Exception {
        String json = String.format(
            "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false,\"options\":{\"num_predict\":%d,\"temperature\":%.1f}}",
            model, prompt.replace("\"", "\\\"").replace("\n", "\\n"), maxTokens, temp);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 200) {
            String body = resp.body();
            int s = body.indexOf("\"response\":\"");
            if (s > 0) {
                s += 12;
                int e = body.indexOf("\"", s);
                if (e > s) return body.substring(s, e)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\t", "\t");
            }
            return body;
        }
        throw new RuntimeException("HTTP " + resp.statusCode());
    }
}
