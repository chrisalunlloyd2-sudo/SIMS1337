package com.aigen.sims.laws;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GODHAND-3 (t_22bb44a7) — acceptance tests for the seven laws.
 *
 * One test cluster per law:
 *   LAW 1  single-instance lock (boot acquires; a second JVM's channel denied; release restores)
 *   LAW 2  phase watchdog (progress within window keeps phase; stall cancels; wall cap cancels)
 *   LAW 3  loopback guard (loopback ok, LAN/refusal behaviour)
 *   LAW 4  cellular staging (3rd small at cap 2 waits; embed exempt)
 *   LAW 5  admission (low-RAM refusal, banned/unknown, CPU ceiling, game priority)
 *   LAW 6  hexflow ledger (chain verifies clean, seq continues across restart, tamper flagged)
 *   LAW 7  graceful degradation (status JSON always valid; missing policy → defaults, no throw)
 *   no-ollama-spawn: bytecode of every laws class contains no ProcessBuilder / Runtime.exec
 *
 * Uses TempDir for lock + ledger files and a temp model_policy.json via the -D
 * override so the live instance's real config is never touched.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LawsAcceptanceTest {

    @TempDir
    static Path tempDir;

    static Path policyFile;
    static Path lockFile;

    @BeforeAll
    static void primePolicyPath() throws Exception {
        policyFile = tempDir.resolve("model_policy.json");
        Files.writeString(policyFile, String.join("\n",
                "{",
                " \"ram_floor_mb\": 1500,",
                " \"max_loaded\": 2,",
                " \"banned_name_patterns\": [\"cloud\", \"gpt-oss:120b\"],",
                " \"models\": {",
                "   \"qwen2.5:0.5b\":           {\"tier\": \"S\", \"ram_mb\": 700,  \"max_tokens\": 256, \"timeout_s\": 60},",
                "   \"tinyllama:1.1b\":         {\"tier\": \"S\", \"ram_mb\": 900,  \"max_tokens\": 256, \"timeout_s\": 60},",
                "   \"llama3.2:1b\":            {\"tier\": \"S\", \"ram_mb\": 1500, \"max_tokens\": 256, \"timeout_s\": 90},",
                "   \"llama3.2:3b\":            {\"tier\": \"M\", \"ram_mb\": 2600, \"max_tokens\": 384, \"timeout_s\": 150},",
                "   \"mistral:7b\":             {\"tier\": \"L\", \"ram_mb\": 5600, \"max_tokens\": 512, \"timeout_s\": 300},",
                "   \"deepseek-r1:1.5b\":       {\"tier\": \"restricted\", \"ram_mb\": 1700, \"max_tokens\": 192, \"timeout_s\": 45},",
                "   \"nomic-embed-text:latest\":{\"tier\": \"embed\", \"ram_mb\": 400, \"max_tokens\": 0,   \"timeout_s\": 30}",
                " }",
                "}"));
        if (System.getProperty("com.aigen.sims.modelpolicy") == null) {
            System.setProperty("com.aigen.sims.modelpolicy", policyFile.toAbsolutePath().toString());
        }
        lockFile = tempDir.resolve("godhand.lock");
        if (System.getProperty("com.aigen.sims.lockfile") == null) {
            System.setProperty("com.aigen.sims.lockfile", lockFile.toAbsolutePath().toString());
        }
        ModelPolicy.reload();
    }

    @AfterAll
    static void releaseLockAndReset() {
        SingleInstanceLock.release();
        LawEnforcer.setGameRunning(false);
        LawEnforcer.setWatchdog(new PhaseWatchdog()); // fresh default for anything else
        AdmissionControl.resetForTests();
    }

    // ── LAW 1: single-instance lock ──────────────────────────

    @Test
    public void law1_bootAcquiresAndSecondInstanceDenied() {
        SingleInstanceLock.release();                       // start clean
        assertFalse(SingleInstanceLock.isHeld());
        assertTrue(LawEnforcer.boot(), "boot() acquires the lock on a fresh JVM");
        assertTrue(SingleInstanceLock.isHeld());

        // Second-instance proxy: a NEW file channel on the same lock file must
        // find it already locked (Windows file locks are per-handle, so this
        // fails exactly like a second JVM would see it).
        assertFalse(secondChannelCanLock(), "second instance must be denied (LAW 1)");

        SingleInstanceLock.release();
        assertFalse(SingleInstanceLock.isHeld());
        assertTrue(secondChannelCanLock(), "after release the file is free again");
        SingleInstanceLock.release();
    }

    /** Opens a NEW FileChannel on the lock file and tryLocks it (second-JVM proxy). */
    private static boolean secondChannelCanLock() {
        try {
            java.nio.channels.FileChannel ch = java.nio.channels.FileChannel.open(lockFile,
                    java.nio.file.StandardOpenOption.WRITE);
            java.nio.channels.FileLock second = ch.tryLock();
            if (second == null) { ch.close(); return false; }  // contended → denied
            second.release();
            ch.close();
            return true;                                        // got it → would-be second instance
        } catch (Exception e) {
            return false;                                       // contention error → denied
        }
    }

    // ── LAW 2: watchdog progress-or-cancel ───────────────────

    @Test
    public void law2_watchdogKeepsProgressingPhaseAndCancelsStalled() {
        PhaseWatchdog wd = new PhaseWatchdog(250, 400);     // tiny thresholds (test seam)
        LawEnforcer.setWatchdog(wd);
        wd.begin("mine");
        assertTrue(wd.isRunning("mine"));
        wd.progress("mine");                                 // forward movement resets clock
        sleep(80);                                           // 80 < 250 → inside window
        assertTrue(wd.check().isEmpty(), "progress inside the window → no cancel");

        sleep(400);                                          // ~480ms since last progress > 250
        List<PhaseWatchdog.Cancel> cancels = wd.check();
        assertEquals(1, cancels.size(), "stalled phase cancels exactly once");
        assertEquals("mine", cancels.get(0).phase);
        assertTrue(cancels.get(0).reason.contains("no progress"));
        assertFalse(wd.isRunning("mine"), "cancelled phase removed from live set");

        // Natural end clears clocks → nothing to cancel
        wd.begin("tune");
        wd.end("tune");
        assertTrue(wd.check().isEmpty());
    }

    @Test
    public void law2_watchdogWallClockCap() {
        PhaseWatchdog wall = new PhaseWatchdog(60_000, 100); // wall cap 100ms, no-progress never
        LawEnforcer.setWatchdog(wall);
        wall.begin("grow");
        sleep(300);                                          // dribbling progress would keep it alive; cap ends it
        wall.progress("grow");                               // even with progress, wall clock rules
        List<PhaseWatchdog.Cancel> cancels = wall.check();
        assertEquals(1, cancels.size());
        assertTrue(cancels.get(0).reason.contains("wall clock"));
    }

    // ── LAW 3: loopback bind guard ───────────────────────────

    @Test
    public void law3_loopbackGuard() {
        assertTrue(LoopbackGuard.isLoopback("127.0.0.1"));
        assertTrue(LoopbackGuard.isLoopback("localhost"));
        assertTrue(LoopbackGuard.isLoopback("::1"));
        assertTrue(LoopbackGuard.isLoopback("127.5.5.5"));   // any 127/8
        assertFalse(LoopbackGuard.isLoopback("192.168.1.10"));
        assertFalse(LoopbackGuard.isLoopback("0.0.0.0"));
        assertFalse(LoopbackGuard.isLoopback(null));
        boolean threw = false;
        try {
            LoopbackGuard.assertLoopback(new java.net.InetSocketAddress("192.168.1.10", 8899));
        } catch (IllegalStateException expected) { threw = true; }
        assertTrue(threw, "non-loopback bind must be refused");
        LoopbackGuard.assertLoopback(new java.net.InetSocketAddress("127.0.0.1", 8899)); // ok, no throw
    }

    // ── LAW 4: cellular staging (3-small / 2-resident cap; embed exempt) ──

    @Test
    public void law4_stagingCapAndEmbedExempt() {
        int saved = LawEnforcer.residentCount();
        LawEnforcer.setGameRunning(false);
        AdmissionControl.cpuOverride = 20;                   // don't flake on ambient CPU load
        try {
            LawEnforcer.residencyAcquire(2 - saved);         // house full (cap 2)
            assertEquals(2, LawEnforcer.residentCount());
            AdmissionControl.Verdict third =
                    LawEnforcer.admit("qwen2.5:0.5b", AdmissionControl.Kind.INFER, 8000);
            assertFalse(third.allowed, "3rd small at cap 2 must wait");
            assertTrue(third.reason.contains("resident cap"));

            // embed is exempt from the residency cap
            AdmissionControl.Verdict embed =
                    LawEnforcer.admit("nomic-embed-text:latest", AdmissionControl.Kind.EMBED, 8000);
            assertTrue(embed.allowed, "embed jobs are residency-exempt (LAW 4)");

            LawEnforcer.residencyRelease(1);                 // one resident leaves
            assertEquals(1, LawEnforcer.residentCount());
            AdmissionControl.Verdict third2 =
                    LawEnforcer.admit("qwen2.5:0.5b", AdmissionControl.Kind.INFER, 8000);
            assertTrue(third2.allowed, "slot freed → 3rd small admits");
        } finally {
            int cur = LawEnforcer.residentCount();           // restore exactly
            if (cur > saved) LawEnforcer.residencyRelease(cur - saved);
            LawEnforcer.setGameRunning(false);
            AdmissionControl.cpuOverride = null;
        }
    }

    // ── LAW 5: admission control ─────────────────────────────

    @Test
    public void law5_lowRamRefusal() {
        LawEnforcer.setGameRunning(false);
        AdmissionControl.cpuOverride = 20;                   // don't flake on ambient CPU load
        // llama3.2:3b needs 2600 (footprint) + 1500 (floor) = 4100MB free
        AdmissionControl.Verdict v = LawEnforcer.admit("llama3.2:3b", AdmissionControl.Kind.LOAD, 2000);
        assertFalse(v.allowed, "RAM below footprint+floor must deny");
        assertTrue(v.reason.contains("ram"));
        AdmissionControl.Verdict ok = LawEnforcer.admit("qwen2.5:0.5b", AdmissionControl.Kind.INFER, 8000);
        assertTrue(ok.allowed, "healthy RAM admits small tier");
        AdmissionControl.cpuOverride = null;
        // BUSY degradation (law 7): denial when free ram < 512mb reports busy
        AdmissionControl.Verdict starved = AdmissionControl.decide(
                "tinyllama:1.1b", AdmissionControl.Kind.INFER, false, 0, 400);
        assertTrue(AdmissionControl.shouldRejectAsBusy(starved), "starved host → BUSY not crash");
    }

    @Test
    public void law5_policyGates() {
        assertFalse(ModelPolicy.checkModel("gpt-oss:120b").allowed, "banned model denied");
        assertFalse(ModelPolicy.checkModel("my-totally-cloud-model").allowed, "banned substring denied");
        assertFalse(ModelPolicy.checkModel("totally-unknown-model").allowed, "unknown denied (default deny)");
        assertFalse(ModelPolicy.checkModel(null).allowed, "null denied");
        assertTrue(ModelPolicy.checkModel("tinyllama:1.1b").allowed, "known small allowed");
    }

    @Test
    public void law5_cpuCeilingAndGamePriority() {
        LawEnforcer.setGameRunning(false);
        AdmissionControl.cpuOverride = 95;                   // same-package test hook
        AdmissionControl.Verdict hot = LawEnforcer.admit("tinyllama:1.1b", AdmissionControl.Kind.INFER, 8000);
        assertFalse(hot.allowed, ">85% CPU denies");
        assertTrue(hot.reason.contains("ceiling"));

        AdmissionControl.cpuOverride = 20;
        LawEnforcer.setGameRunning(true);
        AdmissionControl.Verdict heavy = LawEnforcer.admit("llama3.2:3b", AdmissionControl.Kind.LOAD, 8000);
        assertFalse(heavy.allowed, "game running blocks M-tier LOAD (L5c game priority)");
        AdmissionControl.Verdict small = LawEnforcer.admit("tinyllama:1.1b", AdmissionControl.Kind.INFER, 8000);
        assertTrue(small.allowed, "S-tier INFER passes while game runs");

        // restricted tier waits for game idle too (deepseek behaviour)
        AdmissionControl.Verdict restricted =
                LawEnforcer.admit("deepseek-r1:1.5b", AdmissionControl.Kind.INFER, 8000);
        assertFalse(restricted.allowed, "restricted tier also defers to the game");

        LawEnforcer.setGameRunning(false);
        AdmissionControl.cpuOverride = null;
    }

    // ── LAW 6: hexflow ledger ────────────────────────────────

    @Test
    public void law6_ledgerChainVerifiesAndTamperFlagged() throws Exception {
        Path ledgerFile = tempDir.resolve("test_ledger.jsonl");
        Files.deleteIfExists(ledgerFile);
        HexflowLedger ledger = new HexflowLedger(ledgerFile, 10_000);

        String h1 = ledger.append("mine", HexflowLedger.BEGIN);
        ledger.append("mine", HexflowLedger.PROGRESS, "scan 3 repos");
        String h3 = ledger.append("mine", HexflowLedger.END);

        assertEquals(3, ledger.seq());
        assertFalse(h1.isEmpty(), "appends produce hashes");
        assertFalse(h3.isEmpty(), "appends produce hashes");
        assertTrue(ledger.verify().isEmpty(), "fresh chain must verify clean");

        List<String> lines = Files.readAllLines(ledgerFile);
        assertEquals(3, lines.size());
        assertTrue(lines.get(1).contains("\"prev\":\"" + h1 + "\""), "line2.prev == line1.hash");

        // Tamper-evidence: rewrite line 2's event → verify() flags the break
        lines.set(1, lines.get(1).replace("\"event\":\"progress\"", "\"event\":\"cancel\""));
        Files.write(ledgerFile, lines);
        HexflowLedger tamperedReader = new HexflowLedger(ledgerFile, 10_000);
        assertFalse(tamperedReader.verify().isEmpty(), "tampering must break the chain");
    }

    @Test
    public void law6_ledgerSeqContinuesAcrossRestart() throws Exception {
        Path f = tempDir.resolve("restart_ledger.jsonl");
        Files.deleteIfExists(f);
        HexflowLedger a = new HexflowLedger(f, 10_000);
        a.append("deploy", HexflowLedger.BEGIN);
        a.append("deploy", HexflowLedger.END);
        long seqA = a.seq();
        assertEquals(2, seqA);

        HexflowLedger b = new HexflowLedger(f, 10_000);      // "restart" re-read from disk
        assertEquals(seqA, b.seq(), "seq continues across restart");
        b.append("tune", HexflowLedger.BEGIN);
        assertEquals(seqA + 1, b.seq());
        assertTrue(b.verify().isEmpty(), "continued chain still verifies");
    }

    // ── LAW 7: graceful degradation ──────────────────────────

    @Test
    public void law7_statusJsonAlwaysValid() {
        LawEnforcer.setGameRunning(false);
        String json = LawEnforcer.lawsStatusJson();
        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertTrue(json.contains("\"law1_single_instance\""));
        assertTrue(json.contains("\"law2_watchdog\""));
        assertTrue(json.contains("\"law3_loopback_only\""));
        assertTrue(json.contains("\"law4_staging\""));
        assertTrue(json.contains("\"law5_admission\""));
        assertTrue(json.contains("\"law6_hexflow\""));
        assertTrue(json.contains("\"law7_degraded_ok\""));
    }

    @Test
    public void law7_missingPolicyDegradesToDefaults() {
        String prev = System.getProperty("com.aigen.sims.modelpolicy", "");
        try {
            System.setProperty("com.aigen.sims.modelpolicy",
                    tempDir.resolve("definitely_missing.json").toAbsolutePath().toString());
            ModelPolicy.reload();                            // must NOT throw (law 7)
            assertEquals("embedded-defaults", ModelPolicy.source().split(" \\(")[0]);
            assertTrue(ModelPolicy.checkModel("qwen2.5:0.5b").allowed, "defaults keep live tables");
            assertFalse(ModelPolicy.checkModel("some-cloud-model").allowed, "defaults keep bans");
        } finally {
            System.setProperty("com.aigen.sims.modelpolicy", prev);
            ModelPolicy.reload();                            // restore temp policy
        }
    }

    // ── no ollama spawns (standing priority) ─────────────────

    @Test
    public void noOllamaOrAnyProcessSpawnedByLawsCore() {
        // Structural guarantee: not one laws class references ProcessBuilder or
        // Runtime/exec — spawn code cannot exist here, so it cannot run.
        assertFalse(referencesProcessBuilder(AdmissionControl.class));
        assertFalse(referencesProcessBuilder(LawEnforcer.class));
        assertFalse(referencesProcessBuilder(ModelPolicy.class));
        assertFalse(referencesProcessBuilder(SingleInstanceLock.class));
        assertFalse(referencesProcessBuilder(PhaseWatchdog.class));
        assertFalse(referencesProcessBuilder(HexflowLedger.class));
        assertFalse(referencesProcessBuilder(LoopbackGuard.class));
        assertFalse(referencesProcessBuilder(SystemProbe.class));
    }

    private static boolean referencesProcessBuilder(Class<?> c) {
        try (java.io.InputStream in = c.getResourceAsStream(c.getSimpleName() + ".class")) {
            if (in == null) return false;
            String cp = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
            return cp.contains("java/lang/ProcessBuilder")
                    || (cp.contains("java/lang/Runtime") && cp.contains("exec"));
        } catch (Exception e) {
            return false;
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}