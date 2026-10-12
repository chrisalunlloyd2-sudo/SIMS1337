package com.aigen.sims.laws;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GODHAND-3 (t_22bb44a7) — LAW 2: PhaseWatchdog (progress-or-cancel).
 *
 * Per pipeline phase (mine → deploy → tune → grow), the scheduler must show
 * forward progress or the watchdog cancels the phase. Two thresholds:
 *
 *   - no-progress timeout: no phase event for this long → cancel
 *   - max wall time: hard cap regardless of dribbling progress
 *
 * Reentrancy model:PipelineScheduler is single-threaded over its EventBus,
 * so watchdog methods take a simple monitor lock; cheap and adequate.
 * Cancellation is a STATUS CHANGE consumed by the scheduler — this class
 * never kills threads (nothing in the pipeline holds locks across awaits).
 */
public final class PhaseWatchdog {

    public static final String[] PHASES = {"mine", "deploy", "tune", "grow"};

    /** Cancellation notice the scheduler polls/consumes. */
    public static final class Cancel {
        public final String phase;
        public final String reason;
        public final long atMs;
        Cancel(String phase, String reason) {
            this.phase = phase; this.reason = reason; this.atMs = System.currentTimeMillis();
        }
        @Override public String toString() {
            return "WATCHDOG CANCEL phase=" + phase + " reason=" + reason + " at=" + atMs;
        }
    }

    public static final long DEFAULT_NO_PROGRESS_MS = 20 * 60 * 1000L; // 20 min
    public static final long DEFAULT_MAX_PHASE_MS   = 90 * 60 * 1000L; // 90 min

    private final long noProgressMs;
    private final long maxPhaseMs;

    private static final class PhaseState {
        long startedAt, lastProgressAt;
    }
    private final Map<String, PhaseState> running = new ConcurrentHashMap<>();
    private final Deque<Cancel> cancels = new ArrayDeque<>();

    public PhaseWatchdog() { this(DEFAULT_NO_PROGRESS_MS, DEFAULT_MAX_PHASE_MS); }

    /** Test seam: tiny thresholds (e.g. 50ms) prove the cancel path in ms not minutes. */
    public PhaseWatchdog(long noProgressMs, long maxPhaseMs) {
        this.noProgressMs = noProgressMs;
        this.maxPhaseMs = maxPhaseMs;
    }

    /** Mark a phase as started (restarts both clocks). */
    public void begin(String phase) {
        long now = System.currentTimeMillis();
        PhaseState st = new PhaseState();
        st.startedAt = st.lastProgressAt = now;
        running.put(phase, st);
    }

    /** Any forward movement: a step finished, a model answered, a block appended. */
    public void progress(String phase) {
        PhaseState st = running.get(phase);
        if (st != null) st.lastProgressAt = System.currentTimeMillis();
    }

    /** Phase naturally finished — clears its clocks, no cancel possible. */
    public void end(String phase) {
        running.remove(phase);
    }

    /** LAW 2 check: for every live phase, cancel if stalled past no-progress OR past wall cap.
     *  Returns cancellations issued this call (caller logs + acts on them). */
    public synchronized List<Cancel> check() {
        List<Cancel> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, PhaseState> e : running.entrySet()) {
            PhaseState st = e.getValue();
            if (now - st.lastProgressAt > noProgressMs) {
                out.add(new Cancel(e.getKey(), "no progress for " + ((now - st.lastProgressAt) / 1000) + "s (limit " + (noProgressMs / 1000) + "s)"));
                running.remove(e.getKey());
            } else if (now - st.startedAt > maxPhaseMs) {
                out.add(new Cancel(e.getKey(), "phase wall clock " + ((now - st.startedAt) / 1000) + "s exceeded (limit " + (maxPhaseMs / 1000) + "s)"));
                running.remove(e.getKey());
            }
        }
        out.forEach(cancels::addLast);
        return out;
    }

    /** Consumed cancellations (scheduler drains after check()). */
    public synchronized List<Cancel> drainCancels() {
        List<Cancel> out = new ArrayList<>(cancels);
        cancels.clear();
        return out;
    }

    public synchronized boolean isCancelled(String phase) {
        return cancels.stream().anyMatch(c -> c.phase.equals(phase));
    }

    public boolean isRunning(String phase) { return running.containsKey(phase); }
}