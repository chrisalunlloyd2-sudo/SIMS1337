package com.aigen.sims.phase1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FOWGate — Fog-of-War visibility gate.
 * Determines if a model (via its assigned agent) can see a given hex.
 * From hex-fow gist: middleware-fow.go → Java port.
 */
public class FOWGate {
    public static final int DEFAULT_HOP = 1;

    private final Map<String, String> agentHex;     // agent name → "q,r"
    private final Map<String, String> modelAgent;    // model name → agent name
    private final int hopLimit;
    private boolean enabled = true;

    public FOWGate() { this(DEFAULT_HOP); }

    public FOWGate(int hopLimit) {
        this.hopLimit = hopLimit;
        this.agentHex = new ConcurrentHashMap<>();
        this.modelAgent = new ConcurrentHashMap<>();
    }

    /** Pin an agent to a hex coordinate */
    public void pinAgent(String agent, HexCoord hex) {
        agentHex.put(agent, hex.key());
    }

    /** Assign a model to an agent for FOV purposes */
    public void assignModel(String model, String agent) {
        modelAgent.put(model, agent);
    }

    /** Check if a hex is visible to a given model */
    public boolean isVisible(HexCoord target, String model) {
        if (!enabled) return true;
        String agent = modelAgent.get(model);
        if (agent == null) return true; // unassigned → sees all
        String agentKey = agentHex.get(agent);
        if (agentKey == null) return true;
        return HexCoord.fromString(agentKey).distanceTo(target) <= hopLimit;
    }

    /** Slice a hex coordinate to a model's visible neighborhood (null if outside) */
    public HexCoord sliceLocal(HexCoord globalCoord, String model) {
        if (isVisible(globalCoord, model)) return globalCoord;
        return null; // outside FOW — return null (not visible)
    }

    public void setEnabled(boolean e) { this.enabled = e; }
    public boolean isEnabled() { return enabled; }
    public int getHopLimit() { return hopLimit; }
    public int agentCount() { return agentHex.size(); }
    public int modelCount() { return modelAgent.size(); }
    public java.util.Set<String> modelNames() { return modelAgent.keySet(); }
    public String agentFor(String model) { return modelAgent.get(model); }
    public HexCoord hexFor(String agent) {
        String key = agentHex.get(agent);
        return key != null ? HexCoord.fromString(key) : null;
    }
}
