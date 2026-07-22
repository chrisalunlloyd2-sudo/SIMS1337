package com.aigen.sims.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * SLM Agent - Wrapper for Ollama model inference
 * Supports 4 model tiers with mmap optimization
 */
public class SLMAgent {
    private static final Logger logger = LoggerFactory.getLogger(SLMAgent.class);
    
    private static final String OLLAMA_BASE = "http://localhost:11434";
    private static final Duration TIMEOUT = Duration.ofSeconds(120);
    
    private final String modelName;
    private final HttpClient httpClient;
    private boolean isWarm;
    private final Map<String, Object> context;
    
    public SLMAgent(String modelName) {
        this.modelName = modelName;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
        this.isWarm = false;
        this.context = new HashMap<>();
    }
    
    /**
     * Warm up the model (load into RAM)
     */
    public void warmUp() {
        logger.info("Warming up model: {}", modelName);
        try {
            String prompt = "Ready";
            generate(prompt, 10);
            isWarm = true;
            logger.info("Model {} warmed up successfully", modelName);
        } catch (Exception e) {
            logger.error("Failed to warm up model {}: {}", modelName, e.getMessage());
        }
    }
    
    /**
     * Generate text from prompt
     */
    public String generate(String prompt, int maxTokens) throws IOException, InterruptedException {
        String jsonBody = String.format(
            "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false,\"options\":{\"num_predict\":%d}}",
            modelName,
            prompt.replace("\"", "\\\""),
            maxTokens
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_BASE + "/api/generate"))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Ollama API error: " + response.statusCode());
        }
        
        // Simple JSON parsing (in production, use Jackson)
        String body = response.body();
        int start = body.indexOf("\"response\":\"") + 12;
        int end = body.indexOf("\"", start);
        return body.substring(start, end).replace("\\n", "\n");
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
