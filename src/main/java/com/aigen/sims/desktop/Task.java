package com.aigen.sims.desktop;

import javafx.beans.property.*;

/** Desktop-paradigm Task model. */
public class Task {
    private final String id;
    private final String title;
    private final Priority priority;
    private final String deadline;          // "2026-07-20 18:00"
    private final StringProperty status = new SimpleStringProperty("Pending");
    private String assignedAgent;

    public Task(String id, String title, Priority priority, String deadline) {
        this.id = id; this.title = title; this.priority = priority; this.deadline = deadline;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public Priority getPriority() { return priority; }
    public String getDeadline() { return deadline; }
    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String s) { status.set(s); }
    public String getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(String a) { this.assignedAgent = a; }
    @Override public String toString() { return title; }
}
