package com.aigen.sims;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SIMS1337 - OllamaRouter
 * Cellular Microphone Gating (CMG) orchestrator.
 *
 * H7/C.2 contract (plan 2026-10-02 sections C + E; scope note 2026-10-09):
 *   - At most ONE Ollama model is loaded or inferring at any moment. All callers
 *     share one worker thread over a bounded FIFO queue; overflow is explicitly
 *     rejected (never silently interleaved).
 *   - keep_alive policy: the hot model stays resident (default {@code 5m}); an
 *     explicit keep_alive:0 purge happens only on the cellular-mic swap or at
 *     JVM shutdown.
 *   - RAM guard (C.6 health probe): load is refused when free physical RAM is
 *     below {@code OLLAMA_ORCH_MIN_FREE_MB} (default 2048). Refusal is logged,
 *     the caller gets a structured REJECTED result.
 *   - Per-job timeout applies to EXECUTION (counted from dequeue), so queue
 *     wait is bounded by OLLAMA_ORCH_MAX_QUEUE instead of by timers. A timed
 *     out generate is logged, skipped, and the worker moves on.
 *   - Pacing: a configurable gap (default 1000 ms) runs after every job so the
 *     server settles between requests.
 *   - Instrumentation: every state change emits one structured line
 *     {@code [ORCH] ts=... event=... job=... model=... queue=... in_flight=0|1
 *     free_ram_mb=... <k=v pairs>} on stdout AND appended to
 *     {@code logs/orch_events.jsonl} for downstream consumers (t_6af8f408).
 *
 * Environment overrides:
 *   OLLAMA_ORCH_PACING_MS    (default 1000)   gap between jobs
 *   OLLAMA_ORCH_KEEP_ALIVE   (default "5m")   generate keep_alive
 *   OLLAMA_ORCH_TIMEOUT_MS   (default 120000) per-job execution timeout
 *   OLLAMA_ORCH_MIN_FREE_MB  (default 2048)   RAM guard floor
 *   OLLAMA_ORCH_MAX_QUEUE    (default 32)     FIFO capacity
 *   OLLAMA_ORCH_HTTP         (default "on")   /v1/run listener on/off
 *   OLLAMA_ORCH_HTTP_PORT    (default 11435)  listener port (loopback only)
 *
 * POST /v1/run  {"model":..,"prompt":..,"timeout_ms":..} -> 200 DONE,
 * 503 REJECTED (queue_full|ram_low), 504 TIMEOUT, 400 bad request.
 * GET /v1/status -> live queue/in_flight/active_model/free_ram snapshot.
 *
 * Compatibility: {@link #query(String, String)} keeps the legacy contract -
 * model text, or {@code NO_RESPONSE} on reject/timeout/error.
 */
public class OllamaRouter {

    // ---------------- status codes for structured results ----------------
    public static final int ST_DONE = 0;
    public static final int ST_REJECTED_QUEUE_FULL = 1;
    public static final int ST_REJECTED_RAM_LOW = 2;
    public static final int ST_TIMEOUT = 3;

    /** Structured run result: status + text (text null unless DONE). */
    public static final class RunResult {
        public final int status;
        public final String text;
        RunResult(int status, String text) { this.status = status; this.text = text; }
        public boolean isDone() { return status == ST_DONE; }
    }

    // ---------------- configuration ----------------
    private static final int PACING_MS       = envInt("OLLAMA_ORCH_PACING_MS", 1000);
    private static final String KEEP_ALIVE   = envStr("OLLAMA_ORCH_KEEP_ALIVE", "5m");
    private static final long JOB_TIMEOUT_MS = envInt("OLLAMA_ORCH_TIMEOUT_MS", 120000);
    private static final long MIN_FREE_MB    = envInt("OLLAMA_ORCH_MIN_FREE_MB", 2048);
    private static final int MAX_QUEUE       = envInt("OLLAMA_ORCH_MAX_QUEUE", 32);
    private static final boolean HTTP_ON     = envStr("OLLAMA_ORCH_HTTP", "on").equalsIgnoreCase("on");
    private static final int HTTP_PORT       = envInt("OLLAMA_ORCH_HTTP_PORT", 11435);
    private static final String OLLAMA_URL   = envStr("OLLAMA_ORCH_URL", "http://localhost:11434/api/generate");
    private static final int NUM_PREDICT     = envInt("OLLAMA_ORCH_NUM_PREDICT", 128);
    public  static final String NO_RESPONSE  = "NO_RESPONSE";

    // ---------------- single-flight state ----------------
    private static final LinkedBlockingQueue<Job> QUEUE = new LinkedBlockingQueue<>(MAX_QUEUE);
    private static final AtomicLong JOB_SEQ = new AtomicLong(0);
    private static final AtomicReference<Job> CURRENT = new AtomicReference<>();
    /** 0 or 1 at all times - the single-flight invariant surfaced in logs. */
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger(0);
    private static final AtomicBoolean WORKER_STARTED = new AtomicBoolean(false);
    private static volatile String activeModel = null;
    private static volatile Path EVENT_FILE;
    private static volatile boolean HEADER_WRITTEN = false;

    // ---------------- public API ----------------
    /** Legacy entry point: serialized, blocking; NO_RESPONSE on non-DONE. */
    public String query(String model, String prompt) {
        RunResult r = run(model, prompt, JOB_TIMEOUT_MS);
        return r.text == null ? NO_RESPONSE : r.text;
    }

    /**
     * Serialized, blocking query with explicit per-job EXECUTION timeout budget.
     * Blocks up to queueWait + timeout; returns structured result, never throws.
     * First call lazily boots the worker + /v1/run listener (idempotent).
     */
    public RunResult run(String model, String prompt, long timeoutMs) {
        if (model == null || model.trim().isEmpty() || prompt == null) {
            return new RunResult(ST_REJECTED_QUEUE_FULL, null); // malformed = not runnable
        }
        ensureStarted();
        Job job = new Job(model.trim(), prompt, Math.max(1000, timeoutMs));
        if (QUEUE.offer(job)) {
            orchEvent(job.id, "queued", model, "depth_at_submit=" + QUEUE.size());
            return await(job);
        }
        orchEvent(job.id, "rejected", model, "reason=queue_full capacity=" + MAX_QUEUE);
        return new RunResult(ST_REJECTED_QUEUE_FULL, null);
    }

    /** RAM-guard-only check for callers that want a load decision. */
    public static boolean canLoadModelNow() {
        return freeRamMb() >= MIN_FREE_MB;
    }

    public static String getActiveModel() { return activeModel; }

    private static RunResult await(Job job) {
        long capMs = job.timeoutMs + (long) MAX_QUEUE * Math.max(1000, JOB_TIMEOUT_MS) + 5000L;
        try {
            return job.future.get(capMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            job.future.complete(new RunResult(ST_TIMEOUT, null));
            orchEvent(job.id, "timeout", job.model, "phase=await cap_ms=" + capMs);
            return new RunResult(ST_TIMEOUT, null);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return new RunResult(ST_TIMEOUT, null);
        } catch (ExecutionException | RuntimeException e) {
            return new RunResult(ST_TIMEOUT, null);
        }
    }

    // ---------------- structured event log ----------------
    /** Emit with the executing job id taken from CURRENT (0 = none executing). */
    private static void orchEvent(String event, String model, String detail) {
        orchEvent(0L, event, model, detail);
    }

    /** jobOverride > 0 pins the job id explicitly (queued / await-timeout events). */
    private static void orchEvent(long jobOverride, String event, String model, String detail) {
        Job cur = CURRENT.get();
        long jobId = jobOverride > 0 ? jobOverride : (cur != null ? cur.id : 0L);
        String line = String.format(
            "[ORCH] ts=%s event=%s job=%d model=%s queue=%d in_flight=%d free_ram_mb=%d%s",
            Instant.ofEpochMilli(System.currentTimeMillis()), event,
            jobId, model == null ? "-" : model,
            QUEUE.size(), IN_FLIGHT.get(), freeRamMb(),
            (detail == null || detail.isEmpty()) ? "" : " " + detail);
        System.out.println(line);
        appendEventFile(line);
    }

    private static synchronized void appendEventFile(String line) {
        try {
            if (EVENT_FILE == null) {
                Path logs = Paths.get("logs");
                if (!Files.isDirectory(logs)) Files.createDirectories(logs);
                EVENT_FILE = logs.resolve("orch_events.jsonl");
                System.out.println("[ORCH] event_file=" + EVENT_FILE.toAbsolutePath());
            }
            if (!HEADER_WRITTEN) {
                writeLine("# v1 format: [ORCH] ts=<iso8601> event=<queued|rejected|load_start|"
                        + "infer_done|timeout|http_up|http_failed|shutdown> job=<id> model=<name>"
                        + " queue=<depth> in_flight=<0|1> free_ram_mb=<n> <k=v pairs>");
                HEADER_WRITTEN = true;
            }
            writeLine(line);
        } catch (Throwable t) {
            // best-effort telemetry only - never break the game over logging;
            // stdout [ORCH] lines above are the primary stream and still emit.
        }
    }

    private static void writeLine(String line) {
        try (BufferedWriter w = Files.newBufferedWriter(EVENT_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (Throwable ignored) {
        }
    }

    // ---------------- single worker: drain loop ----------------
    /** Idempotent boot: starts the worker thread, pacing, HTTP listener and
     *  shutdown hook. Call once at app start so /v1/run is live before the
     *  first model call arrives. */
    public static void ensureStarted() {
        if (!WORKER_STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(() -> {
            while (true) {
                Job job = null;
                try {
                    job = QUEUE.take();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return; // daemon thread dies with the JVM
                }
                try {
                    runOne(job);
                    Thread.sleep(PACING_MS); // paced queue: settle gap between jobs
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    job.completeIfPending(new RunResult(ST_TIMEOUT, null));
                    return;
                } catch (Throwable t) {
                    job.completeIfPending(new RunResult(ST_TIMEOUT, null));
                    orchEvent("timeout", job.model, "phase=worker exception=" + t.getClass().getSimpleName());
                }
            }
        }, "ollama-orch-worker");
        worker.setDaemon(true);
        worker.start();
        if (HTTP_ON) startHttpServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            String m = activeModel;
            if (m != null) {
                orchEvent("shutdown", m, null);
                purgeModel(m);
            }
        }, "ollama-orch-shutdown"));
    }

    /** Executes exactly one job end to end: RAM guard, mic swap, generate, log. */
    private static void runOne(Job job) {
        CURRENT.set(job);
        IN_FLIGHT.set(1);
        job.beginExecution();
        try {
            long freeMb = freeRamMb();
            if (freeMb < MIN_FREE_MB) {
                orchEvent("rejected", job.model, "reason=ram_low free_ram_mb=" + freeMb
                        + " min_free_mb=" + MIN_FREE_MB);
                job.completeIfPending(new RunResult(ST_REJECTED_RAM_LOW, null));
                return;
            }

            String prev = activeModel;
            long t0 = System.currentTimeMillis();
            if (prev != null && !prev.equals(job.model)) {
                long p0 = System.currentTimeMillis();
                purgeModel(prev);
                orchEvent("load_start", job.model, "phase=swap prev_model=" + prev
                        + " purge_ms=" + (System.currentTimeMillis() - p0));
            } else {
                orchEvent("load_start", job.model,
                        "phase=" + (prev == null ? "cold" : "same_model_kept"));
            }

            String text = generate(job.model, job.prompt, job.remainingMs());
            if (text != null) {
                activeModel = job.model;
                orchEvent("infer_done", job.model, "total_ms=" + (System.currentTimeMillis() - t0)
                        + " chars=" + text.length());
                job.completeIfPending(new RunResult(ST_DONE, text));
            } else {
                orchEvent("timeout", job.model, "phase=generate total_ms="
                        + (System.currentTimeMillis() - t0) + " timeout_ms=" + job.timeoutMs);
                job.completeIfPending(new RunResult(ST_TIMEOUT, null));
            }
        } finally {
            IN_FLIGHT.set(0);
            CURRENT.set(null);
        }
    }

    /** Explicitly evicts the resident model via keep_alive:0 (cellular-mic handoff). */
    private static void purgeModel(String modelName) {
        System.out.println("[CMG GATING] Purging model from memory: " + modelName);
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(OLLAMA_URL).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(30000); // unload itself is a server op; give it room
            byte[] payload = ("{\"model\":\"" + modelName + "\", \"prompt\": \"\", "
                    + "\"keep_alive\":0, \"stream\":false}").getBytes(StandardCharsets.UTF_8);
            OutputStream os = con.getOutputStream();
            os.write(payload);
            os.flush();
            os.close();
            int code = con.getResponseCode();
            System.out.println("[CMG GATING] Purge result code for " + modelName + ": " + code);
        } catch (Exception e) {
            System.out.println("[CMG GATING ERROR] Failed to purge model: " + e.getMessage());
        }
    }

    /**
     * Blocking synchronous generate on the caller (the single worker) thread.
     * Returns model text, or null on HTTP error / timeout / offline - the
     * null return is what distinguishes a non-DONE job. Reading runs on the
     * worker thread only, so a timed-out job can never overlap the next load.
     */
    private static String generate(String model, String prompt, long budgetMs) {
        try {
            String safePrompt = prompt.replace("\"", "\\\"").replace("\n", " ");
            String json = "{\"model\":\"" + model + "\", \"prompt\":\"" + safePrompt
                    + "\", \"stream\":false, \"keep_alive\":\"" + KEEP_ALIVE + "\""
                    + ", \"options\":{\"num_predict\":" + NUM_PREDICT + "}}";
            HttpURLConnection con = (HttpURLConnection) new URL(OLLAMA_URL).openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout((int) Math.min(15000, Math.max(1000, budgetMs / 2)));
            con.setReadTimeout((int) Math.max(1000, Math.min(budgetMs, Integer.MAX_VALUE)));
            OutputStream os = con.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            if (con.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    String body = response.toString();
                    int idx = body.indexOf("\"response\":\"");
                    if (idx != -1) {
                        int start = idx + 12;
                        int end = start;
                        while (end < body.length()) {
                            if (body.charAt(end) == '"' && body.charAt(end - 1) != '\\') break;
                            end++;
                        }
                        return body.substring(start, Math.min(end, body.length()))
                                .replace("\\n", " ").replace("\\\"", "\"").trim();
                    }
                    return body;
                }
            } else {
                System.out.println("[OLLAMA ERROR] HTTP " + con.getResponseCode() + " for " + model);
            }
        } catch (SocketTimeoutException ste) {
            System.out.println("[OLLAMA ERROR] Job budget exceeded: " + model);
        } catch (Exception e) {
            System.out.println("[OLLAMA ERROR] Timeout or offline: " + model + " - " + e.getMessage());
        }
        return null;
    }

    // ---------------- /v1/run + /v1/status (loopback only) ----------------
    private static void startHttpServer() {
        try {
            com.sun.net.httpserver.HttpServer server =
                    com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", HTTP_PORT), 3);
            server.createContext("/v1/run", exchange -> {
                try {
                    String body = readBody(exchange);
                    String model = extractJsonField(body, "model");
                    String prompt = extractJsonField(body, "prompt");
                    String tmoField = extractJsonField(body, "timeout_ms");
                    long timeoutMs = JOB_TIMEOUT_MS;
                    if (tmoField != null) {
                        try { timeoutMs = Long.parseLong(tmoField.trim()); }
                        catch (NumberFormatException ignored) {}
                    }
                    if (model == null || model.trim().isEmpty() || prompt == null) {
                        sendJson(exchange, 400, "{\"status\":\"ERROR\",\"reason\":\"model_and_prompt_required\"}");
                        return;
                    }
                    RunResult r = new OllamaRouter().run(model.trim(), prompt, timeoutMs);
                    switch (r.status) {
                        case ST_DONE:
                            sendJson(exchange, 200, "{\"status\":\"DONE\",\"model\":\"" + model.trim()
                                    + "\",\"text\":" + jsonQuote(r.text) + "}"); break;
                        case ST_REJECTED_QUEUE_FULL:
                            sendJson(exchange, 503, "{\"status\":\"REJECTED\",\"reason\":\"queue_full\"}"); break;
                        case ST_REJECTED_RAM_LOW:
                            sendJson(exchange, 503, "{\"status\":\"REJECTED\",\"reason\":\"ram_low\"}"); break;
                        default:
                            sendJson(exchange, 504, "{\"status\":\"TIMEOUT\",\"timeout_ms\":" + timeoutMs + "}"); break;
                    }
                } catch (Exception e) {
                    sendJson(exchange, 500, "{\"status\":\"ERROR\",\"reason\":\""
                            + e.getClass().getSimpleName() + "\"}");
                }
            });
            server.createContext("/v1/status", exchange -> sendJson(exchange, 200,
                    "{\"status\":\"ACTIVE\",\"queue\":" + QUEUE.size()
                    + ",\"in_flight\":" + IN_FLIGHT.get()
                    + ",\"active_model\":" + (activeModel == null ? "null" : "\"" + activeModel + "\"")
                    + ",\"free_ram_mb\":" + freeRamMb() + "}"));
            ExecutorService httpPool = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "ollama-orch-httpd");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(httpPool);
            server.start();
            orchEvent("http_up", null, "port=" + HTTP_PORT + " bind=127.0.0.1");
        } catch (Exception e) {
            // port already bound (second app instance) - local API stays usable, never crash
            orchEvent("http_failed", null, "port=" + HTTP_PORT + " error=" + e.getClass().getSimpleName());
        }
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static void sendJson(com.sun.net.httpserver.HttpExchange exchange, int status, String json) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        } catch (Exception ignored) {
        }
    }

    /** Minimal JSON string quoting for /v1/run responses. */
    private static String jsonQuote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    /**
     * Extracts a quoted-string or bare-number field value from a small JSON
     * body without pulling in a parser dependency. Handles escaped chars.
     */
    static String extractJsonField(String json, String field) {
        if (json == null) return null;
        String needle = "\"" + field + "\":";
        int k = json.indexOf(needle);
        if (k < 0) return null;
        k += needle.length();
        while (k < json.length() && json.charAt(k) == ' ') k++;
        if (k >= json.length()) return null;
        if (json.charAt(k) != '"') {
            int end = k;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            return end > k ? json.substring(k, end) : null;
        }
        k++;
        StringBuilder val = new StringBuilder();
        while (k < json.length()) {
            char c = json.charAt(k);
            if (c == '\\' && k + 1 < json.length()) {
                char n = json.charAt(k + 1);
                if (n == 'n') val.append('\n');
                else if (n == 'r') val.append('\r');
                else if (n == 't') val.append('\t');
                else val.append(n);
                k += 2;
                continue;
            }
            if (c == '"') break;
            val.append(c);
            k++;
        }
        return val.toString();
    }

    // ---------------- helpers ----------------
    private static long freeRamMb() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long free = os.getFreePhysicalMemorySize();
            return free <= 0 ? Long.MAX_VALUE / 2 : free / (1024 * 1024);
        } catch (Throwable t) {
            return Long.MAX_VALUE / 2; // guard disabled if probe unavailable
        }
    }

    private static String envStr(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    private static int envInt(String key, int def) {
        String v = System.getenv(key);
        if (v == null || v.trim().isEmpty()) return def;
        try {
            return (int) Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---------------- job record ----------------
    private static final class Job {
        final long id;
        final String model;
        final String prompt;
        final long timeoutMs;
        private final CompletableFuture<RunResult> future = new CompletableFuture<>();
        private volatile boolean completed = false;
        private long deadlineAt;

        Job(String model, String prompt, long timeoutMs) {
            this.id = JOB_SEQ.incrementAndGet();
            this.model = model;
            this.prompt = prompt;
            this.timeoutMs = timeoutMs;
        }

        /** Deadline is set at dequeue: timeout budgets EXECUTION, not queue wait. */
        void beginExecution() { deadlineAt = System.currentTimeMillis() + timeoutMs; }
        long remainingMs() { return Math.max(1000, deadlineAt - System.currentTimeMillis()); }

        void completeIfPending(RunResult r) {
            if (!completed) {
                completed = true;
                future.complete(r);
            }
        }
    }
}