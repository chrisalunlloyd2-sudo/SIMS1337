package com.aigen.sims.laws;

/**
 * GODHAND-3 (t_22bb44a7) — free RAM / CPU probing and residency bookkeeping
 * shared by the admission core. No external dependencies; OS-level probes are
 * best-effort with conservative fallbacks (law 7: never fail open).
 */
final class SystemProbe {

    /** Free RAM in MB as seen by the OS for THIS process's admission maths.
     *  On Windows uses process-free memory (commit charge headroom); fallbacks
     *  to JVM-internal estimate. Always >= 0. */
    static long freeRamMb() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean)
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            // freePhysicalMemorySize is what ops_health.py's psutil mirror uses
            long freeBytes = os.getFreePhysicalMemorySize();
            if (freeBytes > 0) return Math.max(0, freeBytes / (1024L * 1024L));
        } catch (Throwable ignored) { /* fall through */ }
        Runtime rt = Runtime.getRuntime();
        long freeJvmMb = (rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / (1024L * 1024L);
        return Math.max(0, freeJvmMb + 1024); // assume ≥1 GB outside the JVM
    }

    /** CPU load 0..100 of the last short window; -1 when the OS bean can't tell. */
    static int cpuLoadPercent() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean)
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double load = os.getSystemCpuLoad();
            if (load < 0) return -1; // jvm does not support
            return (int) Math.round(load * 100.0);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /** Number of available processors. */
    static int cores() {
        return Runtime.getRuntime().availableProcessors();
    }

    private SystemProbe() { /* static-only */ }
}