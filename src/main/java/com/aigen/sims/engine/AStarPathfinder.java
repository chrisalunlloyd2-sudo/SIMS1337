package com.aigen.sims.engine;

import java.util.*;

/**
 * AStarPathfinder — generic A* on a grid with 4/8-direction movement.
 * Used for agent navigation on the hex/desktop map.
 * Costs: 1 per cardinal step, 1.414 per diagonal (if diagonals enabled).
 */
public class AStarPathfinder {

    public static class Node implements Comparable<Node> {
        public final int x, y;
        public double g, f;
        public Node parent;
        public Node(int x, int y) { this.x = x; this.y = y; g = f = Double.MAX_VALUE; }
        @Override public int compareTo(Node o) { return Double.compare(f, o.f); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node n = (Node) o; return n.x == x && n.y == y;
        }
        @Override public int hashCode() { return x * 73856093 ^ y * 19349663; }
    }

    private final int width, height;
    private final boolean[][] blocked; // blocked[y][x]
    private final boolean diagonals;

    public AStarPathfinder(int width, int height, boolean diagonals) {
        this.width = width; this.height = height; this.diagonals = diagonals;
        this.blocked = new boolean[height][width];
    }

    public void setBlocked(int x, int y, boolean b) {
        if (inBounds(x, y)) blocked[y][x] = b;
    }

    private boolean inBounds(int x, int y) { return x >= 0 && y >= 0 && x < width && y < height; }

    private double heuristic(int ax, int ay, int bx, int by) {
        double dx = Math.abs(ax - bx), dy = Math.abs(ay - by);
        return diagonals ? Math.max(dx, dy) : dx + dy;
    }

    private List<Node> neighbors(Node n) {
        List<Node> out = new ArrayList<>();
        int[][] dirs = diagonals
            ? new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}
            : new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = n.x + d[0], ny = n.y + d[1];
            if (inBounds(nx, ny) && !blocked[ny][nx]) out.add(new Node(nx, ny));
        }
        return out;
    }

    /** Returns path as list of (x,y) from start to goal, or empty list if none. */
    public List<int[]> findPath(int sx, int sy, int gx, int gy) {
        if (!inBounds(sx, sy) || !inBounds(gx, gy) || blocked[sy][sx] || blocked[gy][gx])
            return Collections.emptyList();
        PriorityQueue<Node> open = new PriorityQueue<>();
        Set<Node> closed = new HashSet<>();
        Node start = new Node(sx, sy);
        start.g = 0; start.f = heuristic(sx, sy, gx, gy);
        open.add(start);
        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (cur.x == gx && cur.y == gy) { return reconstruct(cur); }
            closed.add(cur);
            for (Node nb : neighbors(cur)) {
                if (closed.contains(nb)) continue;
                double step = (nb.x != cur.x && nb.y != cur.y) ? 1.414 : 1.0;
                double tentative = cur.g + step;
                if (tentative < nb.g) {
                    nb.g = tentative;
                    nb.f = tentative + heuristic(nb.x, nb.y, gx, gy);
                    nb.parent = cur;
                    open.add(nb);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<int[]> reconstruct(Node goal) {
        List<int[]> path = new ArrayList<>();
        for (Node n = goal; n != null; n = n.parent) path.add(0, new int[]{n.x, n.y});
        return path;
    }
}
