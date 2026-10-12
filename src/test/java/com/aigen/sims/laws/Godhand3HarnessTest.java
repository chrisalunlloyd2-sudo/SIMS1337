package com.aigen.sims.laws;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GODHAND-3 (t_22bb44a7) harness — focus-topic tests for the three gap fixes:
 *   1. L2 watchdog poller: check() must actually be POLLED, cancels must be
 *      recorded to the ledger, and the poller must survive a throwing
 *      watchdog (never dies mid-pipeline).
 *   2. attachable ledger: LawEnforcer.record() goes to the attached ledger
 *      without taking the L1 single-instance lock (the GodHand dashboard
 *      instance holds that lock — a second attempt would brick both).
 *   3. L4 residency: release() must never drive the counter below zero.
 *
 * Hermeticity: every test injects its own temp-dir ledger via the package-
 * private setLedgerForTest seam (never the real production ledger) and stops
 * the shared poller between tests — LawEnforcer static state is JVM-global
 * and surefire runs all classes in one fork.
 */
public class Godhand3HarnessTest {

    @BeforeEach
    void resetLawsStatics() {
        // One shared poller thread serves all tests; stop it between tests so
        // the 1ms-cadence test starts from a clean, non-firing state.
        LawEnforcer.stopWatchdogPollerForTest();
        LawEnforcer.setWatchdog(new PhaseWatchdog()); // fresh default-clock instance
    }

    @AfterAll
    static void restoreDefaults() {
        LawEnforcer.stopWatchdogPollerForTest();
        LawEnforcer.setLedgerForTest(null);
        LawEnforcer.setWatchdog(new PhaseWatchdog());
    }

    @Test
    void harness_attachLedgerFreeAndReceivesRecords() {
        // Direct inject (test package-private seam): record() must reach the
        // attached ledger without ever touching the L1 single-instance lock.
        LawEnforcer.setLedgerForTest(null);
        Path pathBefore = SingleInstanceLock.lockFile();

        Path tmpDir = assertDoesNotThrow(() -> Files.createTempDirectory("g3harness"));
        Path ledgerFile = tmpDir.resolve("harness_ledger.jsonl");
        HexflowLedger shared = assertDoesNotThrow(() -> new HexflowLedger(ledgerFile));
        LawEnforcer.setLedgerForTest(shared);

        assertDoesNotThrow(() -> LawEnforcer.record("phase-test", "begin", "lock-free attach"));
        assertDoesNotThrow(() -> LawEnforcer.record("phase-test", "end", "lock-free attach"));

        List<Long> broken = assertDoesNotThrow(shared::verify);
        assertTrue(broken.isEmpty(), "ledger chain intact after records");
        assertEquals(pathBefore, SingleInstanceLock.lockFile(),
                "record()/verify must not recreate or touch the L1 lock");

        LawEnforcer.setLedgerForTest(null);
    }


    @Test
    void harness_watchdogPollerDetectsStallAndRecordsToLedger() throws Exception {
        Path tmpDir = Files.createTempDirectory("g3harness");
        Path ledgerFile = tmpDir.resolve("harness_ledger.jsonl");
        HexflowLedger shared = new HexflowLedger(ledgerFile);
        LawEnforcer.setLedgerForTest(shared);

        // Tiny watchdog: any running phase stalls instantly (noProgressMs = 1ms).
        PhaseWatchdog tiny = new PhaseWatchdog(1L, 90 * 60_000L);
        LawEnforcer.setWatchdog(tiny);
        tiny.begin("mine");

        LawEnforcer.watchdogPollMs = 1L; // seam: 1ms poll cadence
        LawEnforcer.startWatchdogPoller();

        boolean recorded = false;
        for (int i = 0; i < 200 && !recorded; i++) {
            for (String line : rec(shared)) {
                if (line.contains("\"event\":\"cancel\"") && line.contains("\"phase\":\"mine\"")) {
                    recorded = true;
                    break;
                }
            }
            if (!recorded) Thread.sleep(25);
        }
        assertTrue(recorded, "poller must fire L2 and record the cancel to the ledger");
        assertTrue(shared.verify().isEmpty(), "ledger chain intact after poller writes");
        assertEquals(1, tiny.drainCancels().size(), "cancel consumed exactly once");
    }

    /** The ledger has no read-back API; verify via the file itself. */
    private static List<String> rec(HexflowLedger l) throws Exception {
        if (!Files.exists(l.file())) return List.of();
        return Files.readAllLines(l.file());
    }

    @Test
    void harness_pollerSurvivesWatchdogThrowing() throws Exception {
        Path tmpDir = Files.createTempDirectory("g3harness2");
        HexflowLedger shared = new HexflowLedger(tmpDir.resolve("harness_ledger2.jsonl"));
        LawEnforcer.setLedgerForTest(shared);

        // Error injection: PhaseWatchdog is final (no subclass), so a null
        // watchdog NPE inside check() exercises the same Throwable path.
        LawEnforcer.setWatchdog(null);
        LawEnforcer.watchdogPollMs = 5L;
        LawEnforcer.startWatchdogPoller();

        Thread.sleep(400); // ~80 iterations of absorbed NPEs
        assertTrue(LawEnforcer.watchdogPollerAliveForTest(),
                "poller must survive a throwing/failing watchdog");
    }

    @Test
    void harness_residencyReleaseNeverBelowZero() {
        LawEnforcer.residencyRelease(1); // release from zero...
        assertEquals(0, LawEnforcer.residentCount(), "release must floor at zero");
        LawEnforcer.residencyAcquire(2);
        LawEnforcer.residencyRelease(5); // over-release
        assertEquals(0, LawEnforcer.residentCount(), "over-release must floor at zero");
        LawEnforcer.residencyRelease(0); // no-op release
        assertEquals(0, LawEnforcer.residentCount(), "zero-weight release is a no-op");
    }
}