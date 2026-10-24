package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.aigen.sims.laws.AdmissionControl;
import com.aigen.sims.laws.LawEnforcer;
import com.aigen.sims.laws.ModelPolicy;

/**
 * SIMS1337 - ModelBus
 *
 * Cellular-mic single-flight choke point for ALL model inference in the JVM
 * (perf EPIC H7 + C.2 + C.4, task t_4a3756ed). No code path in this repo may
 * call Ollama's /api/generate except through {@link #ask}:
 *
 *  - ONE worker thread inside ModelBus; jobs are a bounded FIFO (cap 16).
 *    Overflow = {@link #BUSY} answer after a bounded grace wait (documented,
 *    never silent, never concurrent).
 *  - UNIFY-1: the orchestrator (127.0.0.1:11435) is the ONLY path to Ollama
 *    for this JVM. Every job is DELEGATED to it; nothing here touches :11434.
 *  - If the gateway is down or a delegate call fails, jobs complete instantly
 *    with {@link #BUSY} (documented, never silent) — no direct-Ollama fallback.
 *    Gateway health is probed lazily via /health, cached 30s.
 *  - Instrumentation: one [MODELBUS] log line per state transition
 *    (ENQUEUE, DELEGATE, DONE, TIMEOUT, BUSY) so inference
 *    concurrency is auditable from app logs alone.
 *
 * Thread-safety: all mutable state is confined to the single worker thread or
 * per-job latches; the shared queue is LinkedBlockingQueue.
 */
public final class ModelBus {

