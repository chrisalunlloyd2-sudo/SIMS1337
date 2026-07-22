# 🚀 SIMS JAVA NEO FX - PHASE 2A IMPLEMENTATION

**Start Date:** 2026-07-20  
**Target:** Complete Core Routing (ModelRouter, TaskQueue, LoRASwitcher)  
**Duration:** 5 days  
**Coverage Target:** >90%

---

## 📋 DAY 1: ModelRouter + ModelPool

### Morning (9:00-12:00)
```bash
# Create package structure
mkdir -p src/main/java/com/aigen/sims/routing
mkdir -p src/test/java/com/aigen/sims/routing
```

### Implement ModelPool.java
```java
package com.aigen.sims.routing;

import com.aigen.sims.agents.SLMAgent;
import java.util.HashMap;
import java.util.Map;

/**
 * Model Pool - Manages all 4 SLM models
 * Keeps models warm and ready for instant access
 */
public class ModelPool {
    
    private final Map<String, SLMAgent> models;
    
    public ModelPool() {
        models = new HashMap<>();
        initializeModels();
    }
    
    private void initializeModels() {
        // Fast tier
        models.put("qwen2_0_5b", new SLMAgent("qwen2.5:0.5b"));
        
        // Balanced tier
        models.put("tinyllama_1_1b", new SLMAgent("tinyllama:1.1b"));
        
        // Reasoning tier
        models.put("phi_latest", new SLMAgent("phi:latest"));
        
        // Deep tier
        models.put("phi3_mini", new SLMAgent("phi3:mini"));
        
        // Warm up all models
        warmUpAll();
    }
    
    private void warmUpAll() {
        for (SLMAgent model : models.values()) {
            model.warmUp();
        }
    }
    
    public SLMAgent getQwen2_0_5b() {
        return models.get("qwen2_0_5b");
    }
    
    public SLMAgent getTinyllama_1_1b() {
        return models.get("tinyllama_1_1b");
    }
    
    public SLMAgent getPhiLatest() {
        return models.get("phi_latest");
    }
    
    public SLMAgent getPhi3Mini() {
        return models.get("phi3_mini");
    }
    
    public SLMAgent getModel(String name) {
        return models.get(name);
    }
    
    public Map<String, SLMAgent> getAllModels() {
        return new HashMap<>(models);
    }
}
```

### Implement ModelRouter.java
```java
package com.aigen.sims.routing;

import com.aigen.sims.agents.SLMAgent;
import com.aigen.sims.tasks.Task;
import com.aigen.sims.tasks.Complexity;

/**
 * Model Router - Selects appropriate model for each task
 * Routes based on complexity, latency requirements, and model availability
 */
public class ModelRouter {
    
    private final ModelPool modelPool;
    
    // Complexity thresholds
    private static final double SIMPLE_THRESHOLD = 0.3;
    private static final double MEDIUM_THRESHOLD = 0.6;
    private static final double COMPLEX_THRESHOLD = 0.8;
    
    public ModelRouter(ModelPool modelPool) {
        this.modelPool = modelPool;
    }
    
    /**
     * Select best model for task based on complexity
     */
    public SLMAgent selectModel(Task task) {
        Complexity complexity = task.getComplexity();
        
        switch (complexity) {
            case VERY_LOW:
            case LOW:
                return modelPool.getQwen2_0_5b();
            
            case MEDIUM:
                return modelPool.getTinyllama_1_1b();
            
            case HIGH:
                return modelPool.getPhiLatest();
            
            case VERY_HIGH:
            case CRITICAL:
                return modelPool.getPhi3Mini();
            
            default:
                // Default to balanced model
                return modelPool.getTinyllama_1_1b();
        }
    }
    
    /**
     * Select model with latency constraint
     */
    public SLMAgent selectModelWithLatency(Task task, long maxLatencyMs) {
        if (maxLatencyMs < 200) {
            // Must use fastest model
            return modelPool.getQwen2_0_5b();
        } else if (maxLatencyMs < 1000) {
            // Can use balanced model
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
        
        return new RoutingRecommendation(
            selectedModel,
            estimatedLatency,
            getReason(task)
        );
    }
    
    private long estimateLatency(SLMAgent model, Task task) {
        // Base latency by model
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
    
    public SLMAgent getModel() { return model; }
    public long getEstimatedLatencyMs() { return estimatedLatencyMs; }
    public String getReason() { return reason; }
}
```

