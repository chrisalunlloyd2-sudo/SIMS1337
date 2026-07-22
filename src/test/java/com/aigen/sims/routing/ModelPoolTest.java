package com.aigen.sims.routing;

import com.aigen.sims.agents.SLMAgent;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ModelPoolTest {
    
    private ModelPool modelPool;
    
    @BeforeEach
    public void setUp() {
        modelPool = new ModelPool();
    }
    
    @Test
    @DisplayName("Model pool initializes with 4 models")
    public void testModelPoolInitialization() {
        assertThat(modelPool.getModelCount()).isEqualTo(4);
    }
    
    @Test
    @DisplayName("Get fast tier model (qwen2.5:0.5b)")
    public void testGetQwenModel() {
        SLMAgent model = modelPool.getQwen2_0_5b();
        
        assertThat(model).isNotNull();
        assertThat(model.getModelName()).isEqualTo("qwen2.5:0.5b");
    }
    
    @Test
    @DisplayName("Get balanced tier model (tinyllama:1.1b)")
    public void testGetTinyllamaModel() {
        SLMAgent model = modelPool.getTinyllama_1_1b();
        
        assertThat(model).isNotNull();
        assertThat(model.getModelName()).isEqualTo("tinyllama:1.1b");
    }
    
    @Test
    @DisplayName("Get reasoning tier model (phi:latest)")
    public void testGetPhiModel() {
        SLMAgent model = modelPool.getPhiLatest();
        
        assertThat(model).isNotNull();
        assertThat(model.getModelName()).isEqualTo("phi:latest");
    }
    
    @Test
    @DisplayName("Get deep tier model (phi3:mini)")
    public void testGetPhi3Model() {
        SLMAgent model = modelPool.getPhi3Mini();
        
        assertThat(model).isNotNull();
        assertThat(model.getModelName()).isEqualTo("phi3:mini");
    }
    
    @Test
    @DisplayName("Get model by name")
    public void testGetModelByName() {
        SLMAgent model = modelPool.getModel("phi_latest");
        
        assertThat(model).isNotNull();
        assertThat(model.getModelName()).isEqualTo("phi:latest");
    }
    
    @Test
    @DisplayName("Get all models returns read-only copy")
    public void testGetAllModels() {
        var allModels = modelPool.getAllModels();
        
        assertThat(allModels).hasSize(4);
        assertThat(allModels).containsKeys("qwen2_0_5b", "tinyllama_1_1b", "phi_latest", "phi3_mini");
    }
    
    @Test
    @DisplayName("Model names are correct")
    public void testModelNames() {
        assertThat(modelPool.getQwen2_0_5b().getModelName()).isEqualTo("qwen2.5:0.5b");
        assertThat(modelPool.getTinyllama_1_1b().getModelName()).isEqualTo("tinyllama:1.1b");
        assertThat(modelPool.getPhiLatest().getModelName()).isEqualTo("phi:latest");
        assertThat(modelPool.getPhi3Mini().getModelName()).isEqualTo("phi3:mini");
    }
}
