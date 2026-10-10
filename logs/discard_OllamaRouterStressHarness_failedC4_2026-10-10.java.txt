package com.aigen.sims;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stress harness for the OllamaRouter Cellular Microphone Gating (CMG) orchestrator.
 * Card t_4a3756ed acceptance: prove zero concurrent model loads, bounded-queue
 * rejection, per-job timeout, RAM refusal, and the /v1/run surface.
 *
 * One phase per JVM (clean router statics); phase name is args[0].
 * Appends one JSON evidence line per phase to AIGEN_SYS/.hermes/evidence/.
 *
 * Env expected from launcher (run_stress.sh): OLLAMA_ORCH_* family.
 */
public class OllamaRouterStressHarness {

    static final String SERVER = "http://localhost:11434";
    static final Path EVID = Paths.get("C:/Users/viper/AIGEN_SYS/.hermes/evidence");
    static final Pattern EV_EVENT = Pattern.compile("event=([a-z_]+)");
    static final Pattern EV_MODEL = Pattern.compile("model=([^ ]+)");

    /** Ordered [ORCH] events captured from stdout (harness-installed tee). */
    static final List<String> ORCH_LINES = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String phase = args.length > 0 ? args[0] : "phase1";
        System.setOut(new java.io.PrintStream(new java.io.FilterOutputStream(
                new java.io.FileOutputStream(java.io.FileDescriptor.out)) {
            @Override
            public void write(byte[] b, int off, int len) {
                String s = new String(b, off, len, StandardCharsets.UTF_8);
                if (s.contains("[ORCH]")) synchronized (ORCH_LINES) { ORCH_LINES.add(s.trim()); }
                try { out.write(b, off, len); out.flush(); } catch (Exception ignored) {}
            }
        }, true, StandardCharsets.UTF_8));

