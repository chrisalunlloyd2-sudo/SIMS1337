package com.aigen.sims.voting;

/**
 * HexCoord — Axial hex coordinate (Q, R, Z) with 4D time pulse phase.
 * Distance uses max(|dq|,|dr|,|dq+dr|) — standard axial hex metric.
 */
public class HexCoord {
    public final int q, r, z; // z = elevation layer

    public HexCoord(int q, int r, int z) { this.q = q; this.r = r; this.z = z; }
    public HexCoord(int q, int r) { this(q, r, 0); }

    public static HexCoord fromString(String s) {
        String[] parts = s.split(",");
        int q = Integer.parseInt(parts[0].trim());
        int r = Integer.parseInt(parts[1].trim());
        int z = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
        return new HexCoord(q, r, z);
    }

    /** Axial hex distance */
    public int distanceTo(HexCoord other) {
        int dq = q - other.q, dr = r - other.r;
        return Math.max(Math.max(Math.abs(dq), Math.abs(dr)), Math.abs(dq + dr));
    }

    /** Topological distance: hex distance weighted by Z elevation */
    public double topologicalDist(HexCoord other) {
        return Math.sqrt(distanceTo(other) * distanceTo(other) + (z - other.z) * (z - other.z));
    }

    /** 1-hop neighborhood (6 neighbors + self = 7) */
    public HexCoord[] oneHop() {
        int[][] dirs = {{1,0},{1,-1},{0,-1},{-1,0},{-1,1},{0,1}};
        HexCoord[] n = new HexCoord[7];
        n[0] = this;
        for (int i = 0; i < 6; i++) n[i+1] = new HexCoord(q + dirs[i][0], r + dirs[i][1], z);
        return n;
    }

    public String key() { return z == 0 ? q+","+r : q+","+r+","+z; }

    @Override public boolean equals(Object o) {
        if (!(o instanceof HexCoord h)) return false;
        return q == h.q && r == h.r && z == h.z;
    }
    @Override public int hashCode() { return 31 * (31 * q + r) + z; }
    @Override public String toString() { return "⬡(" + key() + ")"; }
}
