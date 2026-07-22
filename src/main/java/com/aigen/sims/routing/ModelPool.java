package com.aigen.sims.routing;

import com.aigen.sims.agents.SLMAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Model Pool - Manages all 4 SLM models
 * Keeps models warm and ready for instant access
 */
public class ModelPool {
    private static final Logger logger = LoggerFactory.getLogger(ModelPool.class);
    
    private final Map<String, SLMAgent> models;
    
    public ModelPool() {
        models = new HashMap<>();
        initializeModels();
    }
    
    /**
     * Initialize all available models (flexible strategy)
     * Uses whatever small models are available + LoRA adapters for specialization
     */
    private void initializeModels() {
        logger.info("Initializing model pool (flexible strategy)...");
        
        // Fast tier - qwen2.5:0.5b (398MB, <100ms) - PERFECT for LoRA
        models.put("fast_primary", new SLMAgent("qwen2.5:0.5b"));
        logger.info("  ✓ Fast tier: qwen2.5:0.5b (398MB)");
        
        // Balanced tier - tinyllama:1.1b (638MB, ~500ms) - great middle ground
        models.put("balanced_primary", new SLMAgent("tinyllama:1.1b"));
        logger.info("  ✓ Balanced tier: tinyllama:1.1b (638MB)");
        
        // Reasoning tier - phi:latest (1.6GB, ~2-5s) - solid reasoning
        models.put("reasoning_primary", new SLMAgent("phi:latest"));
        logger.info("  ✓ Reasoning tier: phi:latest (1.6GB)");
        
        // Deep tier - phi3:mini (2.2GB, ~5-10s) - deep decisions
        models.put("deep_primary", new SLMAgent("phi3:mini"));
        logger.info("  ✓ Deep tier: phi3:mini (2.2GB)");
        
        // Warm up all models
        warmUpAll();
        
        logger.info("Model pool initialized: 4 tiers, total ~4.8GB with mmap");
    }
    
    /**
     * Try models in priority order, use first available
     */
    private SLMAgent tryModel(String... modelNames) {
        for (String name : modelNames) {
            try {
                SLMAgent agent = new SLMAgent(name);
                // Quick availability check
                agent.warmUp();
                return agent;
            } catch (Exception e) {
                logger.debug("Model {} not available, trying next", name);
            }
        }
        // Fallback to smallest known model
        logger.warn("No preferred models available, using qwen2.5:0.5b as fallback");
        return new SLMAgent("qwen2.5:0.5b");
    }
    
    /**
     * Warm up all models (public API for GUI startup)
     */
    public void warmUpAll() {
        logger.info("Warming up all models...");
        for (SLMAgent model : models.values()) {
            model.warmUp();
        }
        logger.info("All models warmed up successfully");
    }
    
    /**
     * Get fast tier model (qwen2.5:0.5b)
     */
    public SLMAgent getQwen2_0_5b() {
        return models.get("qwen2_0_5b");
    }
    
    /**
     * Get balanced tier model (tinyllama:1.1b)
     */
    public SLMAgent getTinyllama_1_1b() {
        return models.get("tinyllama_1_1b");
    }
    
    /**
     * Get reasoning tier model (phi:latest)
     */
    public SLMAgent getPhiLatest() {
        return models.get("phi_latest");
    }
    
    /**
     * Get deep reasoning tier model (phi3:mini)
     */
    public SLMAgent getPhi3Mini() {
        return models.get("phi3_mini");
    }
    
    /**
     * Get model by name
     */
    public SLMAgent getModel(String name) {
        return models.get(name);
    }
    
    /**
     * Get all models (read-only copy)
     */
    public Map<String, SLMAgent> getAllModels() {
        return new HashMap<>(models);
    }
    
    /**
     * Check if all models are warm
     */
    public boolean allModelsWarm() {
        return models.values().stream().allMatch(SLMAgent::isWarm);
    }
    
    /**
     * Get model count
     */
    public int getModelCount() {
        return models.size();
    }
}
