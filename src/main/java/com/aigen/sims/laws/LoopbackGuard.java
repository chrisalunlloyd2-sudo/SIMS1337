package com.aigen.sims.laws;

/**
 * GODHAND-3 (t_22bb44a7) — LAW 3: loopback bind guard for the dashboard server.
 *
 * GodHandApp already binds 127.0.0.1 (loopback-only, GODHAND-1). This guard
 * makes that invariant explicit and testable: every bind used for dashboards
 * must pass assertLoopback(InetSocketAddress).
 *
 * The 127.0.0.1:1338 operator exception is NOT a dashboard binding: nothing
 * in GodHand binds or serves on 1338 — it stays unrestricted for the ops
 * layer (GODHAND-2C owns any re-add there). This class therefore does not
 * special-case 1338; it simply never touches it.
 */
public final class LoopbackGuard {

    private LoopbackGuard() { /* static-only */ }

    /** True when host is the IPv4/IPv6 loopback ("127.0.0.1", "localhost", "::1"). */
    public static boolean isLoopback(String host) {
        if (host == null) return false;
        return "127.0.0.1".equals(host)
                || "localhost".equalsIgnoreCase(host)
                || "::1".equals(host)
                || host.startsWith("127.");
    }

    /**
     * LAW 3 gate: throw when a server would bind a non-loopback interface.
     * Called before every HttpServer.create in wired callers.
     */
    public static void assertLoopback(java.net.InetSocketAddress addr) {
        String host = addr == null ? null : addr.getHostString();
        if (!isLoopback(host)) {
            throw new IllegalStateException("LAW3 violation: refusing to bind non-loopback interface " + host);
        }
    }
}