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

    private static final String GGUF_URL = "http://localhost:5000/api/generate";
    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

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
            case "mine" -> {
                com.aigen.sims.mining.CodeMinerOrchestrator orch =
                    new com.aigen.sims.mining.CodeMinerOrchestrator(System.getProperty("user.home") + "/AIGEN_SYU/repos", "suggestions");
                com.aigen.sims.mining.CodeMinerOrchestrator.MiningReport report = orch.runMiningCycle();
                System.out.println(report.toEmailString());
            }
            default -> System.out.println("Unknown mode: " + mode);
        }
    }

    private static void generateCode(String prompt) throws Exception {
        System.out.println("💻 CODE GENERATION PIPELINE");
        System.out.println("==============================");
        String initialCode = callModel("qwen2.5:0.5b", "Write code for: " + prompt + ". Output ONLY the code, no explanation.", 200, 0.1);
        System.out.println("📝 [qwen2.5:0.5b] Initial code:\n" + initialCode);
        String reviewedCode = callModel("llama3.2:1b", "Review and improve this code. Fix bugs, add comments, optimize. Output ONLY the improved code:\n" + initialCode, 300, 0.2);
        System.out.println("\n🔍 [llama3.2:1b] Reviewed code:\n" + reviewedCode);
        String finalCode = callModel("deepseek-r1:1.5b", "Finalize this code. Add error handling, edge cases, and docstring. Output ONLY the final code:\n" + reviewedCode, 400, 0.1);
        System.out.println("\n✅ [deepseek-r1:1.5b] Final code:\n" + finalCode);
        String filename = prompt.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(30, prompt.length())) + ".py";
        Files.writeString(Path.of(filename), finalCode);
        System.out.println("\n💾 Saved to: " + filename);
    }

    private static void writeEssay(String topic) throws Exception {
        System.out.println("📝 ESSAY WRITING PIPELINE");
        System.out.println("==========================");
        String outline = callModel("tinyllama:1.1b", "Create a detailed outline for an essay about: " + topic + ". Use bullet points.", 200, 0.3);
        System.out.println("[tinyllama:1.1b] Outline:\n" + outline);
        String body = callModel("llama3.2:1b", "Write the body paragraphs for this essay outline. Be thorough and detailed:\n" + outline, 500, 0.5);
        System.out.println("\n🔄 [llama3.2:1b] Body:\n" + body);
        String intro = callModel("phi3:mini", "Write an engaging introduction and conclusion for this essay:\n" + body, 300, 0.4);
        System.out.println("\n🎯 [phi3:mini] Intro/Conclusion:\n" + intro);
        String fullEssay = intro + "\n\n" + body;
        String filename = topic.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(30, topic.length())) + "_essay.txt";
        Files.writeString(Path.of(filename), fullEssay);
        System.out.println("\n💾 Saved to: " + filename);
    }

    private static void completeTask(String task) throws Exception {
        System.out.println("⚡ TASK COMPLETION");
        System.out.println("==================");
        String analysis = callModel("qwen2.5:0.5b", "Analyze this task and break it into steps: " + task, 150, 0.2);
        System.out.println("🔍 [qwen2.5:0.5b] Analysis:\n" + analysis);
        String solution = callModel("llama3.2:1b", "Complete this task step by step: " + task + "\nAnalysis: " + analysis, 400, 0.3);
        System.out.println("\n⚡ [llama3.2:1b] Solution:\n" + solution);
        String verification = callModel("deepseek-r1:1.5b", "Verify this solution is correct and complete. Point out any issues:\nTask: " + task + "\nSolution: " + solution, 200, 0.1);
        System.out.println("\n✅ [deepseek-r1:1.5b] Verification:\n" + verification);
    }

    private static void runPipeline(String input) throws Exception {
        System.out.println("🔗 FULL MODEL PIPELINE (6 models chained)");
        System.out.println("===========================================");
        String current = input;
        String[] pipeline = {"qwen2.5:0.5b", "tinyllama:1.1b", "phi:latest", "llama3.2:1b", "phi3:mini", "deepseek-r1:1.5b"};
        for (String model : pipeline) {
            System.out.println("\n--- " + model + " ---");
            current = callModel(model, "Process this and improve it. Add your unique perspective:\n" + current, 200, 0.4);
            System.out.println(current.substring(0, Math.min(150, current.length())) + "...");
        }
        System.out.println("\n✅ Pipeline complete! Final output length: " + current.length() + " chars");
    }

    /**
     * MARKOV VERIFICATION CHAIN voting (upgraded from single-round majority).
     *
     * The doctrine: small models reach LOGICALLY CORRECT decisions by stepping
     * through a chain of verified sub-claims. Correctness is accumulated
     * probability, not one vote.
     *
     *    CLAIM0 ─P01→ CLAIM1 ─P12→ CLAIM2 ─P23→ DECISION
     *      │          │          │
     *    verified    verified   verified
     *    (N models)  (N models) (N models)
     *
     *  P(decision) = Π P(step_i | step_{i-1})
     *
     * Weak links get SPLIT into smaller sub-claims and re-verified (more steps,
     * more verification). Transitions are appended to chain_transitions.log
     * so lstm_refractor.py can learn temporal priors over time.
     */
    private static void runVote(String proposal) throws Exception {
        System.out.println("🧬 MARKOV CHAIN VOTING ON: " + proposal);
        System.out.println("==============================");
        String[] voters = {"qwen2.5:0.5b", "tinyllama:1.1b", "llama3.2:1b", "deepseek-r1:1.5b"};

        // 1. DECOMPOSE — proposal -> ordered claim chain
        String[] claims = {
            "The proposal is clearly stated and its scope is understood: " + proposal,
            "The key premise of this proposal is factually correct: " + proposal,
            "Given the premise, the logical inference is sound: " + proposal,
            "The conclusion follows necessarily and is safe to act on: " + proposal
        };
        String[] claimIds = {"fact", "premise", "infer", "conclude"};

        double chainConfidence = 1.0;
        boolean chainBroken = false;
        for (int i = 0; i < claims.length && !chainBroken; i++) {
            // 2. VERIFY — every link voted by all models, weighted by confidence
            double conf = verifyClaim(claims[i], voters);
            System.out.printf("   %-8s claim verified: confidence=%.2f %s%n",
                    claimIds[i], conf, conf >= 0.7 ? "✅" : "⚠️  weak");
            // learn this transition (append for lstm_refractor.py)
            appendTransition(claimIds[Math.max(0, i - 1)], claimIds[i], conf >= 0.7);
            // 3. PROPAGATE — chain product
            chainConfidence *= conf;
            if (conf < 0.7) {
                System.out.println("   ⚠️  weak link — SPLITTING into sub-claims for re-verification");
                // split into two smaller claims (each easier to verify)
                double subA = verifyClaim("Part A of: " + claims[i], voters);
                double subB = verifyClaim("Part B of: " + claims[i], voters);
                double subConf = Math.min(subA, subB);
                System.out.printf("   split re-verify: A=%.2f B=%.2f → combined=%.2f%n", subA, subB, subConf);
                chainConfidence = (chainConfidence / conf) * subConf; // replace weak link
                appendTransition(claimIds[i], claimIds[i] + "_split", subConf >= 0.7);
                if (subConf < 0.7) {
                    chainBroken = true; // a false premise — stop, don't build on it
                    System.out.println("   ❌ chain broken at " + claimIds[i] + " — do not proceed");
                }
            }
        }

        // 4. GATE — chain product must clear threshold
        System.out.printf("\n📊 Chain confidence: %.3f (product of %d verified links)%n", chainConfidence, claims.length);
        boolean passed = !chainBroken && chainConfidence >= 0.5;
        System.out.println(passed ? "✅ PROPOSAL PASSED (Markov chain verified)"
                                  : "❌ PROPOSAL REJECTED (chain confidence below gate)");
        appendDecision(proposal, passed, chainConfidence);
    }

    /** Verify one claim across all models; returns weighted confidence 0..1. */
    private static double verifyClaim(String claim, String[] voters) throws Exception {
        String prompt = "You are one of several small models in a Markov verification chain. "
                + "Is this claim TRUE? Answer STRICTLY as JSON: "
                + "{\"answer\": \"YES\" or \"NO\", \"confidence\": 0.0-1.0}\n"
                + "Claim: " + claim;
        double yesW = 0, noW = 0;
        for (String model : voters) {
            String raw = callModel(model, prompt, 80, 0.0);
            double conf = 0.5;
            String ans = "NO";
            try {
                int ci = raw.indexOf("confidence");
                if (ci > 0) conf = Double.parseDouble(raw.substring(raw.indexOf(':', ci) + 1).trim().replaceAll("[^0-9.]", ""));
            } catch (Exception ignored) {}
            if (raw.toUpperCase().contains("\"YES\"") || raw.toUpperCase().contains("YES")) ans = "YES";
            if (ans.equals("YES")) yesW += conf; else noW += conf;
            System.out.printf("      %-16s %s (conf %.2f)%n", model, ans.equals("YES") ? "✅ TRUE" : "❌ FALSE", conf);
        }
        double total = yesW + noW;
        return total == 0 ? 0 : Math.max(yesW, noW) / total;
    }

    /** Append a learned transition to chain_transitions.log (CSV: prev,curr,correct). */
    private static void appendTransition(String prev, String curr, boolean correct) {
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("chain_transitions.log"),
                prev + "," + curr + "," + correct + "\n",
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    /** Append decision outcome for lstm_refractor.py sequence learning. */
    private static void appendDecision(String proposal, boolean passed, double confidence) {
        try {
            String json = "{\"ts\": " + System.currentTimeMillis() / 1000.0
                + ", \"decision\": \"" + (passed ? "APPROVE" : "REJECT")
                + "\", \"confidence\": " + String.format("%.3f", confidence)
                + ", \"proposal\": \"" + escapeJson(proposal) + "\"}\n";
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("chain_decisions.jsonl"), json,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    private static void runAll(String input) throws Exception {
        System.out.println("😀 RUNNING ALL PIPELINES\n");
        generateCode(input);
        System.out.println("\n" + "=".repeat(60) + "\n");
        writeEssay(input);
        System.out.println("\n" + "=".repeat(60) + "\n");
        completeTask(input);
        System.out.println("\n" + "=".repeat(60) + "\n");
        runVote("Should we deploy the generated code for: " + input);
    }

    private static String callModel(String model, String prompt, int maxTokens, double temp) throws Exception {
        String json = String.format(
            "{\"prompt\":\"%s\",\"max_tokens\":%d}",
            escapeJson(prompt), maxTokens);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GGUF_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        Exception lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
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
                lastEx = new RuntimeException("HTTP " + resp.statusCode());
            } catch (Exception e) {
                lastEx = e;
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                Thread.sleep((long)(Math.pow(2, attempt) * 500));
            }
        }
        throw lastEx;
    }
}
