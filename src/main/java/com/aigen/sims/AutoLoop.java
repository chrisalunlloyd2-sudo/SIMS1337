package com.aigen.sims;

/**
 * AutoLoop — Continuous autonomous pipeline runner.
 * Runs the 5-phase pipeline every 60 minutes in a loop.
 * No JavaFX dependency. Runs on bare Termux.
 *
 * 2026-08-02: was calling all 4 phases directly, back-to-back, inside one try/catch --
 * PipelineScheduler + EventBus (Architect gist 3.4, built 2026-07-28) existed and were verified in
 * isolation, but this real entrypoint was never updated to use them, so none of that work -- the
 * dependency-driven phase triggers, the per-phase failure events, NyxGate's wiring into deploy, or
 * WebDashboard -- actually ran when Chris runs `java AutoLoop`. Now wired for real.
 */
public class AutoLoop {
    public static void main(String[] args) {
        long intervalMs = 60 * 60 * 1000; // 60 minutes default
        if (args.length > 0) {
            try { intervalMs = Long.parseLong(args[0]) * 60 * 1000; } catch (Exception e) {}
        }

        String home = System.getProperty("user.home");
        com.aigen.sims.scheduler.PipelineScheduler scheduler = new com.aigen.sims.scheduler.PipelineScheduler(home);
        com.aigen.sims.web.WebDashboard dashboard = new com.aigen.sims.web.WebDashboard(scheduler, 8899);
        try {
            dashboard.start();
        } catch (Exception e) {
            System.err.println("⚠️  WebDashboard failed to start (continuing headless): " + e.getMessage());
        }

        System.out.println("🔄 SIMS1337 — Autonomous Loop Started");
        System.out.println("   Interval: " + (intervalMs / 60000) + " minutes");
        System.out.println("   Phases: mine → deploy → tune → grow (dependency-triggered via EventBus)\n");

        int cycle = 0;
        while (true) {
            cycle++;
            long start = System.currentTimeMillis();
            System.out.println("─── Cycle #" + cycle + " [" + new java.util.Date() + "] ───");

            try {
                scheduler.runCycle();
                System.out.println(scheduler.cycleLog());
            } catch (Exception e) {
                System.err.println("⚠️  Cycle #" + cycle + " error: " + e.getMessage());
            }

            long elapsed = System.currentTimeMillis() - start;
            long sleep = intervalMs - elapsed;
            if (sleep > 0) {
                System.out.println("💤 Sleeping " + (sleep / 60000) + " min until next cycle...\n");
                try { Thread.sleep(sleep); } catch (InterruptedException e) { break; }
            }
        }
    }
}
