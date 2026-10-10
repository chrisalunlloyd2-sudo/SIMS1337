package com.aigen.sims;

/**
 * SIMS1337 - OllamaRouter
 * Cellular Microphone Gating (CMG) Implementation
 * Ensures only the active model in use remains in RAM/VRAM.
 *
 * PERF EPIC t_4a3756ed (H7/C.2/C.4): query() no longer talks to Ollama and no
 * longer mutates its own "mic" state — every inference funnels through
 * ModelBus (single-flight queue -> orchestrator :11435, or built-in direct
 * single flight when the orchestrator service is down). This class stays as
 * the thin facade all existing call sites already use; unloading is handled
 * by the orchestrator's keep_alive policy instead of explicit purge calls.
 */
public class OllamaRouter {

    public String query(String model, String prompt) {
        System.out.println("[OLLAMA] Routing to " + model + " via ModelBus | Prompt: " + prompt);
        return ModelBus.ask(model, prompt);
    }

    /** Facade re-added: one-arg callers pass default num_predict via ModelBus. */
    public String query(String model, String prompt, int maxTokens) {
        System.out.println("[OLLAMA] Routing to " + model + " via ModelBus (maxTokens=" + maxTokens + ")");
        return ModelBus.ask(model, prompt, maxTokens);
    }

    /** Informational: model the mic currently points at (best effort). */
    public static String getActiveModel() {
        return ModelBus.getActiveModel();
    }
}