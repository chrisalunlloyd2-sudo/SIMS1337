package com.aigen.sims.routing;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoRASwitcherTest {
    
    private LoRASwitcher switcher;
    
    @BeforeEach
    public void setUp() {
        switcher = new LoRASwitcher();
    }
    
    @Test
    @DisplayName("Switcher initializes with 6 adapters")
    public void testAdapterInitialization() {
        assertThat(switcher.getAvailableAdapters()).hasSize(6);
    }
    
    @Test
    @DisplayName("Initial adapter is CHAT")
    public void testInitialAdapter() {
        assertThat(switcher.getCurrentAdapterType()).isEqualTo(AdapterType.CHAT);
    }
    
    @Test
    @DisplayName("Switch adapter returns success")
    public void testSwitchSuccess() {
        SwitchResult result = switcher.switchAdapter(AdapterType.CODE);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Success");
    }
    
    @Test
    @DisplayName("Switch time is <100ms (target)")
    public void testSwitchTime() {
        SwitchResult result = switcher.switchAdapter(AdapterType.CODE);
        
        assertThat(result.getSwitchTimeMs()).isLessThan(100);
    }
    
    @Test
    @DisplayName("Switch to same adapter returns already active")
    public void testSwitchToSameAdapter() {
        // Already on CHAT
        SwitchResult result = switcher.switchAdapter(AdapterType.CHAT);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSwitchTimeMs()).isEqualTo(0);
        assertThat(result.getMessage()).isEqualTo("Already active");
    }
    
    @Test
    @DisplayName("Circular switching works")
    public void testCircularSwitching() {
        AdapterType initial = switcher.getCurrentAdapterType();
        
        // Switch to next 6 times (full circle)
        for (int i = 0; i < 6; i++) {
            switcher.switchToNext();
        }
        
        // Should be back to initial
        assertThat(switcher.getCurrentAdapterType()).isEqualTo(initial);
    }
    
    @Test
    @DisplayName("Context is preserved on adapter")
    public void testContextPreservation() {
        switcher.setContext("test_key", "test_value");
        
        String value = switcher.getContext("test_key");
        
        assertThat(value).isEqualTo("test_value");
    }
    
    @Test
    @DisplayName("Switch statistics are tracked")
    public void testSwitchStats() {
        // Perform 5 switches
        for (int i = 0; i < 5; i++) {
            switcher.switchToNext();
        }
        
        SwitchStats stats = switcher.getStats();
        
        assertThat(stats.getTotalSwitches()).isEqualTo(5);
        assertThat(stats.getTotalTimeMs()).isGreaterThan(0);
        assertThat(stats.getAvgSwitchTimeMs()).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("All adapter types are available")
    public void testAllAdapterTypes() {
        AdapterType[] adapters = switcher.getAvailableAdapters();
        
        assertThat(adapters).containsExactlyInAnyOrder(
            AdapterType.CHAT,
            AdapterType.CODE,
            AdapterType.PATHFIND,
            AdapterType.MOTIVES,
            AdapterType.CAREER,
            AdapterType.ANALYSIS
        );
    }
    
    @Test
    @DisplayName("Switch from CHAT to CODE changes adapter")
    public void testAdapterChange() {
        assertThat(switcher.getCurrentAdapterType()).isEqualTo(AdapterType.CHAT);
        
        switcher.switchAdapter(AdapterType.CODE);
        
        assertThat(switcher.getCurrentAdapterType()).isEqualTo(AdapterType.CODE);
    }
    
    @Test
    @DisplayName("Average switch time is calculated correctly")
    public void testAverageSwitchTime() {
        // Perform known number of switches
        switcher.switchAdapter(AdapterType.CODE);
        switcher.switchAdapter(AdapterType.PATHFIND);
        switcher.switchAdapter(AdapterType.MOTIVES);
        
        SwitchStats stats = switcher.getStats();
        
        assertThat(stats.getTotalSwitches()).isEqualTo(3);
        // Average should be total / count
        double expectedAvg = (double) stats.getTotalTimeMs() / stats.getTotalSwitches();
        assertThat(stats.getAvgSwitchTimeMs()).isCloseTo(expectedAvg, within(0.01));
    }
}
