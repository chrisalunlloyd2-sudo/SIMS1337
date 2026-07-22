package com.aigen.sims.routing;

import com.aigen.sims.agents.SLMAgent;
import com.aigen.sims.tasks.Task;
import com.aigen.sims.tasks.Complexity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model Router - Selects appropriate model for each task
 * Routes based on complexity, latency requirements, and model availability
 */
public class ModelRouter {
    private static final Logger logger = LoggerFactory.getLogger(ModelRouter.class);
    
    private final ModelPool modelPool;
    
    // Complexity thresholds
    private static final double SIMPLE_THRESHOLD = 0.3;
    private static final double MEDIUM_THRESHOLD = 0.6;
    private static final double COMPLEX_THRESHOLD = 0.8;
    
    public ModelRouter(ModelPool modelPool) {
        this.modelPool = modelPool;
        logger.info("ModelRouter initialized with {} models", modelPool.getModelCount());
    }
    
    /**
     * Select best model for task based on complexity
     */
    public SLMAgent selectModel(Task task) {
        Complexity complexity = task.getComplexity();
        SLMAgent selected;
        
        switch (complexity) {
            case VERY_LOW:
            case LOW:
                selected = modelPool.getQwen2_0_5b();
                logger.debug("Routed LOW complexity task '{}' to qwen2.5:0.5b", task.getName());
                break;
            
            case MEDIUM:
                selected = modelPool.getTinyllama_1_1b();
                logger.debug("Routed MEDIUM complexity task '{}' to tinyllama:1.1b", task.getName());
                break;
            
            case HIGH:
                selected = modelPool.getPhiLatest();
                logger.debug("Routed HIGH complexity task '{}' to phi:latest", task.getName());
                break;
            
            case VERY_HIGH:
            case CRITICAL:
                selected = modelPool.getPhi3Mini();
                logger.debug("Routed CRITICAL task '{}' to phi3:mini", task.getName());
                break;
            
            default:
                selected = modelPool.getTinyllama_1_1b();
                logger.debug("Routed default task '{}' to tinyllama:1.1b", task.getName());
        }
        
        return selected;
    }
    
    /**
     * Select model with latency constraint
     */
    public SLMAgent selectModelWithLatency(Task task, long maxLatencyMs) {
        if (maxLatencyMs < 200) {
            // Must use fastest model regardless of complexity
            logger.debug("Latency-constrained (<200ms) routing for '{}' to qwen2.5:0.5b", task.getName());
            return modelPool.getQwen2_0_5b();
        } else if (maxLatencyMs < 1000) {
            // Can use balanced model
            logger.debug("Latency-constrained (<1000ms) routing for '{}' to tinyllama:1.1b", task.getName());
            return modelPool.getTinyllama_1_1b();
        } else {
            // Use complexity-based selection
            return selectModel(task);
        }
    }
    
    /**
     * Get routing recommendation with explanation
     */
    public RoutingRecommendation getRecommendation(Task task) {
        SLMAgent selectedModel = selectModel(task);
        long estimatedLatency = estimateLatency(selectedModel, task);
        String reason = getReason(task);
        
        return new RoutingRecommendation(selectedModel, estimatedLatency, reason);
    }
    
    /**
     * Estimate latency for task on given model
     */
    private long estimateLatency(SLMAgent model, Task task) {
        // Base latency by model (milliseconds)
        long baseLatency;
        switch (model.getModelName()) {
            case "qwen2.5:0.5b":
                baseLatency = 50;
                break;
            case "tinyllama:1.1b":
                baseLatency = 500;
                break;
            case "phi:latest":
                baseLatency = 2000;
                break;
            case "phi3:mini":
                baseLatency = 5000;
                break;
            default:
                baseLatency = 1000;
        }
        
        // Adjust by task complexity
        double complexityMultiplier = task.getComplexity().getMultiplier();
        return (long) (baseLatency * complexityMultiplier);
    }
    
    /**
     * Get human-readable reason for routing decision
     */
    private String getReason(Task task) {
        switch (task.getComplexity()) {
            case VERY_LOW:
            case LOW:
                return "Simple task - using fastest model (qwen2.5:0.5b)";
            case MEDIUM:
                return "Medium complexity - using balanced model (tinyllama:1.1b)";
            case HIGH:
                return "Complex task - using reasoning model (phi:latest)";
            case VERY_HIGH:
            case CRITICAL:
                return "Critical task - using deep reasoning model (phi3:mini)";
            default:
                return "Default routing - using balanced model";
        }
    }
}

/**
 * Routing recommendation with explanation
 */
class RoutingRecommendation {
    private final SLMAgent model;
    private final long estimatedLatencyMs;
    private final String reason;
    
    public RoutingRecommendation(SLMAgent model, long estimatedLatencyMs, String reason) {
        this.model = model;
        this.estimatedLatencyMs = estimatedLatencyMs;
        this.reason = reason;
    }
    
    public SLMAgent getModel() {
        return model;
    }
    
    public long getEstimatedLatencyMs() {
        return estimatedLatencyMs;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return String.format("RoutingRecommendation{model=%s, latency=%dms, reason='%s'}", 
            model.getModelName(), estimatedLatencyMs, reason);
    }
}
