package com.aigen.sims.scheduler;

import com.aigen.sims.commander.AegisCommander;
import com.aigen.sims.commander.PipelineMonitor;
import com.aigen.sims.deploy.DeployOrchestrator;
import com.aigen.sims.deploy.GateKeeper;
import com.aigen.sims.gui.GuiGardener;
import com.aigen.sims.lora.AdapterRegistry;
import com.aigen.sims.lora.LoRATuner;
import com.aigen.sims.mining.CodeMinerOrchestrator;
import com.aigen.sims.mining.SuggestionRegistry;

/**
 * PipelineScheduler -- 2026-07-28 Architect gist 3.4: "Replace hardcoded 19:00/20:00/21:00/22:00
 * with dependency-driven triggers: mining complete -> deploy; deploy complete -> tune; tune
 * complete -> grow; any phase failure -> Aegis notified."
 *
 * AutoLoop.runPipelineCycle() today calls all 4 phases back-to-back inside one try/catch: if
 * mining throws, deploy/tune/grow are silently skipped for the whole cycle with no per-phase
 * record of what actually ran. This wires the SAME real phase classes through EventBus instead:
 * each phase's real completion triggers the next, and a failure in one phase is a real, visible
 * event (phase_failed) rather than an uncaught exception that kills the rest of the cycle.
 */
public class PipelineScheduler {
    private final EventBus bus = new EventBus();
    private final String home, suggestionsPath, repoPath;
    private final StringBuilder cycleLog = new StringBuilder();
    private final PipelineMonitor monitor;
    // 2026-08-02: shared across every cycle -- both classes are pure in-memory with no disk
    // persistence, so a fresh instance per cycle (the original code) silently wiped proposal/adapter
    // history every 60 minutes. One long-lived instance for this scheduler's whole lifetime, and
    // WebDashboard reads the SAME instances via gateKeeper()/adapterRegistry() below.
    private final GateKeeper gateKeeper;
    private final AdapterRegistry adapterRegistry = new AdapterRegistry();

    public PipelineScheduler(String home) {
        this.home = home;
        this.suggestionsPath = home + "/suggestions";
        this.repoPath = home + "/SIMS1337";
        this.gateKeeper = new GateKeeper(repoPath);
        this.monitor = new PipelineMonitor(new SuggestionRegistry(suggestionsPath),
            home + "/pipeline_monitor_state.txt");
        wire();
    }

    public EventBus bus() { return bus; }
    public String cycleLog() { return cycleLog.toString(); }
    public GateKeeper gateKeeper() { return gateKeeper; }
    public AdapterRegistry adapterRegistry() { return adapterRegistry; }
    public String home() { return home; }
    public String repoPath() { return repoPath; }
    public String suggestionsPath() { return suggestionsPath; }

    private void wire() {
        bus.subscribe("mine_complete", payload -> runDeploy());
        bus.subscribe("deploy_complete", payload -> runTune());
        bus.subscribe("tune_complete", payload -> runGrow());
        bus.subscribe("grow_complete", payload -> {
            cycleLog.append(monitor.report()).append("\n");
        });
        bus.subscribe("phase_failed", payload -> cycleLog.append("FAILED: ").append(payload).append("\n"));
    }

    /** Real strategic priority, same as Step A: read before the cycle starts, not guessed. */
    public com.aigen.sims.commander.Strategy currentStrategy() {
        SuggestionRegistry reg = new SuggestionRegistry(suggestionsPath);
        return new AegisCommander(reg, home + "/strategy.json").generateStrategy();
    }

    public void runCycle() {
        cycleLog.setLength(0);
        runMine();
    }

    private void runMine() {
        try {
            CodeMinerOrchestrator miner = new CodeMinerOrchestrator(home + "/AIGEN_SYS/repos", suggestionsPath);
            var report = miner.runMiningCycle();
            cycleLog.append("mine: ").append(report.toEmailString()).append("\n");
            bus.publish("mine_complete", report);
        } catch (Exception e) {
            bus.publish("phase_failed", "mine: " + e.getMessage());
        }
    }

    private void runDeploy() {
        try {
            SuggestionRegistry reg = new SuggestionRegistry(suggestionsPath);
            DeployOrchestrator deployer = new DeployOrchestrator(repoPath, gateKeeper);
            var report = deployer.runDeployCycle(reg, repoPath);
            cycleLog.append("deploy: ").append(report.toEmailString()).append("\n");
            bus.publish("deploy_complete", report);
        } catch (Exception e) {
            bus.publish("phase_failed", "deploy: " + e.getMessage());
        }
    }

    private void runTune() {
        try {
            LoRATuner tuner = new LoRATuner(adapterRegistry);
            var report = tuner.runTuningCycle();
            cycleLog.append("tune: ").append(report.toEmailString()).append("\n");
            bus.publish("tune_complete", report);
        } catch (Exception e) {
            bus.publish("phase_failed", "tune: " + e.getMessage());
        }
    }

    private void runGrow() {
        try {
            GuiGardener gardener = new GuiGardener();
            cycleLog.append("grow: ").append(gardener.getComponentMapString()).append("\n");
            bus.publish("grow_complete", gardener);
        } catch (Exception e) {
            bus.publish("phase_failed", "grow: " + e.getMessage());
        }
    }
}
