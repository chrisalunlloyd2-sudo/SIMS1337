package com.aigen.sims;

/**
 * AutoLoop — Continuous autonomous pipeline runner.
 * Runs the 5-phase pipeline every 60 minutes in a loop.
 * No JavaFX dependency. Runs on bare Termux.
 */
public class AutoLoop {
    public static void main(String[] args) {
        long intervalMs = 60 * 60 * 1000; // 60 minutes default
        if (args.length > 0) {
            try { intervalMs = Long.parseLong(args[0]) * 60 * 1000; } catch (Exception e) {}
        }

        System.out.println("🔄 SIMS1337 — Autonomous Loop Started");
        System.out.println("   Interval: " + (intervalMs / 60000) + " minutes");
        System.out.println("   Phases: mine → deploy → tune → grow\n");

        int cycle = 0;
        while (true) {
            cycle++;
            long start = System.currentTimeMillis();
            System.out.println("─── Cycle #" + cycle + " [" + new java.util.Date() + "] ───");

            try {
                runPipelineCycle();
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

    private static void runPipelineCycle() {
        String home = System.getProperty("user.home");
        com.aigen.sims.mining.CodeMinerOrchestrator miner =
            new com.aigen.sims.mining.CodeMinerOrchestrator(home + "/AIGEN_SYS/repos", home + "/suggestions");
        System.out.println("📊 [19:00] " + miner.runMiningCycle().toEmailString());
        com.aigen.sims.mining.SuggestionRegistry reg =
            new com.aigen.sims.mining.SuggestionRegistry(home + "/suggestions");
        com.aigen.sims.deploy.DeployOrchestrator deployer =
            new com.aigen.sims.deploy.DeployOrchestrator(home + "/SIMS1337");
        System.out.println("🚀 [20:00] " + deployer.runDeployCycle(reg, home + "/SIMS1337").toEmailString());
        com.aigen.sims.lora.AdapterRegistry ar = new com.aigen.sims.lora.AdapterRegistry();
        com.aigen.sims.lora.LoRATuner tuner = new com.aigen.sims.lora.LoRATuner(ar);
        System.out.println("🔧 [21:00] " + tuner.runTuningCycle().toEmailString());
        com.aigen.sims.gui.GuiGardener gardener = new com.aigen.sims.gui.GuiGardener();
        System.out.println("🗺️ [22:00] " + gardener.getComponentMapString());
    }
}
