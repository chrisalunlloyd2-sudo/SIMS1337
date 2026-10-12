package com.aigen.sims.laws;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * GODHAND-3 (t_22bb44a7) — LAW 1: single-instance lock.
 *
 * A file lock (AIGEN_SYS/config/godhand.lock) is held open+locked for the
 * whole process lifetime. A second instance's tryAcquire fails immediately
 * and exits (the JVM shuts down cleanly). OS releases the lock on crash —
 * no stale-lock problem, unlike PID-file schemes.
 *
 * Lock file lives next to model_policy.json so it inherits the same dir;
 * overridable with -Dcom.aigen.sims.lockfile=... (tests use temp dirs).
 */
public final class SingleInstanceLock {

    private static FileChannel channel;
    private static FileLock lock;

    private SingleInstanceLock() { /* static-only */ }

    public static Path lockFile() {
        String p = System.getProperty("com.aigen.sims.lockfile");
        if (p != null && !p.isEmpty()) return Paths.get(p);
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, "AIGEN_SYS", "config", "godhand.lock");
    }

    /**
     * Law 1 gate. MUST be called before any port binding / backend init.
     *
     * @return true if THIS instance holds the lock and may run;
     *         false if another instance is alive (caller must exit).
     */
    public static synchronized boolean tryAcquire() {
        if (lock != null && lock.isValid()) return true; // already ours
        try {
            Path path = lockFile();
            Files.createDirectories(path.getParent());
            channel = new RandomAccessFile(path.toFile(), "rw").getChannel();
            lock = channel.tryLock();                            // non-blocking
            if (lock == null) {
                channel.close();
                channel = null;
                return false;                                    // LAW 1: another instance holds it
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> releaseQuietly(), "godhand-lock-release"));
            return true;
        } catch (Exception e) {
            releaseQuietly();
            // Law 7: degrade — lock failure must not take the instance down,
            // but treat as NOT held-uniquely only for lock-IO errors; a clean
            // contention result (already false) never reaches here.
            return false;
        }
    }

    /** Test/support: voluntarily drop the lock. */
    public static synchronized void release() {
        releaseQuietly();
    }

    public static boolean isHeld() { return lock != null && lock.isValid(); }

    private static void releaseQuietly() {
        try {
            if (lock != null) { lock.release(); lock = null; }
        } catch (IOException ignored) { }
        try {
            if (channel != null) { channel.close(); channel = null; }
        } catch (IOException ignored) { }
    }
}