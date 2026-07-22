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
        assertThat(model.getModelName()).isEqualTo("qwen2.5:0.5b");
    }
    
    @Test
    @DisplayName("Route medium task to tinyllama:1.1b")
    public void testMediumTaskRouting() {
        Task task = new Task("pathfinding", Complexity.MEDIUM);
        
        SLMAgent model = router.selectModel(task);
        
        assertThat(model).isEqualTo(modelPool.getTinyllama_1_1b());
        assertThat(model.getModelName()).isEqualTo("tinyllama:1.1b");
    }
    
    @Test
    @DisplayName("Route complex task to phi:latest")
    public void testComplexTaskRouting() {
        Task task = new Task("social_interaction", Complexity.HIGH);
        
        SLMAgent model = router.selectModel(task);
        
        assertThat(model).isEqualTo(modelPool.getPhiLatest());
        assertThat(model.getModelName()).isEqualTo("phi:latest");
    }
    
    @Test
    @DisplayName("Route critical task to phi3:mini")
    public void testCriticalTaskRouting() {
        Task task = new Task("career_decision", Complexity.CRITICAL);
        
        SLMAgent model = router.selectModel(task);
        
        assertThat(model).isEqualTo(modelPool.getPhi3Mini());
        assertThat(model.getModelName()).isEqualTo("phi3:mini");
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
    @DisplayName("Latency-constrained routing (<200ms)")
    public void testLatencyConstrainedRouting200ms() {
        Task task = new Task("urgent", Complexity.HIGH);
        
        SLMAgent model = router.selectModelWithLatency(task, 200);
        
        // Must use fastest model regardless of complexity
        assertThat(model).isEqualTo(modelPool.getQwen2_0_5b());
    }
    
    @Test
    @DisplayName("Latency-constrained routing (<1000ms)")
    public void testLatencyConstrainedRouting1000ms() {
        Task task = new Task("moderately_urgent", Complexity.HIGH);
        
        SLMAgent model = router.selectModelWithLatency(task, 1000);
        
        // Can use balanced model
        assertThat(model).isEqualTo(modelPool.getTinyllama_1_1b());
    }
    
    @Test
    @DisplayName("Routing recommendation includes all fields")
    public void testRoutingRecommendation() {
        Task task = new Task("test", Complexity.MEDIUM);
        
        RoutingRecommendation rec = router.getRecommendation(task);
        
        assertThat(rec.getModel()).isNotNull();
        assertThat(rec.getEstimatedLatencyMs()).isGreaterThan(0);
        assertThat(rec.getReason()).isNotNull();
        assertThat(rec.getReason()).contains("balanced model");
    }
    
    @Test
    @DisplayName("Routing recommendation for LOW complexity")
    public void testRoutingRecommendationLow() {
        Task task = new Task("simple", Complexity.LOW);
        
        RoutingRecommendation rec = router.getRecommendation(task);
        
        assertThat(rec.getReason()).contains("fastest model");
        assertThat(rec.getReason()).contains("qwen2.5:0.5b");
    }
    
    @Test
    @DisplayName("Routing recommendation for CRITICAL complexity")
    public void testRoutingRecommendationCritical() {
        Task task = new Task("critical", Complexity.CRITICAL);
        
        RoutingRecommendation rec = router.getRecommendation(task);
        
        assertThat(rec.getReason()).contains("deep reasoning");
        assertThat(rec.getReason()).contains("phi3:mini");
    }
    
    @Test
    @DisplayName("Estimated latency increases with complexity")
    public void testLatencyIncreasesWithComplexity() {
        Task lowTask = new Task("low", Complexity.LOW);
        Task highTask = new Task("high", Complexity.HIGH);
        
        RoutingRecommendation lowRec = router.getRecommendation(lowTask);
        RoutingRecommendation highRec = router.getRecommendation(highTask);
        
        // Higher complexity should have higher estimated latency
        assertThat(highRec.getEstimatedLatencyMs())
            .isGreaterThan(lowRec.getEstimatedLatencyMs());
    }
}
