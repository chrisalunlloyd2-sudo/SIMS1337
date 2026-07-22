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
        assertThat(stats.getTotalEnqueued()).isEqualTo(25);
        assertThat(stats.getTotalDequeued()).isEqualTo(0);
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
    
    @Test
    @DisplayName("Dequeue updates statistics")
    public void testDequeueStats() {
        for (int i = 0; i < 10; i++) {
            queue.enqueue(new Task("task" + i, Complexity.LOW));
        }
        
        for (int i = 0; i < 5; i++) {
            queue.dequeue();
        }
        
        QueueStats stats = queue.getStats();
        
        assertThat(stats.getTotalEnqueued()).isEqualTo(10);
        assertThat(stats.getTotalDequeued()).isEqualTo(5);
        assertThat(stats.getCurrentSize()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("Empty queue returns null on dequeue")
    public void testEmptyQueueDequeue() {
        Task dequeued = queue.dequeue();
        
        assertThat(dequeued).isNull();
    }
    
    @Test
    @DisplayName("Queue tracks utilization correctly")
    public void testUtilization() {
        TaskQueue smallQueue = new TaskQueue(10);
        
        for (int i = 0; i < 5; i++) {
            smallQueue.enqueue(new Task("task" + i, Complexity.LOW));
        }
        
        QueueStats stats = smallQueue.getStats();
        
        assertThat(stats.getUtilization()).isEqualTo(0.5);
    }
    
    @Test
    @DisplayName("isFull returns true at capacity")
    public void testIsFull() {
        TaskQueue smallQueue = new TaskQueue(3);
        
        assertThat(smallQueue.isFull()).isFalse();
        
        smallQueue.enqueue(new Task("1", Complexity.LOW));
        assertThat(smallQueue.isFull()).isFalse();
        
        smallQueue.enqueue(new Task("2", Complexity.LOW));
        assertThat(smallQueue.isFull()).isFalse();
        
        smallQueue.enqueue(new Task("3", Complexity.LOW));
        assertThat(smallQueue.isFull()).isTrue();
    }
}
