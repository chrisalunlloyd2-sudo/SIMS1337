package com.aigen.sims.agents;

import com.aigen.sims.ModelBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SLM Agent - Wrapper for Ollama model inference
 * Supports 4 model tiers with mmap optimization
 *
 * PERF EPIC t_4a3756ed (C.2 cellular-mic): generate() no longer opens its own
 * HTTP connection to Ollama (:11434) — that was a cellular-mic violation
 * (ModelPool warm-up could collide with routed traffic). All inference now
 * funnels through ModelBus: single-flight queue, serialized load/infer,
 * delegated to the orchestrator (:11435) when it is up.
 */
public class SLMAgent {
    private static final Logger logger = LoggerFactory.getLogger(SLMAgent.class);

    private final String modelName;
    private boolean isWarm;
    private final Map<String, Object> context;

    public SLMAgent(String modelName) {
        this.modelName = modelName;
        this.isWarm = false;
        this.context = new HashMap<>();
    }

    /**
     * Warm up the model (load into RAM) — serialized through the single flight.
     */
    public void warmUp() {
        logger.info("Warming up model: {}", modelName);
        try {
            String reply = generate("Ready", 10);
            isWarm = !"NO_RESPONSE".equals(reply);
            if (isWarm) {
                logger.info("Model {} warmed up successfully", modelName);
            } else {
                logger.warn("Model {} warm-up got NO_RESPONSE (bus busy or offline)", modelName);
            }
        } catch (Exception e) {
            logger.error("Failed to warm up model {}: {}", modelName, e.getMessage());
        }
    }

    /**
     * Generate text from prompt. Serialized through ModelBus (cellular mic).
     */
    public String generate(String prompt, int maxTokens) throws IOException, InterruptedException {
        return ModelBus.ask(modelName, prompt, maxTokens);
    }

    /**
     * Set context for LoRA adapter switching
     */
    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    /**
     * Get context value
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        return (T) context.get(key);
    }

    public String getModelName() {
        return modelName;
    }

    public boolean isWarm() {
        return isWarm;
    }

    public void clearContext() {
        context.clear();
    }

    @Override
    public String toString() {
        return String.format("SLMAgent{model='%s', warm=%s}", modelName, isWarm);
    }
}