### Afternoon (13:00-17:00)
### Write ModelRouterTest.java
```java
package com.aigen.sims.routing;

import com.aigen.sims.agents.SLMAgent;
import com.aigen.sims.tasks.Task;
import com.aigen.sims.tasks.Complexity;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ModelRouterTest {
    
    private ModelRouter router;
    private ModelPool modelPool;
    
    @BeforeEach
    public void setUp() {
        modelPool = new ModelPool();
        router = new ModelRouter(modelPool);
    }
    
    @Test
    @DisplayName("Route simple task to qwen2.5:0.5b")
    public void testSimpleTaskRouting() {
        Task task = new Task("classify_image", Complexity.LOW);
        
        SLMAgent model = router.selectModel(task);
        
        assertThat(model).isEqualTo(modelPool.getQwen2_0_5b());
    }
    
    @Test
    @DisplayName("Route medium task to tinyllama:1.1b")
    public void testMediumTaskRouting() {
        Task task = new Task("pathfinding", Complexity.MEDIUM);
        
        SLMAgent model = router.selectModel(task);
        
        assertThat(model).isEqualTo(modelPool.getTinyllama_1_1b());
    }
    
    @Test
    @DisplayName("Route complex task to phi:latest")
    public void testComplexTaskRouting() {
        Task task = new Task("social_interaction", Complexity.HIGH);
        
        SLMAgent model = router.selectModel(task);
        
        assertThat(model).isEqualTo(modelPool.getPhiLatest());
    }
    
    @Test
    @DisplayName("Route critical task to phi3:mini")
    public void testCriticalTaskRouting() {
        Task task = new Task("career_decision", Complexity.CRITICAL);
        
        SLMAgent model = router.selectModel(task);
        
        assertThat(model).isEqualTo(modelPool.getPhi3Mini());
    }
    
    @Test
    @DisplayName("Model selection latency <10ms")
    public void testRoutingLatency() {
        Task task = new Task("test", Complexity.MEDIUM);
        
        long start = System.nanoTime();
        router.selectModel(task);
        long duration = System.nanoTime() - start;
        
        assertThat(duration).isLessThan(10_000_000); // <10ms
    }
    
    @Test
    @DisplayName("Latency-constrained routing")
    public void testLatencyConstrainedRouting() {
        Task task = new Task("urgent", Complexity.HIGH);
        
        // Must complete in <200ms, so use fastest model
        SLMAgent model = router.selectModelWithLatency(task, 200);
        
        assertThat(model).isEqualTo(modelPool.getQwen2_0_5b());
    }
    
    @Test
    @DisplayName("Routing recommendation includes reason")
    public void testRoutingRecommendation() {
        Task task = new Task("test", Complexity.MEDIUM);
        
        RoutingRecommendation rec = router.getRecommendation(task);
        
        assertThat(rec.getModel()).isNotNull();
        assertThat(rec.getEstimatedLatencyMs()).isGreaterThan(0);
        assertThat(rec.getReason()).isNotNull();
        assertThat(rec.getReason()).contains("balanced model");
    }
}
```

### Evening (18:00-20:00)
```bash
# Run tests
mvn test -Dtest=ModelRouterTest

# Check coverage
mvn test jacoco:report

# Commit
git add .
git commit -m "Phase 2A: ModelRouter + ModelPool complete with tests"
git push origin main
```

---

## 📋 DAY 2: TaskQueue