    private static final Logger log = LoggerFactory.getLogger(ModelBus.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String BUSY = "NO_RESPONSE";
    /** Orchestrator service endpoint (standalone scripts/ollama_orchestrator.py). */
    public static final String ORCH_URL = "http://127.0.0.1:11435";

    private static final int QUEUE_CAP = 16;
    private static final long ENQUEUE_GRACE_S = 10;   // bounded wait for queue slot
    private static final long DELEGATE_TIMEOUT_S = 320;
    private static final long PROBE_CACHE_MS = 30_000;

    private static final LinkedBlockingQueue<Job> queue = new LinkedBlockingQueue<>(QUEUE_CAP);
    private static volatile boolean orchUp = true;     // optimistic; probed before first use
    private static volatile long lastProbeMs = 0L;
    private static volatile String activeModel = null; // informational only

    private static final class Job {
        final String model;
        final String prompt;
        final int numPredict;
        final boolean embed; // GODHAND-3: embed jobs are L4 residency-exempt
        final CountDownLatch done = new CountDownLatch(1);
        volatile String result = BUSY;
        Job(String model, String prompt, int numPredict) {
            this.model = model; this.prompt = prompt; this.numPredict = numPredict;
            this.embed = model != null && (model.contains("embed") || model.contains("nomic"));
        }
    }

    static {
        ThreadFactory tf = new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger();
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "modelbus-single-flight-" + n.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        Thread w = tf.newThread(ModelBus::workerLoop);
        w.start();
        log.info("[MODELBUS] single-flight worker started (queue cap {}, orchestrator {})",
                QUEUE_CAP, ORCH_URL);
    }

    private ModelBus() { /* static-only */ }

    /**
     * THE only way to run model inference from this JVM. Blocking; serializes
     * behind the single flight; never throws. Returns the model text, or
     * {@link #BUSY} on queue-full / timeout / gateway-down / transport failure.
     */
    public static String ask(String model, String prompt) {
        return ask(model, prompt, 220);
    }

    /** num_predict-aware variant; numPredict &lt;= 0 falls back to 220. */
    public static String ask(String model, String prompt, int numPredict) {
        if (model == null || model.isEmpty()) {
            log.warn("[MODELBUS] reject: null/empty model");
            return BUSY;
        }
        // GODHAND-3 LAW 5e: cheap policy gate at enqueue (banned/unknown deny instantly,
        // no queue slot consumed). Full resource admission still runs at dispatch (worker).
        ModelPolicy.Verdict policy = ModelPolicy.checkModel(model);
        if (!policy.allowed) {
            log.warn("[MODELBUS] LAW5 policy deny {}: {}", model, policy.reason);
            return BUSY;
        }
        final int np = numPredict > 0 ? numPredict : 220;
        Job job = new Job(model, prompt, np);
        boolean enqueued;
        try {
            enqueued = queue.offer(job, ENQUEUE_GRACE_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[MODELBUS] ENQUEUE interrupted for {}", model);
            return BUSY;
        }
        if (!enqueued) {
            log.warn("[MODELBUS] BUSY queue full (cap {}) after {}s grace: {}",
                    QUEUE_CAP, ENQUEUE_GRACE_S, model);
            return BUSY;
        }
        log.info("[MODELBUS] ENQUEUE {} (depth {})", model, queue.size());
        try {
            if (!job.done.await(DELEGATE_TIMEOUT_S + 30, TimeUnit.SECONDS)) {
                log.warn("[MODELBUS] TIMEOUT wait>{}s for {} (abandoning latch)",
                        DELEGATE_TIMEOUT_S + 30, model);
                return BUSY;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return BUSY;
        }
        return job.result;
    }

    /** informational: model the mic currently points at (best effort). */
    public static String getActiveModel() { return activeModel; }

    // ------------------------------------------------------------------ worker

    private static void workerLoop() {
        while (true) {
            Job job;
            try {
                job = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long t0 = System.currentTimeMillis();
            String via = null;
            // GODHAND-3 LAW 5 (full admission at dispatch): resources + residency +
            // game priority decided the moment the job leaves the queue. Rejected
            // jobs complete instantly with BUSY — the caller's timeout never fires.
            AdmissionControl.Kind kind = job.embed ? AdmissionControl.Kind.EMBED : AdmissionControl.Kind.INFER;
            AdmissionControl.Verdict v = LawEnforcer.admit(job.model, kind);
            if (!v.allowed) {
                job.result = BUSY;
                log.warn("[MODELBUS] LAW5 admission deny {}: {}", job.model, v);
                job.done.countDown();
                continue;
            }
            LawEnforcer.residencyAcquire(job.embed ? 0 : 1); // LAW 4: count residency (embed excluded)
            LawEnforcer.record("modelbus", "infer", job.model);
            try {
                if (orchestratorHealthy()) {
                    via = "DELEGATE";
                    log.info("[MODELBUS] DELEGATE {} -> orchestrator", job.model);
                    job.result = viaOrchestrator(job.model, job.prompt, job.numPredict);
                } else {
                    // UNIFY-1: no direct fallback — gateway down == BUSY, instantly.
                    // Residency release + record still run in the finally below.
                    job.result = BUSY;
                    log.warn("[MODELBUS] BUSY gateway down ({} unreachable): {}",
                            ORCH_URL, job.model);
                }
            } catch (Exception e) {
                // UNIFY-1: no direct fallback — delegate failure == BUSY.
                log.warn("[MODELBUS] BUSY delegate failed ({}): {}",
                        e.toString(), job.model);
                job.result = BUSY;
            } finally {
                // GODHAND-3 L4: residency must be freed on EVERY exit path — an
                // Error escaping the catch above would otherwise wedge the
                // resident cap at full forever (admission permanently denies).
                LawEnforcer.residencyRelease(job.embed ? 0 : 1);
                LawEnforcer.record("modelbus", "done", job.model + " via " + via);
            }
            activeModel = job.model;
            log.info("[MODELBUS] DONE {} via {} in {}ms chars={} ok={}",
                    job.model, via, System.currentTimeMillis() - t0,
                    job.result.length(), isRealText(job.result));
            job.done.countDown();
        }
    }

    private static boolean isRealText(String r) {
        return r != null && !r.equals(BUSY) && !r.startsWith("NO_RESPONSE");
    }

    // ------------------------------------------------------- orchestrator mode

    private static boolean orchestratorHealthy() {
        long now = System.currentTimeMillis();
        if (now - lastProbeMs < PROBE_CACHE_MS) return orchUp;
        lastProbeMs = now;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(ORCH_URL + "/health")
                    .openConnection();
            c.setConnectTimeout(2000);
            c.setReadTimeout(2000);
            orchUp = (c.getResponseCode() == 200);
            c.disconnect();
        } catch (Exception e) {
            orchUp = false;
        }
        return orchUp;
    }

    private static String viaOrchestrator(String model, String prompt, int numPredict) throws Exception {
        String body = MAPPER.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
            put("model", model);
            put("prompt", prompt);
            put("num_predict", numPredict);
            put("wait", true);
        }});
        HttpURLConnection c = (HttpURLConnection) new URL(ORCH_URL + "/v1/run")
                .openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(5000);
        c.setReadTimeout((int) (DELEGATE_TIMEOUT_S * 1000));
        c.setRequestProperty("Content-Type", "application/json");
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        c.setFixedLengthStreamingMode(payload.length);
        try (OutputStream os = c.getOutputStream()) {
            os.write(payload);
            os.flush();
        }
        int code = c.getResponseCode();
        String resp;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                code == 200 ? c.getInputStream() : c.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            resp = sb.toString();
        } finally {
            c.disconnect();
        }
        if (code != 200) {
            // UNIFY-1: any documented reject (queue full 429, resource deny 507,
            // wait timeout 504, gateway->ollama unreachable 502) == BUSY; no fallback.
            log.warn("[MODELBUS] orchestrator /v1/run HTTP {}: {}", code, abbrev(resp));
            return BUSY;
        }
        JsonNode n = MAPPER.readTree(resp);
        String text = n.path("response").asText("");
        if (text.isEmpty()) {
            log.warn("[MODELBUS] orchestrator empty response: {}", abbrev(resp));
            return BUSY;
        }
        return text;
    }

    // UNIFY-1: the direct-Ollama fallback (viaDirectOllama + /api/generate on
    // :11434) is GONE. Gateway down or a failed delegate == BUSY, documented.

    private static String abbrev(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }
}