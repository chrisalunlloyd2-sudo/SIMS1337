package com.aigen.sims.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KnowledgeGraph — in-memory entity graph for the SIMS1337 fleet.
 * Nodes are entities; edges are typed relations ("depends_on", "implements", ...).
 * Cross-repo KG: 48 repos indexed, nodes carry tag sets for search.
 */
public class KnowledgeGraph {
    private final Map<String, Set<String>> edges = new ConcurrentHashMap<>(); // "a->b" relations
    private final Map<String, Set<String>> tags = new ConcurrentHashMap<>();
    private final Set<String> nodes = ConcurrentHashMap.newKeySet();

    public void addNode(String id) { nodes.add(id); tags.putIfAbsent(id, ConcurrentHashMap.newKeySet()); }

    public void addNode(String id, String... tagList) {
        addNode(id);
        Set<String> t = tags.get(id);
        for (String tag : tagList) t.add(tag);
    }

    public void addEdge(String from, String to, String type) {
        addNode(from); addNode(to);
        edges.computeIfAbsent(from + "->" + to, k -> ConcurrentHashMap.newKeySet()).add(type);
    }

    public Set<String> getNodes() { return new HashSet<>(nodes); }

    /** All neighbors reachable from id via any edge. */
    public Set<String> neighbors(String id) {
        Set<String> out = new HashSet<>();
        for (String key : edges.keySet()) {
            String[] parts = key.split("->");
            if (parts.length != 2) continue;
            if (parts[0].equals(id)) out.add(parts[1]);
            if (parts[1].equals(id)) out.add(parts[0]);
        }
        return out;
    }

    /** Search nodes by tag (case-insensitive substring). */
    public List<String> searchByTag(String tag) {
        List<String> out = new ArrayList<>();
        String t = tag.toLowerCase();
        for (Map.Entry<String, Set<String>> e : tags.entrySet())
            for (String s : e.getValue())
                if (s.toLowerCase().contains(t)) { out.add(e.getKey()); break; }
        return out;
    }

    /** BFS depth-limited reachability from start (used for impact analysis). */
    public Set<String> reachable(String start, int maxDepth) {
        Set<String> seen = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        Map<String, Integer> depth = new HashMap<>();
        queue.add(start); seen.add(start); depth.put(start, 0);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            int d = depth.get(cur);
            if (d >= maxDepth) continue;
            for (String nb : neighbors(cur)) {
                if (!seen.contains(nb)) {
                    seen.add(nb); depth.put(nb, d + 1); queue.add(nb);
                }
            }
        }
        return seen;
    }

    public int size() { return nodes.size(); }
}