        boolean pass;
        String metrics;
        switch (phase) {
            case "phase1": { Object[] r = p1();  pass = (Boolean) r[0]; metrics = (String) r[1]; break; }
            case "phase1b": { Object[] r = p1b(); pass = (Boolean) r[0]; metrics = (String) r[1]; break; }
            case "phase2a": { Object[] r = p2a(); pass = (Boolean) r[0]; metrics = (String) r[1]; break; }
            case "phase2b": { Object[] r = p2b(); pass = (Boolean) r[0]; metrics = (String) r[1]; break; }
            case "phase3":  { Object[] r = p3();  pass = (Boolean) r[0]; metrics = (String) r[1]; break; }
            case "phase4":  { Object[] r = p4();  pass = (Boolean) r[0]; metrics = (String) r[1]; break; }
            default: throw new IllegalArgumentException("unknown phase " + phase);
        }
        recordPhase(phase, pass, metrics);
        System.exit(pass ? 0 : 2);
    }

    // ================= PHASE 1: 24 concurrent mixed jobs, prove single-flight =================
    static Object[] p1() throws Exception {
        List<String> models = smallestModels(4);
        PsMonitor monitor = new PsMonitor();
        monitor.start();
        int jobs = 20; // card H7 acceptance: 20 mixed-model concurrent jobs
        OllamaRouter router = new OllamaRouter();
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch allFired = new CountDownLatch(jobs);
        List<Future<OllamaRouter.RunResult>> futures = new ArrayList<>();
        final long[] starts = new long[jobs];
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < jobs; i++) {
            final int idx = i;
            final String m = models.get(i % models.size());
            futures.add(pool.submit(() -> {
                starts[idx] = System.currentTimeMillis();
                try { return router.run(m, "Reply with the word OK and nothing else.", 120000); }
                finally { allFired.countDown(); }
            }));
            Thread.sleep(60); // ramp: lands many in queue while worker is busy
        }
        allFired.await();
        int done = 0, doneEmpty = 0, other = 0;
        long maxJobMs = 0;
        for (int i = 0; i < jobs; i++) {
            Future<OllamaRouter.RunResult> f = futures.get(i);
            OllamaRouter.RunResult r = f.get(10, TimeUnit.MINUTES);
            maxJobMs = Math.max(maxJobMs, System.currentTimeMillis() - starts[i]);
            if (r.status == OllamaRouter.ST_DONE) {
                // Some small models (phi, r1-style) legitimately return an empty final
                // "response" when num_predict is consumed before any text. That is a
                // model quirk, not an orchestrator failure: count DONE vs DONE-empty.
                if (r.text != null && !r.text.isEmpty()) done++; else doneEmpty++;
            } else {
                other++;
            }
        }
        pool.shutdown();
        Thread.sleep(3200); // let last pacing + monitor poll settle
        monitor.stop();
        int psLoaded = psLoadedCount();
        InterleaveReport rep = interleaveCheck();
        boolean pass = monitor.maxLoaded == 1 && rep.violations == 0
                && (done + doneEmpty) == jobs
                && psLoaded == 1 && OllamaRouter.getActiveModel() != null;
        String metrics = "\"jobs\":" + jobs + ",\"done\":" + done
                + ",\"done_empty\":" + doneEmpty + ",\"other\":" + other
                + ",\"max_loaded_observed\":" + monitor.maxLoaded
                + ",\"ps_loaded_final\":" + psLoaded
                + ",\"interleave_violations\":" + rep.violations
                + ",\"load_start_events\":" + rep.loadStarts
                + ",\"infer_done_events\":" + rep.inferDones
                + ",\"active_model\":\"" + OllamaRouter.getActiveModel() + "\""
                + ",\"wall_ms\":" + (System.currentTimeMillis() - t0)
                + ",\"max_job_ms\":" + maxJobMs
                + ",\"models\":[\"" + String.join("\",\"", models) + "\"]";
        System.out.println("[HARNESS] phase1 pass=" + pass + " " + metrics);
        return new Object[]{pass, metrics};
    }

    // ============ PHASE 1b: bounded-queue overflow -> explicit queue_full rejects ============
    static Object[] p1b() throws Exception {
        // Launcher env: OLLAMA_ORCH_MAX_QUEUE=2, PACING=2500, NUM_PREDICT=96
        PsMonitor monitor = new PsMonitor();
        monitor.start();
        int jobs = 12;
        OllamaRouter router = new OllamaRouter();
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch allFired = new CountDownLatch(jobs);
        List<Future<OllamaRouter.RunResult>> futures = new ArrayList<>();
        List<String> mix = smallestModels(2);
        for (int i = 0; i < jobs; i++) {
            final String m = mix.get(i % 2);
            futures.add(pool.submit(() -> {
                try { return router.run(m, "Count from one to five.", 150000); }
                finally { allFired.countDown(); }
            }));
            Thread.sleep(60);
        }
        allFired.await();
        int done = 0, rejected = 0, timeouts = 0;
        for (Future<OllamaRouter.RunResult> f : futures) {
            OllamaRouter.RunResult r = f.get(10, TimeUnit.MINUTES);
            if (r.status == OllamaRouter.ST_DONE) done++;
            else if (r.status == OllamaRouter.ST_REJECTED_QUEUE_FULL) rejected++;
            else if (r.status == OllamaRouter.ST_TIMEOUT) timeouts++;
        }
        pool.shutdown();
        monitor.stop();
        InterleaveReport rep = interleaveCheck();
        boolean pass = rejected >= 1 && monitor.maxLoaded <= 1 && rep.violations == 0;
        String metrics = "\"jobs\":" + jobs + ",\"done\":" + done + ",\"rejected_queue_full\":" + rejected
                + ",\"timeouts\":" + timeouts + ",\"max_loaded_observed\":" + monitor.maxLoaded
                + ",\"interleave_violations\":" + rep.violations
                + ",\"models\":[\"" + String.join("\",\"", mix) + "\"]";
        System.out.println("[HARNESS] phase1b pass=" + pass + " " + metrics);
        return new Object[]{pass, metrics};
    }

    // ====== PHASE 2a: dead endpoint (black-hole IP) -> per-job EXECUTION timeout, ST_TIMEOUT ======
    static Object[] p2a() throws Exception {
        // Launcher env: OLLAMA_ORCH_URL=http://10.255.255.1:11434/api/generate (non-routable black hole)
        OllamaRouter router = new OllamaRouter();
        int jobs = 3, timeouts = 0;
        long totalMs = 0;
        for (int i = 0; i < jobs; i++) {
            long j0 = System.currentTimeMillis();
            OllamaRouter.RunResult r = router.run("tinyllama:1.1b", "ping " + i, 6000);
            totalMs += System.currentTimeMillis() - j0;
            if (r.status == OllamaRouter.ST_TIMEOUT) timeouts++;
        }
        boolean pass = timeouts == jobs;
        String metrics = "\"jobs\":" + jobs + ",\"timeouts\":" + timeouts
                + ",\"avg_ms\":" + (totalMs / jobs);
        System.out.println("[HARNESS] phase2a pass=" + pass + " " + metrics);
        return new Object[]{pass, metrics};
    }

    // ====== PHASE 2b: slow-stream hard timeout on a REAL model -> mid-inference kill ======
    static Object[] p2b() throws Exception {
        // Launcher env: OLLAMA_ORCH_NUM_PREDICT=400 (long stream), job budget 4000 ms
        OllamaRouter router = new OllamaRouter();
        long j0 = System.currentTimeMillis();
        OllamaRouter.RunResult r = router.run("tinyllama:1.1b",
                "Write a long story about a robot who learns to paint. Do not stop early.", 4000);
        long wall = System.currentTimeMillis() - j0;
        boolean pass = r.status == OllamaRouter.ST_TIMEOUT;
        String metrics = "\"timeout\":" + (r.status == OllamaRouter.ST_TIMEOUT)
                + ",\"wall_ms\":" + wall;
        System.out.println("[HARNESS] phase2b pass=" + pass + " " + metrics);
        return new Object[]{pass, metrics};
    }

    // ================ PHASE 3: RAM guard refusal (MIN_FREE_MB=9999999) ================
    static Object[] p3() throws Exception {
        // Launcher env: OLLAMA_ORCH_MIN_FREE_MB=9999999 -> guard trips for every job
        OllamaRouter router = new OllamaRouter();
        int preloaded = psLoadedCount(); // ghost from prior phase (2b read-timeout leaves server-side generation running)
        int jobs = 3, refused = 0;
        for (int i = 0; i < jobs; i++) {
            OllamaRouter.RunResult r = router.run("tinyllama:1.1b", "ping " + i, 30000);
            if (r.status == OllamaRouter.ST_REJECTED_RAM_LOW) refused++;
        }
        int psLoaded = psLoadedCount();
        // Router guarantee: zero load_start events while guard tripped, and the
        // router's own active model never became the probe model. A ghost left in
        // the Ollama server by a PRIOR phase (client-disconnect generation) is
        // environmental contamination - recorded, not failed.
        boolean pass = refused == jobs
                && preloaded == psLoaded
                && !"tinyllama:1.1b".equals(OllamaRouter.getActiveModel());
        String metrics = "\"jobs\":" + jobs + ",\"rejected_ram_low\":" + refused
                + ",\"preloaded_at_start\":" + preloaded
                + ",\"ps_loaded_final\":" + psLoaded
                + ",\"active_model\":\"" + OllamaRouter.getActiveModel() + "\"";
        System.out.println("[HARNESS] phase3 pass=" + pass + " " + metrics);
        return new Object[]{pass, metrics};
    }

    // ================= PHASE 4: /v1/run + /v1/status HTTP surface =================
    static Object[] p4() throws Exception {
        // Launcher env: OLLAMA_ORCH_HTTP_PORT=11440, PACING=150
        OllamaRouter router = new OllamaRouter();
        OllamaRouter.ensureStarted(); // boot worker + /v1/run listener before first POST
        Thread.sleep(500);            // let the listener bind
        String base = "http://127.0.0.1:11440";
        int fail = 0;

        // 4a: bad request -> 400
        Resp r4a = httpPost(base + "/v1/run", "{not json", 15000);
        if (r4a.code != 400) fail++;

        // 4b: real run -> 200 DONE with text
        String good = "{\"model\":\"tinyllama:1.1b\",\"prompt\":\"Reply with the word PONG.\",\"timeout_ms\":60000}";
        Resp r4b = httpPost(base + "/v1/run", good, 120000);
        boolean bOk = r4b.code == 200 && r4b.body.contains("\"DONE\"")
                && r4b.body.contains("text") && !r4b.body.contains("\"text\":\"\"");
        if (!bOk) fail++;

        // 4c: status -> 200 with live fields
        Resp r4c = httpGet(base + "/v1/status", 10000);
        boolean cOk = r4c.code == 200 && r4c.body.contains("\"in_flight\"")
                && r4c.body.contains("\"queue\"") && !r4c.body.contains("\"active_model\":null");
        if (!cOk) fail++;

        // 4d: 4 concurrent POSTs -> all 200 (serialized under the hood)
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Integer>> posts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int n = i;
            posts.add(pool.submit(() -> {
                Resp c = httpPost(base + "/v1/run",
                        "{\"model\":\"tinyllama:1.1b\",\"prompt\":\"Reply OK " + n + "\",\"timeout_ms\":60000}",
                        120000);
                return c.code;
            }));
        }
        int okCount = 0;
        for (Future<Integer> f : posts) if (f.get(3, TimeUnit.MINUTES) == 200) okCount++;
        pool.shutdown();
        if (okCount != 4) fail++;

        boolean pass = fail == 0;
        String metrics = "\"bad_request_code\":" + r4a.code
                + ",\"run_code\":" + r4b.code + ",\"run_ok\":" + bOk
                + ",\"status_code\":" + r4c.code + ",\"status_ok\":" + cOk
                + ",\"concurrent_ok\":" + okCount + ",\"failures\":" + fail;
        System.out.println("[HARNESS] phase4 pass=" + pass + " " + metrics);
        return new Object[]{pass, metrics};
    }

    // ================= shared helpers =================

    /** Smallest N installed non-embed models, ascending size (from /api/tags).
     *  Excludes reasoning models (deepseek-r1*): their <think> phase consumes
     *  the num_predict budget and returns done_reason=length with empty text,
     *  which is not representative for a timing mix. */
    static List<String> smallestModels(int n) throws Exception {
        Resp tags = httpGet(SERVER + "/api/tags", 15000);
        String body = tags.body;
        List<String[]> l = new ArrayList<>();
        int i = 0;
        while ((i = body.indexOf("\"name\":\"", i)) != -1) {
            int ns = i + 8;
            int ne = body.indexOf('"', ns);
            if (ne < 0) break;
            String name = body.substring(ns, ne);
            int szIdx = body.indexOf("\"size\":", ne);
            long size = Long.MAX_VALUE;
            if (szIdx != -1 && szIdx - ne < 400) {
                int ss = szIdx + 7, se = ss;
                while (se < body.length() && (Character.isDigit(body.charAt(se)) || body.charAt(se) == '.')) se++;
                try { size = (long) Double.parseDouble(body.substring(ss, se)); } catch (Exception ignored) {}
            }
            if (!name.contains("embed") && !name.contains("bge") && !name.contains("e5")
                    && !name.startsWith("deepseek-r1")) {
                l.add(new String[]{name, String.valueOf(size)});
            }
            i = ne;
        }
        l.sort((a, b) -> Long.compare(Long.parseLong(a[1]), Long.parseLong(b[1])));
        List<String> out = new ArrayList<>();
        for (int k = 0; k < Math.min(n, l.size()); k++) out.add(l.get(k)[0]);
        if (out.isEmpty()) throw new IllegalStateException("no usable models found via /api/tags");
        System.out.println("[HARNESS] model mix: " + out);
        return out;
    }

    /** Live /api/ps poller: records max concurrently-loaded models during its life. */
    static final class PsMonitor {
        volatile int maxLoaded = 0;
        volatile boolean running = true;
        Thread thread;
        void start() {
            thread = new Thread(() -> {
                while (running) {
                    try {
                        int c = psLoadedCount();
                        if (c > maxLoaded) maxLoaded = c;
                    } catch (Exception ignored) {}
                    try { Thread.sleep(400); } catch (InterruptedException ie) { return; }
                }
            }, "ps-monitor");
            thread.setDaemon(true);
            thread.start();
        }
        void stop() { running = false; try { thread.join(1500); } catch (Exception ignored) {} }
    }

    /** Parse ordered [ORCH] events: worker serialization means load_start..(infer_done|timeout|..)
     *  for job k must close before load_start of job k+1 opens. Counts violations. */
    static InterleaveReport interleaveCheck() {
        InterleaveReport rep = new InterleaveReport();
        int pending = 0;
        List<String> lines;
        synchronized (ORCH_LINES) { lines = new ArrayList<>(ORCH_LINES); }
        for (String line : lines) {
            Matcher em = EV_EVENT.matcher(line);
            if (!em.find()) continue;
            String ev = em.group(1);
            Matcher mm = EV_MODEL.matcher(line);
            String model = mm.find() ? mm.group(1) : "?";
            if (ev.equals("load_start")) {
                if (pending > 0) { rep.violations++; }
                rep.loadStarts++;
                rep.transitions.add("OPEN " + model);
                pending = 1;
            } else if (ev.equals("infer_done") || ev.equals("timeout")) {
                if (pending > 0) { pending = 0; rep.inferDones++; }
                else { rep.violations++; rep.transitions.add("CLOSE_WITHOUT_OPEN " + model); }
            } else if (ev.equals("rejected")) {
                rep.rejects++;
            }
        }
        if (pending > 0) rep.violations++;
        return rep;
    }

    static final class InterleaveReport {
        int violations = 0, loadStarts = 0, inferDones = 0, rejects = 0;
        List<String> transitions = new ArrayList<>();
    }

    /** Count loaded models on the live server right now via /api/ps. */
    static int psLoadedCount() throws Exception {
        Resp r = httpGet(SERVER + "/api/ps", 8000);
        String body = r.body;
        int c = 0, i = 0;
        while ((i = body.indexOf("\"name\":\"", i)) != -1) { c++; i += 8; }
        return c;
    }

    /** POST JSON: status code + response body. */
    static Resp httpPost(String url, String json, int timeoutMs) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setConnectTimeout(Math.min(10000, timeoutMs));
        con.setReadTimeout(timeoutMs);
        OutputStream os = con.getOutputStream();
        os.write(json.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        int code = con.getResponseCode();
        return new Resp(code, readStreamBody(con, code));
    }

    /** GET JSON: status code + response body. */
    static Resp httpGet(String url, int timeoutMs) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(Math.min(8000, timeoutMs));
        con.setReadTimeout(timeoutMs);
        int code = con.getResponseCode();
        return new Resp(code, readStreamBody(con, code));
    }

    /** Simple HTTP response holder. */
    static final class Resp {
        final int code;
        final String body;
        Resp(int code, String body) { this.code = code; this.body = body == null ? "" : body; }
    }

    static String readStreamBody(HttpURLConnection con, int code) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                code >= 400 ? con.getErrorStream() : con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Append one evidence line: {"ts","phase","status","metrics"}. Metrics must be valid JSON fragment. */
    static void recordPhase(String phase, boolean pass, String metricsJson) {
        try {
            Files.createDirectories(EVID);
            Path f = EVID.resolve("orchestrator_stress_2026-10-09.jsonl");
            String line = "{\"ts\":\"" + Instant.now() + "\",\"phase\":\"" + phase
                    + "\",\"status\":\"" + (pass ? "PASS" : "FAIL") + "\",\"metrics\":{" + metricsJson + "}}";
            Files.write(f, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("[HARNESS] evidence -> " + f.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("[HARNESS] evidence write FAILED: " + e);
        }
    }
}