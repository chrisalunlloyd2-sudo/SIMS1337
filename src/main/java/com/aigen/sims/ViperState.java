package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Feeds the hexeract REAL system state instead of random numbers.
 *
 * <p>Before this, {@code initHexeract()} filled the board with
 * {@code densities[i] = 0.3 + rand.nextDouble() * 0.7} — a beautiful visualisation of noise. It
 * could not tell you a cell was slow, or empty, or failing, because it had never heard of the
 * things it was drawing. That is the same failure this system kept producing elsewhere: something
 * that <em>looks</em> like live state and isn't.
 *
 * <p>State arrives through {@code gui_state.py}, the single read-only contract, for the same
 * reason the Java suite uses it: every invariant lives in Python, and a second client with its own
 * database connection would sit beside all of them.
 *
 * <p><b>Never on the render thread and never per frame.</b> The read takes ~1.7 s; doing it inline
 * would stall the canvas. Refresh runs on a daemon thread every 15 s and writes into the arrays the
 * renderer already reads, so the draw path is unchanged and cannot block.
 */
public final class ViperState {

    private static final String PYTHON = "C:\\Python314\\python.exe";
    private static final String SCRIPT = "C:\\Viper\\scripts\\gui_state.py";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Guards against overlapping refreshes if one read runs long. */
    private static volatile boolean inFlight = false;

    /** True once real state has landed at least once — until then the board is honestly unknown. */
    public static volatile boolean live = false;
    public static volatile String status = "waiting for first state";
    public static volatile int occupied = 0;

    private ViperState() {
    }

    /**
     * Fetch occupancy off-thread and write it into the renderer's arrays.
     *
     * @param densities per-vertex intensity: occupied cells scale with their measured score
     * @param flows     per-vertex activity: inner shells beat faster, so they flow harder
     */
    public static void refreshAsync(double[] densities, double[] flows) {
        if (inFlight) {
            return;
        }
        inFlight = true;
        Thread t = new Thread(() -> {
            try {
                apply(read(), densities, flows);
            } catch (Exception e) {
                status = "state unavailable: " + e.getClass().getSimpleName();
                // deliberately NOT setting live=false: the last good reading stays on screen and
                // the caption says it is stale. Blanking the board on one failed read would hide
                // more than it reveals.
            } finally {
                inFlight = false;
            }
        }, "viper-state");
        t.setDaemon(true);
        t.start();
    }

    private static JsonNode read() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(PYTHON, SCRIPT, "board");
        Process p = pb.start();
        byte[] out = readAll(p.getInputStream());
        if (!p.waitFor(120, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("gui_state timed out");
        }
        if (out.length == 0) {
            throw new IllegalStateException("gui_state returned nothing");
        }
        return MAPPER.readTree(new String(out, StandardCharsets.UTF_8))
                .path("sections").path("board");
    }

    private static void apply(JsonNode board, double[] densities, double[] flows) {
        if (board == null || board.isMissingNode() || board.has("error")) {
            status = "board section unavailable";
            return;
        }
        JsonNode pts = board.path("points");
        int occ = 0;
        Iterator<Map.Entry<String, JsonNode>> it = pts.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> en = it.next();
            int v;
            try {
                v = Integer.parseInt(en.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            if (v < 0 || v >= densities.length) {
                continue;
            }
            JsonNode p = en.getValue();
            boolean filled = !p.path("occupant").isNull() && !p.path("occupant").isMissingNode();
            int shell = Math.max(0, Math.min(6, p.path("shell").asInt()));

            if (filled) {
                occ++;
                // score is a measured reliability in [0,1]; floor it so a live-but-poor cell is
                // still visibly brighter than an empty one
                double score = p.path("score").isNumber() ? p.path("score").asDouble() : 0.5;
                densities[v] = 0.45 + 0.55 * Math.max(0.0, Math.min(1.0, score));
            } else {
                densities[v] = 0.10;             // empty cells stay dim, not dark: they exist
            }
            // inner shells fire at x1, outer at x64 — flow mirrors that, so the centre visibly
            // moves faster than the rim, which is what the beat schedule actually does
            flows[v] = (filled ? 1.0 : 0.35) / (1 << Math.min(shell, 4));
        }
        occupied = occ;
        live = true;
        status = occ + "/" + pts.size() + " cells occupied (live)";
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
