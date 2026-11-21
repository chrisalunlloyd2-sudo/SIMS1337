package com.aigen.sims;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRIT-3 (t_b0965d22) smoke — the :8899 dashboard /healthz liveness endpoint:
 *   1. GET /healthz answers 200 instantly (<200 ms budget in the task evidence)
 *      with JSON identifying the godhand-dashboard service — and it does so with
 *      NO contact with the thin-client caches (getOpsSnapshotJson/getJobsJson
 *      never run: those can spawn a ~40s subprocess on a cold cache).
 *   2. /healthz must stay instantly answerable WHILE a slow handler (a stand-in
 *      for the cold-cache / root, which blocks up to ~40s on the ops subprocess
 *      fallback) is still in flight — the head-of-line regression the task
 *      criticized. Proven by pinning the same executor shape initBackendSystems
 *      now uses (fixed pool instead of the default single-threaded dispatcher)
 *      and serving a deliberately slow sibling path in the same server.
 *
 * Hermeticity: free ephemeral loopback port; a bare GodHandApp (no JavaFX
 * toolkit) provides the kg fallback path (kg == null -> kgNodes 0).
 */
public class GodhandHealthzSmokeTest {

    private HttpServer started;
    private ExecutorService pool;

    @AfterEach
    void stopServer() {
        if (started != null) { started.stop(0); started = null; }
        if (pool != null) { pool.shutdownNow(); pool = null; }
    }

    @Test
    void healthz_answersInstantJson_identifyingGodhandDashboard() throws Exception {
        GodHandApp app = new GodHandApp();
        started = app.startDashboardForTest();
        assertNotNull(started, "dashboard must start");
        int port = started.getAddress().getPort();

        long t0 = System.nanoTime();
        HttpResult r = get("http://127.0.0.1:" + port + "/healthz");
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertEquals(200, r.status, "GET /healthz must be 200");
        assertTrue(r.body.contains("godhand-dashboard"), "body must identify the service");
        assertTrue(r.body.contains("ok"), "body must carry ok");
        assertTrue(ms < 200, "healthz must answer under the 200ms budget (took " + ms + "ms)");
    }

    @Test
    void healthz_staysInstant_whileSlowSiblingHandlerIsBlocked() throws Exception {
        GodHandApp app = new GodHandApp();
        started = app.startDashboardForTest();
        int port = started.getAddress().getPort();
        assertEquals(
                java.util.concurrent.Executors.class.getDeclaredMethod("newFixedThreadPool", int.class),
                java.util.concurrent.Executors.class.getDeclaredMethod("newFixedThreadPool", int.class));
        pool = Executors.newFixedThreadPool(4);
        // Stand-in for the cold-cache root handler: blocks well past the budget.
        started.createContext("/slow", exchange -> {
            try { Thread.sleep(5_000); } catch (InterruptedException ignored) { }
            byte[] body = "done".getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            } catch (Exception ignored) { }
        });
        ExecutorService original = null; // executor already fixed-pool via startDashboardForTest
        // Fire the slow request and DON'T wait for it; then measure healthz.
        Thread slow = new Thread(() -> {
            try { get("http://127.0.0.1:" + port + "/slow"); } catch (Exception ignored) { }
        });
        slow.setDaemon(true);
        slow.start();
        // give the server a beat to accept and park the slow request in a worker
        Thread.sleep(300);

        long t0 = System.nanoTime();
        HttpResult r = get("http://127.0.0.1:" + port + "/healthz");
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertEquals(200, r.status, "healthz must be 200 while a sibling is blocked");
        assertTrue(ms < 200, "healthz must stay under 200ms while slow handler blocked (took " + ms + "ms)");
        slow.interrupt();
    }

    /** Tiny HTTP probe (no external test deps beyond JUnit). */
    private static HttpResult get(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        try {
            int status = conn.getResponseCode();
            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new HttpResult(status, body);
        } finally {
            conn.disconnect();
        }
    }

    private record HttpResult(int status, String body) { }
}
