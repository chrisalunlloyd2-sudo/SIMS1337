package com.aigen.sims.phase1;

/**
 * HexCoord — Axial hex coordinate (Q, R) with distance and neighbor math.
 * From hex-fow gist: hex-hex.go → Java port.
 */
public class HexCoord {
    public final int q, r;

    public HexCoord(int q, int r) { this.q = q; this.r = r; }

    public static HexCoord fromString(String s) {
        String[] parts = s.split(",");
        return new HexCoord(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    /** Axial hex distance: max(|dq|, |dr|, |dq+dr|) */
    public int distanceTo(HexCoord other) {
        int dq = this.q - other.q;
        int dr = this.r - other.r;
        return Math.max(Math.max(Math.abs(dq), Math.abs(dr)), Math.abs(dq + dr));
    }

    /** All 6 neighbors in axial coordinates */
    public HexCoord[] neighbors() {
        int[][] dirs = {{1,0},{1,-1},{0,-1},{-1,0},{-1,1},{0,1}};
        HexCoord[] n = new HexCoord[6];
        for (int i = 0; i < 6; i++) n[i] = new HexCoord(q + dirs[i][0], r + dirs[i][1]);
        return n;
    }

    /** 1-hop neighborhood: this cell + 6 neighbors = 7 hexes */
    public HexCoord[] oneHop() {
        HexCoord[] n = neighbors();
        HexCoord[] hop = new HexCoord[7];
        hop[0] = this;
        System.arraycopy(n, 0, hop, 1, 6);
        return hop;
    }

    public String key() { return q + "," + r; }

    @Override public boolean equals(Object o) {
        if (!(o instanceof HexCoord h)) return false;
        return q == h.q && r == h.r;
    }
    @Override public int hashCode() { return 31 * q + r; }
    @Override public String toString() { return "⬡(" + q + "," + r + ")"; }
}
