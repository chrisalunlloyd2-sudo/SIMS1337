package com.aigen.sims.gui;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
public class GuiGardener {
    private final ConcurrentHashMap<String, ComponentProposal> proposals = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> componentMap = new ConcurrentHashMap<>();
    public String registerProposal(ComponentProposal p) {
        proposals.put(p.id, p);
        componentMap.computeIfAbsent(p.insertAfter, k -> Collections.synchronizedList(new ArrayList<>())).add(p.id);
        return p.id;
    }
    public String approveProposal(String id) {
        ComponentProposal p = proposals.get(id);
        if (p == null || !p.isPending()) return "ERROR: not found or not pending";
        proposals.put(id, p.withStatus("APPROVED"));
        return generateInsertionCode(p);
    }
    public boolean rejectProposal(String id) {
        ComponentProposal p = proposals.get(id);
        if (p == null || !p.isPending()) return false;
        proposals.put(id, p.withStatus("REJECTED")); return true;
    }
    public boolean markDeployed(String id) {
        ComponentProposal p = proposals.get(id);
        if (p == null || !p.isApproved()) return false;
        proposals.put(id, p.withStatus("DEPLOYED")); return true;
    }
    public String generateInsertionCode(ComponentProposal p) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n        // === GUI: ").append(p.componentName).append(" (").append(p.id).append(") ===\n");
        sb.append("        // Type: ").append(p.componentType).append(" | Model: ").append(p.modelName).append("\n");
        sb.append("        // Description: ").append(p.description).append("\n");
        sb.append("        // Inserted after: ").append(p.insertAfter).append("\n");
        sb.append(p.code);
        if (!p.code.endsWith("\n")) sb.append("\n");
        sb.append("        // === END GUI: ").append(p.componentName).append(" ===\n");
        return sb.toString();
    }
    public String getComponentMapString() {
        StringBuilder sb = new StringBuilder("🗺️ GUI Component Map\n");
        for (Map.Entry<String, List<String>> entry : componentMap.entrySet()) {
            sb.append("  After \"").append(entry.getKey()).append("\":\n");
            for (String pid : entry.getValue()) {
                ComponentProposal p = proposals.get(pid);
                if (p != null) sb.append("    - ").append(p.componentType).append(" \"").append(p.componentName)
                    .append("\" [").append(p.status).append("] by ").append(p.modelName).append("\n");
            }
        }
        return sb.toString();
    }
    public List<ComponentProposal> getPending() {
        return proposals.values().stream().filter(ComponentProposal::isPending).collect(Collectors.toList());
    }
    public List<ComponentProposal> getApproved() {
        return proposals.values().stream().filter(ComponentProposal::isApproved).collect(Collectors.toList());
    }
    public List<ComponentProposal> getDeployed() {
        return proposals.values().stream().filter(ComponentProposal::isDeployed).collect(Collectors.toList());
    }
    public ComponentProposal getProposal(String id) { return proposals.get(id); }
    public Map<String,Integer> getSummary() {
        Map<String,Integer> m = new HashMap<>();
        m.put("total", proposals.size());
        m.put("pending", (int)proposals.values().stream().filter(ComponentProposal::isPending).count());
        m.put("approved", (int)proposals.values().stream().filter(ComponentProposal::isApproved).count());
        m.put("deployed", (int)proposals.values().stream().filter(ComponentProposal::isDeployed).count());
        m.put("rejected", (int)proposals.values().stream().filter(ComponentProposal::isRejected).count());
        return m;
    }
    public Set<String> getComponentTypes() {
        return proposals.values().stream().map(p -> p.componentType).collect(Collectors.toSet());
    }
}
