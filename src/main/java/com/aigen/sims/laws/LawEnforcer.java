package com.aigen.sims.laws;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GODHAND-3 (t_22bb44a7) — LAW 7 enforcer + residency bookkeeping (L4).
 *
 * LawEnforcer is the single facade the rest of the app touches:
 *
 *   - boot(): L1 lock + policy load + ledger open. Exits JVM (exit code 17)
 *     when another instance holds the lock.
 *   - residency(): L4 cellular staging counters — how many models are
 *     currently resident non-embed (cap 2), how many single-flight admits.
 *   - admit(): L5 full admission decision for a (model, kind).
 *   - lawsStatusJson(): /api/laws payload — one JSON doc describing every
 *     law's current state, additive and human-readable.
 *
 * Never spawns processes (standing priority: no `ollama serve/pull` ever),
 * never binds ports, never touches the live 8899 instance.
 */
public final class LawEnforcer {

    public static final int LOCK_EXIT_CODE = 17; // second-instance exit code

    private static final AtomicInteger RESIDENT_NON_EMBED = new AtomicInteger(0);
    private static final AtomicLong ADMITS = new AtomicLong(0);
    private static final AtomicLong DENIES = new AtomicLong(0);
    private static final AtomicLong BUSY_REJECTS = new AtomicLong(0);

    /** Set true by the sim loop while it ticks (game priority, L5c). */
    private static volatile boolean gameRunning = false;

    private static volatile PhaseWatchdog watchdog = new PhaseWatchdog();
    private static volatile HexflowLedger ledger = null;
    // GODHAND-3 L2 poller: only AutoLoop's JVM runs pipeline phases, so the
    // watchdog stall check must live there too. One daemon thread per JVM.
    private static volatile Thread watchdogPoller = null;
    /** Test seam: poll cadence in ms (prod = 30 000). Package-private on purpose. */
    static volatile long watchdogPollMs = 30_000L;

    private LawEnforcer() { /* static-only */ }

    /** LAW 1 + policy + ledger boot. Returns true when this instance may run. */
    public static boolean boot() {
        if (!SingleInstanceLock.tryAcquire()) {
            System.out.println("[LAWS] L1 FAIL: another GodHand instance holds " + SingleInstanceLock.lockFile()
                    + " — exiting (exit " + LOCK_EXIT_CODE + ")");
            return false;
        }
        ModelPolicy.reload();
        openLedger();
        System.out.println("[LAWS] L1 OK: single-instance lock held (" + SingleInstanceLock.lockFile() + "); "
                + "policy=" + ModelPolicy.source() + "; ledger=" + (ledger != null ? ledger.file() : "(off)"));
        return true;
    }

    static void openLedger() {
        try {
            ledger = HexflowLedger.defaultLedger();
        } catch (Throwable t) {
            ledger = null; // law 7: ledger off ≠ instance off
            System.err.println("[LAWS] hexflow ledger unavailable (degraded): " + t);
        }
    }

    /**
     * GODHAND-3 L2/L6 harness mode: attach ONLY the hexflow ledger in a JVM
     * that did NOT boot() (i.e. AutoLoop, where the L1 lock belongs to the
     * GodHand dashboard instance). Phase phase-transition records then reach
     * the same shared ledger chain instead of being silently dropped.
     * Never acquires the lock, never binds, idempotent.
     */
    public static void attachLedgerOnly() {
        if (ledger == null) openLedger();
    }