### Morning
### Implement TaskQueue.java
```java
package com.aigen.sims.routing;

import com.aigen.sims.tasks.Task;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Task Queue - Priority queue for agent tasks
 * Ensures high-priority tasks are processed first
 */
public class TaskQueue {
    
    private final BlockingQueue<Task> queue;
    private final int maxCapacity;
    
    public TaskQueue(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.queue = new LinkedBlockingQueue<>(maxCapacity);
    }
    
    /**
     * Add task to queue
     * @param task Task to enqueue
     * @return true if successful, false if queue is full
     */
    public boolean enqueue(Task task) {
        return queue.offer(task);
    }
    
    /**
     * Get next task (highest priority)
     * @return Task or null if queue is empty
     */
    public Task dequeue() {
        return queue.poll();
    }
    
    /**
     * Get next task, blocking if empty
     * @return Task (blocks until available)
     */
    public Task dequeueBlocking() throws InterruptedException {
        return queue.take();
    }
    
    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Get queue size
     */
    public int size() {
        return queue.size();
    }
    
    /**
     * Clear all tasks
     */
    public void clear() {
        queue.clear();
    }
    
    /**
     * Get queue statistics
     */
    public QueueStats getStats() {
        return new QueueStats(
            queue.size(),
            maxCapacity,
            (double) queue.size() / maxCapacity
        );
    }
}

/**
 * Queue statistics
 */
class QueueStats {
    private final int currentSize;
    private final int maxCapacity;
    private final double utilization;
    
    public QueueStats(int currentSize, int maxCapacity, double utilization) {
        this.currentSize = currentSize;
        this.maxCapacity = maxCapacity;
        this.utilization = utilization;
    }
    
    public int getCurrentSize() { return currentSize; }
    public int getMaxCapacity() { return maxCapacity; }
    public double getUtilization() { return utilization; }
}
```

### Afternoon
### Write TaskQueueTest.java
```java
package com.aigen.sims.routing;

import com.aigen.sims.tasks.Task;
import com.aigen.sims.tasks.Complexity;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TaskQueueTest {
    
    private TaskQueue queue;
    
    @BeforeEach
    public void setUp() {
        queue = new TaskQueue(100);
    }
    
    @Test
    @DisplayName("Enqueue and dequeue single task")
    public void testSingleTask() {
        Task task = new Task("test", Complexity.MEDIUM);
        
        assertThat(queue.enqueue(task)).isTrue();
        assertThat(queue.size()).isEqualTo(1);
        
        Task dequeued = queue.dequeue();
        assertThat(dequeued).isEqualTo(task);
        assertThat(queue.isEmpty()).isTrue();
    }
    
    @Test
    @DisplayName("Queue respects capacity limit")
    public void testCapacityLimit() {
        TaskQueue smallQueue = new TaskQueue(5);
        
        for (int i = 0; i < 5; i++) {
            assertThat(smallQueue.enqueue(new Task("task" + i, Complexity.LOW))).isTrue();
        }
        
        // Queue is full
        assertThat(smallQueue.enqueue(new Task("overflow", Complexity.LOW))).isFalse();
        assertThat(smallQueue.size()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("Queue statistics accurate")
    public void testQueueStats() {
        for (int i = 0; i < 25; i++) {
            queue.enqueue(new Task("task" + i, Complexity.LOW));
        }
        
        QueueStats stats = queue.getStats();
        
        assertThat(stats.getCurrentSize()).isEqualTo(25);
        assertThat(stats.getMaxCapacity()).isEqualTo(100);
        assertThat(stats.getUtilization()).isEqualTo(0.25);
    }
    
    @Test
    @DisplayName("Clear removes all tasks")
    public void testClear() {
        for (int i = 0; i < 10; i++) {
            queue.enqueue(new Task("task" + i, Complexity.LOW));
        }
        
        queue.clear();
        
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.size()).isEqualTo(0);
    }
}
```

---

## 📋 DAY 3-4: LoRASwitcher

### Implement LoRASwitcher.java
### Write LoRASwitcherTest.java

---

## 📋 DAY 5: Integration + Polish

### Run All Tests
```bash
mvn clean test
mvn test jacoco:report
```

### Verify Coverage
```bash
# Open: target/site/jacoco/index.html
# Verify >90% coverage on routing package
```

### Documentation
- Update CHANGELOG.md
- Update README.md
- Commit and push

---

## ✅ PHASE 2A COMPLETION CRITERIA

- [ ] ModelRouter.java implemented
- [ ] ModelPool.java implemented
- [ ] TaskQueue.java implemented
- [ ] LoRASwitcher.java implemented
- [ ] All tests passing
- [ ] Coverage >90%
- [ ] Documentation updated
- [ ] Code committed to GitHub

---

**Phase 2A: 5 days to core routing complete.**  
**Let's build.** 🚀💙
