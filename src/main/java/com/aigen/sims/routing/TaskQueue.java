package com.aigen.sims.routing;

import com.aigen.sims.tasks.Task;
import com.aigen.sims.tasks.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Task Queue - Priority queue for agent tasks
 * Ensures high-priority tasks are processed first
 */
public class TaskQueue {
    private static final Logger logger = LoggerFactory.getLogger(TaskQueue.class);
    
    private final BlockingQueue<Task> queue;
    private final int maxCapacity;
    private long totalEnqueued;
    private long totalDequeued;
    
    public TaskQueue(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.queue = new LinkedBlockingQueue<>(maxCapacity);
        this.totalEnqueued = 0;
        this.totalDequeued = 0;
        logger.info("TaskQueue initialized with capacity {}", maxCapacity);
    }
    
    /**
     * Add task to queue
     * @param task Task to enqueue
     * @return true if successful, false if queue is full
     */
    public boolean enqueue(Task task) {
        boolean success = queue.offer(task);
        if (success) {
            totalEnqueued++;
            task.setStatus(TaskStatus.IN_PROGRESS);
            logger.debug("Enqueued task '{}' (queue size: {}/{})", 
                task.getName(), queue.size(), maxCapacity);
        } else {
            logger.warn("Queue full, rejected task '{}'", task.getName());
        }
        return success;
    }
    
    /**
     * Get next task (highest priority)
     * @return Task or null if queue is empty
     */
    public Task dequeue() {
        Task task = queue.poll();
        if (task != null) {
            totalDequeued++;
            logger.debug("Dequeued task '{}' (queue size: {}/{})", 
                task.getName(), queue.size(), maxCapacity);
        }
        return task;
    }
    
    /**
     * Get next task, blocking if empty
     * @return Task (blocks until available)
     */
    public Task dequeueBlocking() throws InterruptedException {
        Task task = queue.take();
        totalDequeued++;
        logger.debug("Dequeued task '{}' (blocking)", task.getName());
        return task;
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
     * Get max capacity
     */
    public int getCapacity() {
        return maxCapacity;
    }
    
    /**
     * Clear all tasks
     */
    public void clear() {
        queue.clear();
        logger.info("TaskQueue cleared");
    }
    
    /**
     * Get queue statistics
     */
    public QueueStats getStats() {
        return new QueueStats(
            queue.size(),
            maxCapacity,
            (double) queue.size() / maxCapacity,
            totalEnqueued,
            totalDequeued
        );
    }
    
    /**
     * Check if queue is full
     */
    public boolean isFull() {
        return queue.size() >= maxCapacity;
    }
}

/**
 * Queue statistics
 */
class QueueStats {
    private final int currentSize;
    private final int maxCapacity;
    private final double utilization;
    private final long totalEnqueued;
    private final long totalDequeued;
    
    public QueueStats(int currentSize, int maxCapacity, double utilization, 
                      long totalEnqueued, long totalDequeued) {
        this.currentSize = currentSize;
        this.maxCapacity = maxCapacity;
        this.utilization = utilization;
        this.totalEnqueued = totalEnqueued;
        this.totalDequeued = totalDequeued;
    }
    
    public int getCurrentSize() {
        return currentSize;
    }
    
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    public double getUtilization() {
        return utilization;
    }
    
    public long getTotalEnqueued() {
        return totalEnqueued;
    }
    
    public long getTotalDequeued() {
        return totalDequeued;
    }
    
    @Override
    public String toString() {
        return String.format("QueueStats{size=%d/%d, utilization=%.2f, enqueued=%d, dequeued=%d}",
            currentSize, maxCapacity, utilization, totalEnqueued, totalDequeued);
    }
}
