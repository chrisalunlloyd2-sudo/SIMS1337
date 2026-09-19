package com.aigen.sims.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;

/**
 * TocTokTree — Java read-side for the hex-anchored knowledge tree (toc_tok.json).
 *
 * The Python toc_tok.py owns tree WRITES; this class gives Java agents/GUI
 * the READ side: locate nodes by path or hex, get subtree for onboarding,
 * search by keyword. Jackson-backed (jackson-databind 2.15.2 in pom).
 *
 * Example:
 *   TocTokTree tree = new TocTokTree("scripts/toc_tok/toc_tok.json");
 *   tree.atHex(2, 1)                          // nodes at hex + 1-hop
 *   tree.search("markov")                     // keyword hits
 *   tree.subtree("/projects/SIMS1337")        // descendants for onboarding
 */
public class TocTokTree {
    private final File file;
    private JsonNode root;

    public TocTokTree(String path) {
        this.file = new File(path);
        reload();
    }

    public void reload() {
        try {
            if (!file.exists()) { root = null; return; }
            root = new ObjectMapper().readTree(file);
        } catch (Exception e) {
            root = null;
        }
    }

    private JsonNode rootNode() {
        return root == null ? null : root.get("root");
    }

    /** Axial hex distance (same math as HexCoord). */
    public static int hexDistance(int aq, int ar, int bq, int br) {
        int dq = aq - bq, dr = ar - br;
        return Math.max(Math.max(Math.abs(dq), Math.abs(dr)), Math.abs(dq + dr));
    }

    private static int[] parseHex(String h) {
        String[] p = h.trim().split(",");
        return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())};
    }

    /** Collect all nodes depth-first as a flat list. */
    public List<JsonNode> allNodes() {
        List<JsonNode> out = new ArrayList<>();
        if (rootNode() != null) collect(rootNode(), out);
        return out;
    }

    private void collect(JsonNode node, List<JsonNode> out) {
        out.add(node);
        JsonNode kids = node.get("children");
        if (kids != null && kids.isObject()) {
            Iterator<JsonNode> it = kids.elements();
            while (it.hasNext()) collect(it.next(), out);
        }
    }

    /** Nodes anchored at (q,r) or within 1 hop. Returns list of [dist, path]. */
    public List<Map.Entry<Integer, String>> atHex(int q, int r) {
        List<Map.Entry<Integer, String>> out = new ArrayList<>();
        for (JsonNode n : allNodes()) {
            JsonNode h = n.get("hex");
            if (h == null || h.isNull()) continue;
            int[] hh = parseHex(h.asText());
            int d = hexDistance(q, r, hh[0], hh[1]);
            if (d <= 1) out.add(new AbstractMap.SimpleEntry<>(d, n.path("path").asText("/?")));
        }
        out.sort(Comparator.comparingInt(Map.Entry::getKey));
        return out;
    }

    /** Keyword search across title/tags/content/path. */
    public List<String> search(String q) {
        String query = q.toLowerCase();
        List<String> out = new ArrayList<>();
        for (JsonNode n : allNodes()) {
            String blob = (n.path("title").asText("") + " " + n.path("tags").asText("")
                    + " " + n.path("content").asText("") + " " + n.path("path").asText("")).toLowerCase();
            if (blob.contains(query))
                out.add(n.path("path").asText("/?") + " [" + n.path("type").asText("?") + "] @" + n.path("hex").asText("0,0"));
        }
        return out;
    }

    /** Subtree listing for onboarding (path -> descendants, indented). */
    public List<String> subtree(String path) {
        List<String> out = new ArrayList<>();
        JsonNode node = find(path);
        if (node != null) dump(node, 0, out);
        return out;
    }

    private JsonNode find(String path) {
        String[] parts = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
        JsonNode node = rootNode();
        for (String p : parts) {
            if (node == null) return null;
            JsonNode kids = node.get("children");
            if (kids == null || !kids.isObject()) return null;
            node = kids.get(p);
        }
        return node;
    }

    private void dump(JsonNode node, int depth, List<String> out) {
        out.add("  ".repeat(depth) + "[" + node.path("type").asText("?") + "] "
                + node.path("path").asText("/?") + " @" + node.path("hex").asText("0,0"));
        JsonNode kids = node.get("children");
        if (kids != null && kids.isObject()) {
            Iterator<JsonNode> it = kids.elements();
            while (it.hasNext()) dump(it.next(), depth + 1, out);
        }
    }

    public int size() { return allNodes().size(); }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "scripts/toc_tok/toc_tok.json";
        TocTokTree t = new TocTokTree(path);
        System.out.println("nodes: " + t.size());
        System.out.println("search 'markov': " + t.search("markov"));
        System.out.println("atHex(2,1): " + t.atHex(2, 1));
        System.out.println("subtree /projects/SIMS1337:");
        t.subtree("/projects/SIMS1337").forEach(System.out::println);
    }
}
