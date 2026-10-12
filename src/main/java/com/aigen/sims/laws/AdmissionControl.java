package com.aigen.sims.laws;

import java.util.ArrayList;
import java.util.List;

/**
 * GODHAND-3 (t_22bb44a7) — LAW 5: in-JVM AdmissionControl.
 *
 * Decides whether a (model, kind) request may pass the ModelBus right now,
 * mirroring ~/.aria/engine/ops_health.py admit() semantics:
 *
 *   L5a  os_free_ram_mb >= spec.ram_mb + policy.ram_floor_mb → else RAM deny
 *   L5b  CPU > 85% → deny (graceful check; -1 = unknown = don't block)
 *   L5c  game priority: while sim/game is "running" heavyweight (L/M) tiers wait
 *   L5d  max 2 resident models (embed excluded); L-tier requests insist on idle
 *   L5e  policy gate (ModelPolicy.checkModel): banned/unknown → deny
 *
 * The live 8899 instance is untouched: this applies to NEW ModelBus traffic
 * once wired (todo #3). Verdicts are pure decisions; no subprocess is ever
 * spawned by this class (comment priority: never launch `ollama ...`).
 */
public final class AdmissionControl {

    public static final int GAME_CPU_CEILING = 85;   // L5b
    public static final int MAX_RESIDENT = 2;        // L5d (policy can lower only)

    /** Kind of ask: mirror of ops_health model_decision kinds. */
    public enum Kind { INFER, LOAD, BATCH, EMBED }

    public static final class Verdict {
        public final boolean allowed;
        public final String reason;
        public final long freeRamMb;
        public final int cpuPercent;
        Verdict(boolean allowed, String reason, long freeRamMb, int cpuPercent) {
            this.allowed = allowed; this.reason = reason;
            this.freeRamMb = freeRamMb; this.cpuPercent = cpuPercent;
        }
        @Override public String toString() {
            return (allowed ? "ADMIT " : "DENY  ") + reason + " (ram=" + freeRamMb + "mb cpu=" + cpuPercent + "%)";
        }
    }

    /** Test hook: force the CPU reading (null = probe normally). */
    static volatile Integer cpuOverride = null;

    private AdmissionControl() { /* static-only */ }

    /**
     * The single judgement call ModelBus.enqueue/dispatch delegates to.
     *
     * @param model    model name ("llama3.2:3b" etc.)
     * @param kind     INFER | LOAD | BATCH | EMBED
     * @param gameRunning true while the JavaFX sim loop is actively ticking
     * @param residentCount current number of resident non-embed models
     */
    public static Verdict decide(String model, Kind kind, boolean gameRunning, int residentCount) {
        return decide(model, kind, gameRunning, residentCount, -1);
    }

    /** Overload with explicit freeRam for tests (probeRam<0 = probe normally). */
    public static Verdict decide(String model, Kind kind, boolean gameRunning, int residentCount, long probeRamMb) {
        long freeRam = probeRamMb >= 0 ? probeRamMb : SystemProbe.freeRamMb();
        int cpu = cpuOverride != null ? cpuOverride : SystemProbe.cpuLoadPercent();

        // L5e — policy gate first: banned/unknown deny regardless of resources
        ModelPolicy.Verdict policy = ModelPolicy.checkModel(model);
        if (!policy.allowed) return new Verdict(false, "policy: " + policy.reason, freeRam, cpu);

        ModelPolicy.ModelSpec spec = ModelPolicy.spec(model);
        String tier = spec.tier;

        // L5b — CPU ceiling (unknown CPU never blocks — grace over strictness)
        if (cpu >= 0 && cpu > GAME_CPU_CEILING) {
            return new Verdict(false, "cpu " + cpu + "% > " + GAME_CPU_CEILING + "% ceiling", freeRam, cpu);
        }

        // L5d — residency: embed excluded from the cap; LOAD/BATCH/INFER all need a slot
        boolean embed = "embed".equals(tier);
        if (!embed && residentCount >= ModelPolicy.maxLoaded()) {
            return new Verdict(false, "resident cap " + ModelPolicy.maxLoaded() + " reached (" + residentCount + " resident)", freeRam, cpu);
        }

        // L5d — exclusive tiers: L needs an idle game AND an empty house
        if ("L".equals(tier)) {
            if (gameRunning) return new Verdict(false, "L-tier " + model + " requires game idle (game is running)", freeRam, cpu);
            if (residentCount > 0) return new Verdict(false, "L-tier " + model + " requires exclusive residency (" + residentCount + " resident)", freeRam, cpu);
        }
        if ("restricted".equals(tier) && gameRunning) {
            return new Verdict(false, "restricted-tier " + model + " waits for game idle", freeRam, cpu);
        }

        // L5c — game priority: heavyweight loads defer while the sim ticks
        if (gameRunning && kind == Kind.LOAD && ("M".equals(tier) || "L".equals(tier))) {
            return new Verdict(false, "game priority: " + tier + "-tier LOAD defers while sim runs", freeRam, cpu);
        }

        // L5a — RAM: footprint + floor must fit in OS free
        long needed = spec.ramMb + (embed ? 0 : ModelPolicy.ramFloorMb());
        if (freeRam < needed) {
            return new Verdict(false, "ram " + freeRam + "mb < " + needed + "mb (" + spec.ramMb + "+" + ModelPolicy.ramFloorMb() + " floor)", freeRam, cpu);
        }

        return new Verdict(true, tier + "-tier allowed", freeRam, cpu);
    }

    /** Convenience: should the bus degrade to BUSY? (law 7) */
    public static boolean shouldRejectAsBusy(Verdict v) { return !v.allowed && v.freeRamMb < 512; }

    /** For tests: reset any overrides. */
    static void resetForTests() { cpuOverride = null; }
}