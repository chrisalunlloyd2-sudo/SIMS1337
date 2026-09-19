package com.aigen.sims.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * EventBus -- 2026-07-28 Architect gist 3.4: "New module: pipeline-scheduler/ -- event bus with
 * FOW-gated task claims." A real, minimal, in-process pub/sub bus: one JVM, no distributed
 * messaging needed. A handler that throws never breaks another subscriber or the publisher.
 */
public class EventBus {
    private final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();

    public void subscribe(String event, Consumer<Object> handler) {
        subscribers.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void publish(String event, Object payload) {
        for (Consumer<Object> h : subscribers.getOrDefault(event, List.of())) {
            try {
                h.accept(payload);
            } catch (Exception e) {
                System.err.println("[EventBus] handler for '" + event + "' threw: " + e.getMessage());
            }
        }
    }
}
