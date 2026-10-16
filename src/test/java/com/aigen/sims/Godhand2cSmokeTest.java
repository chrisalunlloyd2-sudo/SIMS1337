package com.aigen.sims;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GODHAND-2C (t_63a51063) smoke — the public phone UI port:
 *   1. GUI mode: startPublicPhoneUi(true) binds 0.0.0.0 (any interface), GET "/" is
 *      200 + HTML identifying SIMS1337, and every other path — including API-style
 *      paths like /api/status — is 404 (READ-ONLY by construction).
 *   2. Headless: startPublicPhoneUi(false) must return null and NOTHING must be
 *      listening on the chosen port (the loopback-only contract).
 *
 * Hermeticity: the test picks a free ephemeral port and pins it through the
 * -Dcom.aigen.sims.publicPort seam (production default stays 1338); the server
 * is stopped between tests. No JavaFX toolkit is touched (startPublicPhoneUi is
 * toolkit-free; state fallbacks render even with kg/agents unset).
 */
public class Godhand2cSmokeTest {

    private static String priorPortProp;
    private static int port;
    private HttpServer started;

    @BeforeAll
    static void pickFreePortAndPinIt() throws Exception {
        priorPortProp = System.getProperty("com.aigen.sims.publicPort");
        try (ServerSocket probe = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = probe.getLocalPort();
        }
        System.setProperty("com.aigen.sims.publicPort", String.valueOf(port));
    }

    @AfterAll
    static void restorePortProp() {
        if (priorPortProp == null) System.clearProperty("com.aigen.sims.publicPort");
        else System.setProperty("com.aigen.sims.publicPort", priorPortProp);
    }

    @AfterEach
    void stopServer() {
        if (started != null) { started.stop(0); started = null; }
    }

    @Test
    void guiMode_opensPublicUi_rootIsSimsPage_andEverythingElseIs404() throws Exception {
        GodHandApp app = new GodHandApp();
        started = app.startPublicPhoneUi(true);
        assertNotNull(started, "GUI mode must start the public phone UI");

        // "/" is the ONLY served path: 200 + HTML that identifies SIMS1337.
        HttpResult root = get("http://127.0.0.1:" + port + "/");
        assertEquals(200, root.status, "GET / must be 200");
        assertTrue(root.body.contains("SIMS1337"), "page must identify SIMS1337");
        assertTrue(root.body.contains("<html"), "root must be HTML");
        assertFalse(root.body.contains("org.json"), "page must not leak internals");

        // READ-ONLY: every other path 404s — no API endpoints on the public port.
        assertEquals(404, get("http://127.0.0.1:" + port + "/api/status").status,
                "API-style paths must 404 on the public UI");
        assertEquals(404, get("http://127.0.0.1:" + port + "/index.html").status,
                "asset paths must 404 on the public UI");
    }

    @Test
    void headless_neverOpensPublicUi() throws Exception {
        GodHandApp app = new GodHandApp();
        HttpServer server = app.startPublicPhoneUi(false);
        assertNull(server, "headless mode must skip the public phone UI");
        // Nothing may listen on the chosen port in headless mode: a fresh connect
        // must be refused (the port is still free).
        assertThrows(java.io.IOException.class,
                () -> get("http://127.0.0.1:" + port + "/"),
                "headless must NOT serve the public UI (connection refused expected)");
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
        } catch (java.net.ConnectException ce) {
            throw ce; // caller expects refusal semantics
        } finally {
            conn.disconnect();
        }
    }

    private record HttpResult(int status, String body) { }
}