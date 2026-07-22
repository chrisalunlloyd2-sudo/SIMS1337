package com.aigen.sims.routing;

import com.aigen.sims.agents.SLMAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * LoRA Adapter Switcher - Circular token tree implementation
 * Allows instant switching between LoRA adapters without reloading base model
 * Models stay warm, only weights change (<100ms switch time)
 */
public class LoRASwitcher {
    private static final Logger logger = LoggerFactory.getLogger(LoRASwitcher.class);
    
    private final Map<AdapterType, LoRAAdapter> adapters;
    private final Queue<AdapterType> circularBuffer;
    private LoRAAdapter currentAdapter;
    private long switchCount;
    private long totalSwitchTimeMs;
    
    public LoRASwitcher() {
        this.adapters = new HashMap<>();
        this.circularBuffer = new ArrayDeque<>();
        this.switchCount = 0;
        this.totalSwitchTimeMs = 0;
        initializeAdapters();
    }
    
    /**
     * Initialize all LoRA adapters
     */
    private void initializeAdapters() {
        logger.info("Initializing LoRA adapters...");
        
        // Create adapters for each type
        for (AdapterType type : AdapterType.values()) {
            LoRAAdapter adapter = new LoRAAdapter(type);
            adapters.put(type, adapter);
            circularBuffer.offer(type);
            logger.info("  ✓ LoRA adapter initialized: {}", type);
        }
        
        // Start with first adapter
        currentAdapter = adapters.get(AdapterType.CHAT);
        logger.info("LoRA switcher initialized with {} adapters, starting with: {}", 
            adapters.size(), currentAdapter.getType());
    }
    
    /**
     * Switch to specified adapter
     * @param type Adapter type to switch to
     * @return SwitchResult with timing information
     */
    public SwitchResult switchAdapter(AdapterType type) {
        long startTime = System.nanoTime();
        
        if (!adapters.containsKey(type)) {
            logger.error("Unknown adapter type: {}", type);
            return new SwitchResult(false, -1, "Unknown adapter type");
        }
        
        LoRAAdapter newAdapter = adapters.get(type);
        
        // Skip if already on this adapter
        if (currentAdapter == newAdapter) {
            logger.debug("Already on adapter: {}", type);
            return new SwitchResult(true, 0, "Already active");
        }
        
        // Unload current adapter weights
        currentAdapter.unload();
        
        // Load new adapter weights
        newAdapter.load();
        
        // Update current
        LoRAAdapter oldAdapter = currentAdapter;
        currentAdapter = newAdapter;
        switchCount++;
        
        long endTime = System.nanoTime();
        long switchTimeMs = (endTime - startTime) / 1_000_000;
        totalSwitchTimeMs += switchTimeMs;
        
        logger.info("Switched LoRA adapter: {} → {} ({}ms)", 
            oldAdapter.getType(), type, switchTimeMs);
        
        return new SwitchResult(true, switchTimeMs, "Success");
    }
    
    /**
     * Switch to next adapter in circular buffer
     * @return SwitchResult with timing information
     */
    public SwitchResult switchToNext() {
        // Get next adapter in circular order
        circularBuffer.offer(circularBuffer.poll());
        AdapterType nextType = circularBuffer.peek();
        return switchAdapter(nextType);
    }
    
    /**
     * Get current adapter
     */
    public LoRAAdapter getCurrentAdapter() {
        return currentAdapter;
    }
    
    /**
     * Get current adapter type
     */
    public AdapterType getCurrentAdapterType() {
        return currentAdapter.getType();
    }
    
    /**
     * Set context for current adapter
     */
    public void setContext(String key, Object value) {
        currentAdapter.setContext(key, value);
    }
    
    /**
     * Get context from current adapter
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        return (T) currentAdapter.getContext(key);
    }
    
    /**
     * Get switch statistics
     */
    public SwitchStats getStats() {
        double avgSwitchTime = switchCount > 0 ? 
            (double) totalSwitchTimeMs / switchCount : 0;
        
        return new SwitchStats(
            switchCount,
            totalSwitchTimeMs,
            avgSwitchTime,
            currentAdapter.getType()
        );
    }
    
    /**
     * Get all available adapter types
     */
    public AdapterType[] getAvailableAdapters() {
        return AdapterType.values();
    }
}

/**
 * LoRA Adapter - Represents a single LoRA weight set
 */
class LoRAAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LoRAAdapter.class);
    
    private final AdapterType type;
    private boolean isLoaded;
    private final Map<String, Object> context;
    
    public LoRAAdapter(AdapterType type) {
        this.type = type;
        this.isLoaded = false;
        this.context = new HashMap<>();
    }
    
    /**
     * Load adapter weights (should be <100ms with mmap)
     */
    public void load() {
        logger.debug("Loading LoRA adapter weights: {}", type);
        // In production: mmap weights from disk
        // For now: simulate load
        isLoaded = true;
    }
    
    /**
     * Unload adapter weights
     */
    public void unload() {
        logger.debug("Unloading LoRA adapter weights: {}", type);
        // In production: munmap weights
        isLoaded = false;
    }
    
    /**
     * Check if adapter is loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Set context value
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
    
    public AdapterType getType() {
        return type;
    }
}

/**
 * Switch result with timing information
 */
class SwitchResult {
    private final boolean success;
    private final long switchTimeMs;
    private final String message;
    
    public SwitchResult(boolean success, long switchTimeMs, String message) {
        this.success = success;
        this.switchTimeMs = switchTimeMs;
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public long getSwitchTimeMs() {
        return switchTimeMs;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return String.format("SwitchResult{success=%s, time=%dms, message='%s'}", 
            success, switchTimeMs, message);
    }
}

/**
 * Switch statistics
 */
class SwitchStats {
    private final long totalSwitches;
    private final long totalTimeMs;
    private final double avgSwitchTimeMs;
    private final AdapterType currentAdapter;
    
    public SwitchStats(long totalSwitches, long totalTimeMs, 
                       double avgSwitchTimeMs, AdapterType currentAdapter) {
        this.totalSwitches = totalSwitches;
        this.totalTimeMs = totalTimeMs;
        this.avgSwitchTimeMs = avgSwitchTimeMs;
        this.currentAdapter = currentAdapter;
    }
    
    public long getTotalSwitches() {
        return totalSwitches;
    }
    
    public long getTotalTimeMs() {
        return totalTimeMs;
    }
    
    public double getAvgSwitchTimeMs() {
        return avgSwitchTimeMs;
    }
    
    public AdapterType getCurrentAdapter() {
        return currentAdapter;
    }
    
    @Override
    public String toString() {
        return String.format("SwitchStats{switches=%d, totalTime=%dms, avg=%.2fms, current=%s}",
            totalSwitches, totalTimeMs, avgSwitchTimeMs, currentAdapter);
    }
}