    /**
     * GODHAND-3 L2: start the watchdog stall poller (daemon, 30 s cadence).
     * The poller never kills threads — PhaseWatchdog cancellation is a recorded
     * status change; here it is logged and pushed to the ledger for the
     * pipeline runner to observe on its next drain. Idempotent.
     */
    public static synchronized void startWatchdogPoller() {
        if (watchdogPoller != null && watchdogPoller.isAlive()) return;
        watchdogPoller = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(watchdogPollMs);
                    PhaseWatchdog wd = watchdog; // snapshot
                    if (wd != null) { // test seams may null it mid-run: skip tick, don't spam
                        for (PhaseWatchdog.Cancel c : wd.check()) {
                            System.err.println("[LAWS] " + c);
                            record(c.phase, "cancel", c.reason);
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return; // JVM shutting down
                } catch (Throwable t) {
                    System.err.println("[LAWS] watchdog poller hiccup (continuing): " + t);
                }
            }
        }, "godhand-watchdog-poller");
        watchdogPoller.setDaemon(true);
        watchdogPoller.start();
        System.out.println("[LAWS] L2 watchdog poller started (" + (watchdogPollMs / 1000) + "s cadence, daemon)");
    }

    /** Test seam: stops the poller thread (test class shutdown only). */
    static void stopWatchdogPollerForTest() {
        Thread t = watchdogPoller;
        watchdogPoller = null;
        if (t != null) t.interrupt(); // loop sees InterruptedException and returns
    }

    /** Test seam: force ledger state (null = detached). */
    static void setLedgerForTest(HexflowLedger l) {
        ledger = l;
    }

    /** Test seam: poller thread liveness (test proves error-survival). */
    static boolean watchdogPollerAliveForTest() {
        Thread t = watchdogPoller;
        return t != null && t.isAlive();
    }

    // ---- LAW 5 admission ----

    public static AdmissionControl.Verdict admit(String model, AdmissionControl.Kind kind) {
        return admit(model, kind, -1);
    }

    /** probeRamMb = test seam; -1 probes normally. */
    public static AdmissionControl.Verdict admit(String model, AdmissionControl.Kind kind, long probeRamMb) {
        AdmissionControl.Verdict v = AdmissionControl.decide(model, kind, gameRunning,
                RESIDENT_NON_EMBED.get(), probeRamMb);
        if (v.allowed) ADMITS.incrementAndGet(); else DENIES.incrementAndGet();
        if (AdmissionControl.shouldRejectAsBusy(v)) BUSY_REJECTS.incrementAndGet();
        return v;
    }

    /** L4 residency bookkeeping — ModelBus calls these when a model loads/exits. */
    public static void residencyAcquire(int weight) { RESIDENT_NON_EMBED.addAndGet(weight); }
    public static void residencyRelease(int weight) {
        int before = RESIDENT_NON_EMBED.get();
        int after = Math.max(0, before - weight);
        RESIDENT_NON_EMBED.compareAndSet(before, after); // never below zero
        // CAS loop for safety if concurrent:
        while (!RESIDENT_NON_EMBED.compareAndSet(before, after) && RESIDENT_NON_EMBED.get() != after) {
            before = RESIDENT_NON_EMBED.get();
            after = Math.max(0, before - weight);
        }
    }
    public static int residentCount() { return RESIDENT_NON_EMBED.get(); }

    // ---- LAW 2 watchdog passthrough ----

    public static PhaseWatchdog watchdog() { return watchdog; }
    public static void setWatchdog(PhaseWatchdog w) { watchdog = w; }

    // ---- LAW 6 ledger passthrough ----

    public static HexflowLedger ledger() { return ledger; }
    public static void record(String phase, String event) {
        HexflowLedger l = ledger;
        if (l != null) l.append(phase, event, "");
    }
    public static void record(String phase, String event, String detail) {
        HexflowLedger l = ledger;
        if (l != null) l.append(phase, event, detail);
    }

    // ---- game priority ----

    public static boolean isGameRunning() { return gameRunning; }
    public static void setGameRunning(boolean b) { gameRunning = b; }

    // ---- metrics for /api/laws ----

    public static long admits() { return ADMITS.get(); }
    public static long denies() { return DENIES.get(); }
    public static long busyRejects() { return BUSY_REJECTS.get(); }

    /** One additive JSON doc stating every law's live state. Served ONLY on the 8899 dashboard. */
    public static String lawsStatusJson() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"law1_single_instance\":")
          .append(SingleInstanceLock.isHeld())
          .append(",\"law2_watchdog\":{\"phases\":[");
        List<String> live = new ArrayList<>();
        for (String p : PhaseWatchdog.PHASES) if (watchdog().isRunning(p)) live.add("\"" + p + "\"");
        sb.append(String.join(",", live)).append("]},")
          .append("\"law3_loopback_only\":true,")
          .append("\"law4_staging\":{\"resident\":").append(RESIDENT_NON_EMBED.get())
          .append(",\"cap\":").append(ModelPolicy.maxLoaded()).append("},")
          .append("\"law5_admission\":{\"admits\":").append(ADMITS.get())
          .append(",\"denies\":").append(DENIES.get())
          .append(",\"busy\":").append(BUSY_REJECTS.get()).append("},")
          .append("\"law6_hexflow\":{\"seq\":").append(ledger != null ? ledger.seq() : -1)
          .append(",\"last\":\"").append(ledger != null ? ledger.lastHash() : "").append("\"},")
          .append("\"law7_degraded_ok\":true,");
        sb.append("\"policy\":\"").append(ModelPolicy.source().replace("\\", "/"))
          .append("\",\"gamePriority\":").append(gameRunning).append(",\"timestamp\":\"")
          .append(java.time.LocalDateTime.now().toString()).append("\"}");
        return sb.toString();
    }
}