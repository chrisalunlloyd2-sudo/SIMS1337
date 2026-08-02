package com.aigen.sims.tasks;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Task representation for agent processing
 */
public class Task {
    private final String id;
    private final String name;
    private final String description;
    private final Complexity complexity;
    private final long maxLatencyMs;
    private final LocalDateTime createdAt;
    private TaskStatus status;
    private int priority = 5; // 1 (highest) - 10 (lowest), default 5

    public Task(String name, Complexity complexity) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = "";
        this.complexity = complexity;
        this.maxLatencyMs = Long.MAX_VALUE;
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.priority = 5;
    }

    public Task(String name, String description, Complexity complexity, long maxLatencyMs) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.complexity = complexity;
        this.maxLatencyMs = maxLatencyMs;
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.priority = 5;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Complexity getComplexity() {
        return complexity;
    }

    public long getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Task{id='%s', name='%s', complexity=%s, status=%s}", 
            id, name, complexity, status);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException("priority must be 1-10");
        }
        this.priority = priority;
    }
}
