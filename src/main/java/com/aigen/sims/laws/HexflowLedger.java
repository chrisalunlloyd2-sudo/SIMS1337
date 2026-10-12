package com.aigen.sims.laws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * GODHAND-3 (t_22bb44a7) — LAW 6: HexflowLedger.
 *
 * Append-only evidence chain for every pipeline stage transition. Each line:
 *
 *   {"n":<seq>,"ts":"<iso>","phase":"mine","event":"begin|progress|end|cancel",
 *    "prev":"<sha256-256hex>","hash":"<sha256-256hex>"}
 *
 * hash = SHA-256(prev.hash + "|" + this line's canonical payload minus hash)
 * — tamper-evident exactly like ops_health.py's evidence chain (line ~280)
 * and the kanban hexflow gate. Rotation: when the file passes
 * maxLines, rename with a numeric suffix and start fresh (chain restarts).
 *
 * Never throws: fs failures degrade to stderr warn (law 7), ledger writes
 * are additive only — never delete/rewrite existing lines.
 */
public final class HexflowLedger {

    public static final String BEGIN = "begin";
    public static final String PROGRESS = "progress";
    public static final String END = "end";
    public static final String CANCEL = "cancel";

    private final Path file;
    private final int maxLines;
    private final Object io = new Object();

    private long seq;
    private String prevHash;
    private long lineCount;

    public HexflowLedger(Path file) { this(file, 10_000); }

    public HexflowLedger(Path file, int maxLines) {
        this.file = file;
        this.maxLines = maxLines;
        restoreFromDisk();
    }

    /** Default location: AIGEN_SYS/mindpalace_memory/godhand/hexflow_ledger.jsonl */
    public static HexflowLedger defaultLedger() {
        String p = System.getProperty("com.aigen.sims.hexflowledger");
        if (p != null && !p.isEmpty()) {
            return new HexflowLedger(Paths.get(p));
        }
        String home = System.getProperty("user.home", ".");
        Path path = Paths.get(home, "AIGEN_SYS", "mindpalace_memory", "godhand", "hexflow_ledger.jsonl");
        return new HexflowLedger(path); // GODHAND-3 L6: appends only, never rewrites
    }

    /** Rebuild seq + chain from the existing file so appends across restarts continue it. */
    private void restoreFromDisk() {
        seq = 0; prevHash = ""; lineCount = 0;
        try {
            if (!Files.exists(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                lineCount++;
                String h = jsonField(line, "hash");
                String n = jsonField(line, "n");
                if (h != null && !h.isEmpty()) prevHash = h;
                if (n != null) {
                    try { seq = Long.parseLong(n); } catch (NumberFormatException ignored) { }
                }
            }
        } catch (Exception e) {
            System.err.println("[HEXFLOW] ledger restore degraded: " + e);
        }
    }

    /** Append one transition. Returns the new hash, or "" on fs failure (logged). */
    public String append(String phase, String event) {
        return append(phase, event, "");
    }

    public String append(String phase, String event, String detail) {
        if (event == null || CANCEL.equals(event) == false && BEGIN.equals(event) == false
                && PROGRESS.equals(event) == false && END.equals(event) == false) {
            // allow unknown events too — the ledger is the witness, not the police
        }
        String ts = java.time.Instant.now().toString();
        String canonical = "n=" + (seq + 1) + ";ts=" + ts + ";phase=" + phase + ";event=" + event
                + ";detail=" + (detail == null ? "" : detail) + ";prev=" + prevHash;
        String hash = sha256(canonical);
        String line = "{\"n\":" + (seq + 1) + ",\"ts\":\"" + ts + "\",\"phase\":\"" + js(phase)
                + "\",\"event\":\"" + js(event) + "\",\"detail\":\"" + js(detail == null ? "" : detail)
                + "\",\"prev\":\"" + prevHash + "\",\"hash\":\"" + hash + "\"}";
        try {
            synchronized (io) {
                if (lineCount > 0 && lineCount >= maxLines) rotate();
                Files.createDirectories(file.getParent());
                Files.write(file,
                        (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                lineCount++;
                seq++;
                prevHash = hash;
            }
        } catch (Exception e) {
            System.err.println("[HEXFLOW] ledger append failed (law 7 degrade): " + e);
            return "";
        }
        return hash;
    }

    /** Rotate: hexflow_ledger.jsonl → hexflow_ledger.jsonl.<N>; chain restarts empty. */
    private void rotate() {
        try {
            Path target = file.resolveSibling(stripExt(file.getFileName().toString())
                    + "." + System.currentTimeMillis() + ".jsonl");
            Files.move(file, target);
        } catch (IOException e) {
            System.err.println("[HEXFLOW] rotate failed; ledger keeps growing: " + e);
        }
        lineCount = 0;
    }

    /** Verify the whole chain: recompute hashes. Returns list of broken seq numbers (empty = intact). */
    public List<Long> verify() {
        List<Long> broken = new ArrayList<>();
        synchronized (io) {
            try {
                if (!Files.exists(file)) return broken;
                String prev = "";
                long expect = 0;
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String hash = jsonField(line, "hash");
                    String prevF = jsonField(line, "prev");
                    String n = jsonField(line, "n");
                    long nVal = n == null ? -1 : Long.parseLong(n);
                    String ts = jsonField(line, "ts");
                    String phase = jsonField(line, "phase");
                    String event = jsonField(line, "event");
                    String detail = jsonField(line, "detail");
                    String canonical = "n=" + nVal + ";ts=" + ts + ";phase=" + phase + ";event=" + event
                            + ";detail=" + (detail == null ? "" : detail) + ";prev=" + prevF;
                    if (!sha256(canonical).equals(hash)) {
                        broken.add(nVal);
                    } else if (nVal != ++expect && expect > 1) {
                        // sequence gap — chain still locally consistent, note it
                        broken.add(nVal);
                        expect = nVal;
                    }
                    prev = hash;
                }
            } catch (Exception e) {
                System.err.println("[HEXFLOW] verify degraded: " + e);
            }
        }
        return broken;
    }

    // ---- helpers ----

    static String sha256(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String js(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ");
    }

    /** Grab "field":"value" from a ledger line (values here are single-line, no escapes needed). */
    static String jsonField(String line, String field) {
        String key = "\"" + field + "\":";
        int i = line.indexOf(key);
        if (i < 0) return null;
        i += key.length();
        if (i < line.length() && line.charAt(i) == '"') {
            int end = line.indexOf('"', i + 1);
            return end < 0 ? null : line.substring(i + 1, end);
        }
        int end = line.indexOf(',', i);
        if (end < 0) end = line.indexOf('}', i);
        return end < 0 ? null : line.substring(i, end);
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    public Path file() { return file; }
    public long seq() { return seq; }
    public String lastHash() { return prevHash; }
}