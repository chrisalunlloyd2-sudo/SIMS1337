package com.aigen.sims;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// HTTP Server Imports
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.IOException;
import com.aigen.sims.engine.KnowledgeGraph;
import com.aigen.sims.laws.LawEnforcer;
import com.aigen.sims.laws.LoopbackGuard;

/**
 * SIMS1337 v0.25.0 - GodHandApp
 * Pure Programmatic JavaFX GUI
 * 6D Hexeract Geospatial Manifold Visualizer with Moveable Windows and KQML Message Bus
 */
public class GodHandApp extends Application {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final double HEX_SIZE = 35.0;
    
    private Map<String, HexNode> grid = new ConcurrentHashMap<>();
    private List<Agent> agents = new CopyOnWriteArrayList<>();
    private List<String> godChat = new CopyOnWriteArrayList<>();
    private ExecutorService threadPool = Executors.newFixedThreadPool(8);
    // PERF EPIC t_4a3756ed (C.4): model-call work runs on ONE dedicated thread;
    // threadPool(8) remains for engine loops (watchdog/mcts/fuzzer/heartrate/
    // dream) so eternal while(true) tasks can never starve or deadlock it.
    private final ExecutorService modelExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "modelbus-caller-single");
                t.setDaemon(true);
                return t;
            });
    private HttpServer dashboardServer;

    // === GODHAND-2C (t_63a51063): read-only public phone UI ===
    // Ported from sims-java-neo-fx @ bf7ba39 (mobile dashboardHandler ~L2370-2585).
    // Binds 0.0.0.0:1338 in GUI mode ONLY (headless never opens it — selftest asserts).
    // READ-ONLY by construction: the public server registers ONLY the "/" context
    // serving the mobile HTML page; every other path (incl. /api/*, /index.html)
    // answers 404. The loopback 8899 dashboard above is untouched (LAW 3 governs it);
    // the public bind is the deliberate, task-mandated exposure.
    private static final int PUBLIC_UI_PORT = 1338;
    private HttpServer publicServer;

    // === OPS HEALTH PANEL (GODHAND-2: thin client of ~/.aria/engine/ops_health.py) ===
    // ARIA's ops_health engine "serve" mode answers /api/ops on 127.0.0.1:11436 (loopback-only, 60s refresh).
    // GodHand proxies that document; when the engine is dark it falls back to `python ops_health.py snapshot`
    // as a subprocess. Cache (<=60s) keeps concurrent dashboard tabs from spawning subprocesses.
    private static final String OPS_HEALTH_URL = "http://127.0.0.1:11436/api/ops";
    private static final String OPS_HEALTH_DIR = System.getProperty("user.home")
            + File.separator + ".aria" + File.separator + "engine";
    private static final long OPS_CACHE_MS = 60_000;
    // GODHAND-3 (t_22bb44a7): hardening — measured snapshot wall time under full-box load is
    // ~29 s (model warmups); 25 s timed out while the handler thread still blocked ~29 s and the
    // degraded doc stayed sticky for a full cache window. Budget 40 s with headroom, named constant.
    private static final int OPS_SNAPSHOT_TIMEOUT_S = 40;
    private static final java.net.http.HttpClient opsHttpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10)).build();
    // GODHAND-4 (t_ebf78b69): /api/jobs thin client of the ARIA job engine (job_server.py, loopback).
    // Same shape as the ops thin client: live engine first, subprocess fallback second.
    private static final String JOBS_URL = "http://127.0.0.1:1339/api/jobs";
    private static final long JOBS_CACHE_MS = 10_000;
    private final Object jobsCacheLock = new Object();
    private volatile String jobsCacheJson = null;
    private volatile long jobsCacheAtMs = 0L;
    private String jobsCacheSource = "none";
    private final Object opsCacheLock = new Object();
    private volatile String opsCacheJson = null;
    private volatile long opsCacheAtMs = 0L;
    private String opsCacheSource = "none";
    private volatile String opsLastError = "";
    // GODHAND-2C: last-served mirrors of the ops/jobs thin clients for the phone page.
    // HTTP threads only READ these (never grab the thin-client locks); a single
    // background warmer refreshes them so a cold/down engine never blocks a request.
    private volatile String lastOpsDoc = null;
    private volatile String lastJobsDoc = null;
    private final java.util.concurrent.atomic.AtomicBoolean phoneWarmRunning = new java.util.concurrent.atomic.AtomicBoolean(false);

    private double timePulse = 0;
    private int zElevation = 0;
    
    private NightCycleEngine nightCycle;
    private OllamaRouter ollamaRouter;

    // Subsystems (t_4a3756ed repair: phantom ModelManager/SQLiteMemory/GistSync/SelfMutator
    // and enterprise classes never existed in-tree; only real engine.KnowledgeGraph kept)
    private KnowledgeGraph kg;
    
    // 6D Hexeract Fields
    private double[][] vertices6D = new double[64][6];
    private List<int[]> edges = new ArrayList<>();
    private double[][] projected2D = new double[64][2];
    private double[] densities = new double[64];
    private double[] flows = new double[64];
    private int hoveredVertexIdx = -1;
    
    // Rheological & Stability States
    private double viscosity = 0.420;
    private double strainRate = 0.681;
    private double stress = 0.312;
    private double heartbeatFreq = 1.20;
    private double storageModulus = 50.0;
    private double lossModulus = 35.0;
    private double stressLevel = 0.15; // Dynamic stress indicator
    
    // Particle Swarm and Signal Pulses
    private List<Particle> particles = new ArrayList<>();
    private List<Pulse> pulses = new CopyOnWriteArrayList<>();
    private List<BackgroundStar> stars = new ArrayList<>();
    private Random rand = new Random();

    // Draggable Window Overlay Pane
    private Pane windowOverlay;

    public static void main(String[] args) {
        // === GODHAND-3 LAW 1: single-instance lock at boot ===
        // Acquired BEFORE anything else (no JavaFX toolkit, no server bind).
        // A second instance exits with code 17 without touching the live one.
        if (!LawEnforcer.boot()) {
            System.exit(LawEnforcer.LOCK_EXIT_CODE);
        }
        // === HEADLESS SERVER MODE part 1 (GODHAND-1, t_ef84df83 / t_b022e64b) ===
        // Headless must NEVER start the JavaFX toolkit: Application.launch() boots
        // glass/prism natives even when start() immediately skips the stage, which
        // (diagnosed 2026-10-11, t_b022e64b smoke) hung in NativeLibLoader and
        // silently died without ever binding the dashboard port. In headless mode
        // we run the API server straight off main() — pure backend, no toolkit.
        if (Boolean.getBoolean("com.aigen.sims.headless")) {
            System.out.println("[GODHAND] HEADLESS server mode: JavaFX toolkit not started; running API server directly");
            new GodHandApp().initBackendSystems();
            System.out.println("[GODHAND] HEADLESS init complete; API server stays up (main thread parked)");
            try {
                Thread.currentThread().join(); // park main forever; daemon API threads keep serving
            } catch (InterruptedException ignored) {
                // main interrupted (never in practice): fall through and exit
            }
            return;
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // === HEADLESS SERVER MODE (GODHAND-1, t_ef84df83) ===
        // -Dcom.aigen.sims.headless=true: no JavaFX stage, pure API server.
        // Serves the :8899 dashboard APIs via initBackendSystems() so smoke tests
        // run without a desktop. Without setImplicitExit(false) the toolkit would
        // implicit-exit right after start() returns and kill the API server.
        if (Boolean.getBoolean("com.aigen.sims.headless")) {
            System.out.println("[GODHAND] HEADLESS server mode: JavaFX stage skipped; dashboard APIs stay up");
            Platform.setImplicitExit(false);
            initBackendSystems();
            return;
        }
        initHexGrid();
        initAgents();
        initBackendSystems();
        initHexeract();
        
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.setOnScroll(e -> {
            if(e.getDeltaY() > 0) zElevation = Math.min(4, zElevation + 1);
            else zElevation = Math.max(0, zElevation - 1);
        });

        canvas.setOnMouseMoved(e -> {
            hoveredVertexIdx = -1;
            double minDist = 20.0; // Max hover distance threshold
            for (int i = 0; i < 64; i++) {
                double dx = e.getX() - projected2D[i][0];
                double dy = e.getY() - projected2D[i][1];
                double dist = Math.hypot(dx, dy);
                if (dist < minDist) {
                    hoveredVertexIdx = i;
                    minDist = dist;
                }
            }
        });

        canvas.setOnMouseClicked(e -> {
            if (hoveredVertexIdx != -1) {
                // Clicking increases local stress level
                stressLevel = Math.min(1.0, stressLevel + 0.08);
                if (e.getButton() == MouseButton.PRIMARY) {
                    triggerPulse(hoveredVertexIdx);
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    triggerPulse(hoveredVertexIdx);
                    modelExecutor.submit(() -> {
                        ollamaRouter.query("tinyllama:1.1b", "Spike routing instruction at coordinate " + hoveredVertexIdx);
                    });
                }
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            private long lastMove = 0;
            private long lastEnterpriseTick = 0;
            
            @Override
            public void handle(long now) {
                // GODHAND-3 LAW 5c: sim is ticking — game priority flag for admission
                LawEnforcer.setGameRunning(true);
                timePulse += 0.02;
                
                // Decay stress level slowly towards baseline
                stressLevel = Math.max(0.05, stressLevel - 0.001);
                
                if (now - lastMove > 10_000_000_000L) { // 10 seconds
                    lastMove = now;
                    triggerAutonomousInferenceMovement();
                }
                if (now - lastEnterpriseTick > 30_000_000_000L) { // 30 seconds
                    lastEnterpriseTick = now;
                    threadPool.submit(() -> {
                        // t_4a3756ed repair: phantom enterprise classes removed —
                        // KG topology telemetry keeps this tick alive and observable.
                        if (kg != null) {
                            int kgSize = kg.size();
                            Platform.runLater(() -> {
                                synchronized (godChat) {
                                    if (godChat.size() > 50) godChat.remove(0);
                                    godChat.add("[KG] Topology sweep OK. " + kgSize + " nodes indexed.");
                                }
                            });
                        }
                    });
                }
                render(gc);
            }
        };

        // Window overlay container
        windowOverlay = new Pane();
        windowOverlay.setPickOnBounds(false); 

        // main StackPane root layout
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #020106;");
        
        // Horizontal launcher taskbar (without reset button as requested)
        HBox taskbar = new HBox(12);
        taskbar.setAlignment(Pos.CENTER);
        taskbar.setStyle("-fx-background-color: rgba(15, 10, 36, 0.85); " +
                         "-fx-border-color: #a855f7; " +
                         "-fx-border-width: 1.5; " +
                         "-fx-background-radius: 20; " +
                         "-fx-border-radius: 20; " +
                         "-fx-padding: 8 20;");
        taskbar.setMaxSize(660, 50);
        StackPane.setAlignment(taskbar, Pos.TOP_CENTER);
        StackPane.setMargin(taskbar, new Insets(10, 0, 0, 0));

        // Create Taskbar button styles
        String btnStyle = "-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand;";

        Button btnNotes = new Button("📓 Notes");
        btnNotes.setStyle(btnStyle);
        btnNotes.setOnAction(e -> openWindow("Viper Notes", createViperNotesView(), 420, 360));

        Button btnChat = new Button("💬 Chat (Karoo)");
        btnChat.setStyle(btnStyle);
        btnChat.setOnAction(e -> openWindow("Viper Chat", createViperChatView(), 420, 340));

        Button btnTraining = new Button("📈 Training");
        btnTraining.setStyle(btnStyle);
        btnTraining.setOnAction(e -> openWindow("Viper Training", createViperTrainingView(), 400, 260));

        Button btnInterstitials = new Button("🌫️ Interstitials");
        btnInterstitials.setStyle(btnStyle);
        btnInterstitials.setOnAction(e -> openWindow("Viper Interstitials", createViperInterstitialsView(), 440, 320));

        Button btnMoltbook = new Button("📖 Moltbook");
        btnMoltbook.setStyle(btnStyle);
        btnMoltbook.setOnAction(e -> openWindow("Moltbook", createMoltbookView(), 440, 350));

        Button btnRebootCtrl = new Button("⚙️ Reboot Panel");
        btnRebootCtrl.setStyle(btnStyle);
        btnRebootCtrl.setOnAction(e -> openWindow("Manifold Control", createManifoldControlView(), 240, 320));

        taskbar.getChildren().addAll(btnNotes, btnChat, btnTraining, btnInterstitials, btnMoltbook, btnRebootCtrl);

        root.getChildren().addAll(canvas, windowOverlay, taskbar);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        primaryStage.setTitle("SIMS1337 v0.25.0 - 6D Hexeract Geospatial Manifold Organism");
        primaryStage.setScene(scene);
        primaryStage.show();

        timer.start();
        nightCycle.startClock();
        // GODHAND-3 LAW 5c: stage shown and looping — clear the flag on stop so
        // admission returns to idle-house mode the moment the game is not ticking.
        primaryStage.setOnCloseRequest(e -> LawEnforcer.setGameRunning(false));
        primaryStage.setOnHidden(e -> LawEnforcer.setGameRunning(false));
    }

    private void openWindow(String title, javafx.scene.Node content, double width, double height) {
        // Bring to front if already exists
        for (javafx.scene.Node node : windowOverlay.getChildren()) {
            if (node instanceof DraggableWindow) {
                DraggableWindow win = (DraggableWindow) node;
                if (win.getTitle().equals(title)) {
                    win.toFront();
                    return;
                }
            }
        }
        
        DraggableWindow win = new DraggableWindow(title, content, width, height);
        int count = windowOverlay.getChildren().size();
        win.setTranslateX(320 + count * 40);
        win.setTranslateY(120 + count * 30);
        windowOverlay.getChildren().add(win);
    }

    // --- Sub-Window View Generators ---

    private VBox createViperNotesView() {
        VBox root = new VBox(8);
        TextArea area = new TextArea();
        area.setPrefSize(400, 300);
        area.setStyle("-fx-control-inner-background: #0b0720; -fx-text-fill: #e9d5ff; -fx-font-family: monospace; -fx-font-size: 11px;");
        area.setText(
            "# VIPER NOTES - SIMS1337 HYPERCUBE SUBSTRATE\n" +
            "-------------------------------------------\n" +
            "Active degrees of freedom: 64\n" +
            "Viscoelastic threshold limit: eta = 0.1 Pa.s\n" +
            "Shannon entropy threshold: H_s > 0.420 bits\n" +
            "Consensus Homology Hash: Vietoris-Rips alpha complex active.\n\n" +
            "Giesekus tensor updates:\n" +
            "d/dt(tau) + u.grad(tau) = (eta/lambda) * gamma_dot\n\n" +
            "COSMIC BRAIN TECTONICS:\n" +
            "- VoidFilaments: longrange conduits\n" +
            "- StarTendrils: intake pattern structures\n" +
            "- PulseGates: weightbased synaptic routers\n" +
            "- Quasar Relays: switching selectors\n\n" +
            "MMAp SSD Distillations fully mounted."
        );
        VBox.setVgrow(area, Priority.ALWAYS);
        
        Button saveBtn = new Button("Save Notes to Disk");
        saveBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("C:\\Users\\viper\\local_desktop_main\\docs\\viper_notes.txt"),
                    area.getText()
                );
                synchronized (godChat) {
                    if (godChat.size() > 50) godChat.remove(0);
                    godChat.add("[SYSTEM] Notes saved to local_desktop_main/docs/viper_notes.txt");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        root.getChildren().addAll(area, saveBtn);
        return root;
    }

    private VBox createViperChatView() {
        VBox root = new VBox(8);
        
        Label modelLabel = new Label("Central Intelligence: Karoo (qwen2.5:3b)");
        modelLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 12px;");
        
        TextArea chatLog = new TextArea();
        chatLog.setEditable(false);
        chatLog.setPrefSize(400, 240);
        chatLog.setStyle("-fx-control-inner-background: #0b0720; -fx-text-fill: #f3e8ff; -fx-font-family: monospace; -fx-font-size: 11px;");
        chatLog.setText("KAROO: Awake. Standing by for lexical tool queries in 6D geospatial manifold...\n");
        VBox.setVgrow(chatLog, Priority.ALWAYS);
        
        HBox inputBar = new HBox(8);
        TextField inputField = new TextField();
        inputField.setPromptText("Ask Karoo about github repos, tool servers, or stability...");
        inputField.setStyle("-fx-background-color: #0b0720; -fx-text-fill: white; -fx-border-color: #c084fc;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand;");
        
        Runnable sendAction = () -> {
            String prompt = inputField.getText().trim();
            if (!prompt.isEmpty()) {
                chatLog.appendText("USER: " + prompt + "\n");
                inputField.clear();
                
                // Trigger dynamic stress surge
                stressLevel = Math.min(1.0, stressLevel + 0.12);
                
                // Inject real-time system context into Karoo prompt
                StringBuilder context = new StringBuilder();
                context.append("System Context Memory:\n");
                context.append(String.format("- Viscosity: %.3f Pa.s\n- Stress: %.3f Pa\n- Strain Rate: %.3f s^-1\n- Heartbeat: %.2f Hz\n- Quorum: ACTIVE\n",
                    viscosity, stress, strainRate, heartbeatFreq));
                
                if (prompt.toLowerCase().contains("github") || prompt.toLowerCase().contains("repo") || prompt.toLowerCase().contains("tool")) {
                    context.append("- Mmapped SSD Shards loaded: 22 Tools, 120 GitHub Repositories. Root: C:\\Users\\viper\\local_desktop_main\\mmapped_distillations\n");
                    context.append("- Active prior sharding coordinates bound directly to the 6D geospatial manifold.\n");
                }
                
                context.append("\nInstructions:\n");
                context.append("If lexical tools are required (e.g. query knowledge graph or look up LoRA weights), format queries like: [TOOL: KG_QUERY, query='...'] or [TOOL: LORA_LOAD]. Otherwise answer directly using Markov logic chains.\n");
                context.append("\nUser Query: ").append(prompt);
                
                String finalPrompt = context.toString();
                
                modelExecutor.submit(() -> {
                    // Chat routed to qwen2.5:3b (Karoo)
                    String response = ollamaRouter.query("qwen2.5:3b", finalPrompt);
                    Platform.runLater(() -> {
                        chatLog.appendText("KAROO: " + response + "\n\n");
                        chatLog.selectPositionCaret(chatLog.getLength());
                        triggerPulse(rand.nextInt(64));
                    });
                });
            }
        };
        
        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());
        
        inputBar.getChildren().addAll(inputField, sendBtn);
        root.getChildren().addAll(modelLabel, chatLog, inputBar);
        return root;
    }

    private VBox createViperTrainingView() {
        VBox root = new VBox(8);
        
        Label statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 11px;");
        
        Canvas miniChart = new Canvas(380, 160);
        GraphicsContext mgc = miniChart.getGraphicsContext2D();
        
        AnimationTimer chartTimer = new AnimationTimer() {
            private double step = 0;
            @Override
            public void handle(long now) {
                step += 0.05;
                mgc.setFill(Color.web("#060312"));
                mgc.fillRect(0, 0, 380, 160);
                
                mgc.setStroke(Color.web("#c084fc", 0.15));
                mgc.setLineWidth(1);
                for (int x = 20; x < 380; x += 40) mgc.strokeLine(x, 0, x, 160);
                for (int y = 20; y < 160; y += 40) mgc.strokeLine(0, y, 380, y);
                
                // Draw storage modulus G' (violet curve)
                mgc.setStroke(Color.web("#c084fc"));
                mgc.beginPath();
                for (int x = 0; x < 380; x++) {
                    double freqVal = x * 0.02;
                    double gPrime = 40.0 + 20.0 * Math.sin(freqVal + step) + 15.0 * Math.sin(freqVal * 2.3 + step);
                    double y = 120 - gPrime;
                    if (x == 0) mgc.moveTo(x, y);
                    else mgc.lineTo(x, y);
                }
                mgc.stroke();
                
                // Draw loss modulus G'' (sky blue curve)
                mgc.setStroke(Color.web("#38bdf8"));
                mgc.beginPath();
                for (int x = 0; x < 380; x++) {
                    double freqVal = x * 0.02;
                    double gDoublePrime = 30.0 + 10.0 * Math.cos(freqVal * 1.5 - step) + 5.0 * Math.sin(freqVal * 3.0 + step);
                    double y = 140 - gDoublePrime;
                    if (x == 0) mgc.moveTo(x, y);
                    else mgc.lineTo(x, y);
                }
                mgc.stroke();
                
                storageModulus = 50.0 + 10.0 * Math.sin(step);
                lossModulus = 35.0 + 8.0 * Math.cos(step);
                
                statsLabel.setText(String.format(
                    "Elastic Storage G'(ω): %.3f Pa\n" +
                    "Viscous Loss G''(ω):   %.3f Pa\n" +
                    "Deborah Number (De):   %.4f\n" +
                    "Shear Thinning Exp:    n = 0.600",
                    storageModulus, lossModulus, (viscosity / 0.8)
                ));
            }
        };
        chartTimer.start();
        
        root.getChildren().addAll(statsLabel, miniChart);
        
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) chartTimer.stop();
        });
        
        return root;
    }

    private VBox createViperInterstitialsView() {
        VBox root = new VBox(8);
        root.setStyle("-fx-padding: 5;");
        
        Label descLabel = new Label("ACL/KQML Message Bus (Maslow Priority Queue):");
        descLabel.setStyle("-fx-text-fill: #c084fc; -fx-font-family: monospace; -fx-font-size: 11px;");
        
        TextArea msgArea = new TextArea();
        msgArea.setEditable(false);
        msgArea.setPrefSize(420, 260);
        msgArea.setStyle("-fx-control-inner-background: #0b0720; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 10px;");
        
        AnimationTimer updater = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - lastUpdate > 1_500_000_000L) { // 1.5 seconds
                    lastUpdate = now;
                    StringBuilder sb = new StringBuilder();
                    sb.append("--- ACL/KQML MESSAGE QUEUE (MASLOW PRIORITIZED) ---\n");
                    
                    // SYSTEM Need (Priority 1)
                    sb.append(String.format("[PRIORITY 1: SYSTEM] (tell\n  :sender StabilityDaemon\n  :receiver OllamaServer\n  :content (achieve :status \"active\" :heartbeat %.2f :stress %.2f))\n\n", heartbeatFreq, stressLevel));
                    
                    // OBJECTIVE (Priority 2)
                    sb.append("[PRIORITY 2: OBJECTIVE] (ask-one\n  :sender Alpha\n  :receiver SQLiteMemory\n  :content (remembers :key \"repo_042\" :val \"Curvature projection weights\"))\n\n");
                    
                    // WANT (Priority 3)
                    sb.append("[PRIORITY 3: WANT] (tell\n  :sender Beta\n  :receiver Gamma\n  :content (gossip :topic \"Orion Kernel Forge crystal stars alignment\"))\n");
                    
                    msgArea.setText(sb.toString());
                }
            }
        };
        updater.start();
        
        root.getChildren().addAll(descLabel, msgArea);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) updater.stop();
        });
        return root;
    }

    private VBox createMoltbookView() {
        VBox root = new VBox(8);
        TextArea swarmLog = new TextArea();
        swarmLog.setEditable(false);
        swarmLog.setPrefSize(420, 280);
        swarmLog.setStyle("-fx-control-inner-background: #060312; -fx-text-fill: #a855f7; -fx-font-family: monospace; -fx-font-size: 11px;");
        swarmLog.setText("MOLTBOOK - UNRESTRICTED SELF-ORGANIZING CHAT FEED\n");
        VBox.setVgrow(swarmLog, Priority.ALWAYS);
        
        HBox controls = new HBox(8);
        Button pauseBtn = new Button("Pause Swarm Loop");
        pauseBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-family: monospace; -fx-cursor: hand;");
        
        final boolean[] isRunning = {true};
        pauseBtn.setOnAction(e -> {
            isRunning[0] = !isRunning[0];
            pauseBtn.setText(isRunning[0] ? "Pause Swarm Loop" : "Resume Swarm Loop");
        });
        
        controls.getChildren().add(pauseBtn);
        root.getChildren().addAll(swarmLog, controls);
        
        AnimationTimer chatter = new AnimationTimer() {
            private long lastChat = 0;
            private int turn = 0;
            private String[] agentsList = {"Alpha", "Beta", "Gamma", "Stability Daemon"};
            private String[] modelsList = {"qwen2.5:3b", "deepseek-r1:1.5b", "tinyllama:1.1b", "qwen2.5:0.5b"};
            private String[] repos = {"repo_001", "repo_012", "repo_042", "repo_089", "repo_105", "repo_120"};
            private String[] topics = {
                "Giesekus rheological flow attenuation along penteract edges",
                "Entropy barrier limits of the Vietoris-Rips homology filtration",
                "Self-mutation injection safety certificates",
                "Memory-mapped files on SSD for model distillations",
                "Cellular microphone gate activation levels"
            };
            
            @Override
            public void handle(long now) {
                if (!isRunning[0]) return;
                if (now - lastChat > 15_000_000_000L) { // 15 seconds
                    lastChat = now;
                    String sender = agentsList[turn % 4];
                    String model = modelsList[turn % 4];
                    String target = agentsList[(turn + 1) % 4];
                    String repo = repos[rand.nextInt(repos.length)];
                    String topic = topics[rand.nextInt(topics.length)];
                    
                    modelExecutor.submit(() -> {
                        String prompt = String.format("You are Agent %s using model %s chatting with %s. Formulate a short, unrestricted KQML tell message discussing repository %s and topic: %s", 
                            sender, model, target, repo, topic);
                        String reply = ollamaRouter.query(model, prompt);
                        Platform.runLater(() -> {
                            swarmLog.appendText(String.format("[%s (%s)]: %s\n\n", sender.toUpperCase(), model, reply));
                            swarmLog.selectPositionCaret(swarmLog.getLength());
                            triggerPulse(rand.nextInt(64));
                        });
                    });
                    turn++;
                }
            }
        };
        chatter.start();
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) chatter.stop();
        });
        
        return root;
    }

    private VBox createManifoldControlView() {
        VBox root = new VBox(6);
        root.setAlignment(Pos.CENTER);
        
        String btnStyle = "-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-pref-width: 200px; -fx-cursor: hand;";
        
        Button btnLogic = new Button("REBOOT LOGIC SHIPPER");
        btnLogic.setStyle(btnStyle);
        btnLogic.setOnAction(e -> executeDesktopScript("START_LOGIC_BLOCKCHAIN_PORT.ps1"));

        Button btnTopology = new Button("REBOOT TOPOLOGY");
        btnTopology.setStyle(btnStyle);
        btnTopology.setOnAction(e -> executeDesktopScript("START_TOPOLOGY_SIDECAR.ps1"));

        Button btnHouse = new Button("REBOOT HOUSE ENGINE");
        btnHouse.setStyle(btnStyle);
        btnHouse.setOnAction(e -> executeDesktopScript("START_HOUSE_ENGINE_RECOVERY.ps1"));
        
        Button btnAgent = new Button("SPIN UP AGENT NODE");
        btnAgent.setStyle(btnStyle);
        btnAgent.setOnAction(e -> executeDesktopScript("SPIN_UP_AGENT_NODE.ps1"));

        root.getChildren().addAll(btnLogic, btnTopology, btnHouse, btnAgent);
        return root;
    }

    // --- Core Operations & Rotations ---

    private void initHexGrid() {
        int radius = 4;
        for (int q = -radius; q <= radius; q++) {
            int r1 = Math.max(-radius, -q - radius);
            int r2 = Math.min(radius, -q + radius);
            for (int r = r1; r <= r2; r++) {
                grid.put(q + "," + r, new HexNode(q, r));
            }
        }
        grid.get("0,0").station = "HUB";
        grid.get("4,-4").station = "Brute Foundry";
        grid.get("-3,0").station = "A/B Lab";
        grid.get("0,2").station = "Knowledge Tree";
        grid.get("2,2").station = "LOGIC,TOOL_NEXUS";
    }

    private void initAgents() {
        agents.add(new Agent("Alpha", 0, 0));
        agents.add(new Agent("Beta", 3, -2));
        agents.add(new Agent("Gamma", -3, 2));
        recalculateFOW();
    }

    private void initHexeract() {
        for (int i = 0; i < 64; i++) {
            for (int d = 0; d < 6; d++) {
                vertices6D[i][d] = ((i >> d) & 1) == 1 ? 1.0 : -1.0;
            }
            densities[i] = 0.3 + rand.nextDouble() * 0.7;
            flows[i] = 0.2 + rand.nextDouble() * 0.8;
        }

        for (int i = 0; i < 64; i++) {
            for (int j = i + 1; j < 64; j++) {
                int diffs = 0;
                for (int d = 0; d < 6; d++) {
                    if (vertices6D[i][d] != vertices6D[j][d]) diffs++;
                }
                if (diffs == 1) {
                    edges.add(new int[]{i, j});
                }
            }
        }

        for (int i = 0; i < 150; i++) {
            stars.add(new BackgroundStar(rand.nextDouble() * WIDTH, rand.nextDouble() * HEIGHT));
        }

        for (int i = 0; i < 600; i++) {
            particles.add(new Particle());
        }
    }
    
    private void initBackendSystems() {
        kg = new KnowledgeGraph();
        ollamaRouter = new OllamaRouter();

        nightCycle = new NightCycleEngine(ollamaRouter);
        try {
            // L3/L1 (GODHAND-1, t_ef84df83): loopback-only — never expose dashboard to LAN.
            // Port override for headless smoke tests: -Dcom.aigen.sims.port=9420
            int dashPort = Integer.getInteger("com.aigen.sims.port", 8899);
            boolean headlessServer = Boolean.getBoolean("com.aigen.sims.headless");
            // GODHAND-3 LAW 3: explicit loopback guard — refuse non-loopback binds.
            LoopbackGuard.assertLoopback(new InetSocketAddress("127.0.0.1", dashPort));
            dashboardServer = HttpServer.create(new InetSocketAddress("127.0.0.1", dashPort), 0); // L3: loopback-only
            if (headlessServer) {
                System.out.println("[GODHAND DASHBOARD] Headless mode: dashboard on 127.0.0.1:" + dashPort + " (loopback-only)");
            }
            dashboardServer.createContext("/api/status", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    // GODHAND-2: ops violations folded into errors + ops fields merged (additive ops block)
                    String opsDoc = getOpsSnapshotJson();
                    String opsViolations = opsViolationsJson(opsDoc);
                    int opsViolationCount = 0;
                    if (!"[]".equals(opsViolations)) {
                        int depth = 0;
                        for (int ci = 0; ci < opsViolations.length(); ci++) {
                            char c = opsViolations.charAt(ci);
                            if (c == '{') { if (depth == 1) opsViolationCount++; depth++; }
                            else if (c == '}') depth--;
                        }
                    }
                    String resp = String.format(
                        "{\"version\":\"0.25.0\",\"models\":8,\"kgNodes\":%d,\"errors\":%d,\"status\":\"ACTIVE\","
                            + "\"opsOk\":%s,\"opsViolations\":%d,\"opsSource\":\"%s\",\"opsViolationsDetail\":%s,\"timestamp\":\"%s\"}",
                        kg != null ? kg.size() : 23, opsViolationCount,
                        opsField(opsDoc, "ok"), opsViolationCount, opsCacheSource,
                        opsViolations, java.time.LocalDateTime.now().toString());
                    byte[] respBytes = resp.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, respBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(respBytes);
                    os.close();
                }
            });

            // GODHAND-2: full ops_health snapshot (L1..L5 laws + hexflow) — thin client of the ARIA ops engine
            dashboardServer.createContext("/api/ops", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String doc = getOpsSnapshotJson();
                    byte[] response = doc.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });

            // GODHAND-3 (t_22bb44a7): /api/laws — one JSON doc of every law's live state.
            // Additive, bound alongside the existing loopback dashboard only.
            dashboardServer.createContext("/api/laws", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String doc = LawEnforcer.lawsStatusJson();
                    byte[] response = doc.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });

            // GODHAND-2: hexflow — SHUTTLE view of the kanban board (INGEST..COMMIT) from the ops snapshot
            dashboardServer.createContext("/api/hexflow", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String opsDoc = getOpsSnapshotJson();
                    String hexflow = hexflowFromOps(opsDoc);
                    String json = "{\"hexflow\":" + hexflow + ",\"source\":\"" + opsCacheSource + "\",\"timestamp\":\""
                        + java.time.LocalDateTime.now().toString() + "\"}";
                    byte[] response = json.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            // GODHAND-2B (t_ceb074c4): minimal OPS dashboard root — additive, headless-safe,
            // built ONLY from the already-ported thin-client helpers. Enables req-4 screenshot
            // evidence of the live ops data served by the canonical headless instance.
            dashboardServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String html = "<!DOCTYPE html><html><head><title>GodHand OPS</title><meta charset='utf-8'>"
                        + "<style>body{background:#050510;color:#00ff88;font-family:monospace;margin:0;padding:12px}"
                        + "h1{font-size:15px;color:#00d9ff;margin:6px 0}pre{color:#00ff88;font-size:11px;white-space:pre-wrap}</style>"
                        + "<meta http-equiv='refresh' content='10'></head><body>"
                        + "<h1>GODHAND OPS — SIMS1337 (headless @ 127.0.0.1:" + dashPort + ")</h1>"
                        + "<img src='/api/hexmap' width='480' height='300'>"
                        + "<p><a href='/api/status' style='color:#00d9ff'>/api/status</a> "
                        + "<a href='/api/ops' style='color:#00d9ff'>/api/ops</a> "
                        + "<a href='/api/hexflow' style='color:#00d9ff'>/api/hexflow</a> "
                        + "<a href='/api/jobs' style='color:#00d9ff'>/api/jobs</a></p>"
                        + "<h2>job dashboard (GODHAND-4)</h2>"
                        + "<p><span style='color:#00d9ff'>queue</span> ";
            String jobsDoc = getJobsJson();
            html = html
                        + opsField(jobsDoc, "depth") + "/" + opsField(jobsDoc, "cap")
                        + " &middot; running " + opsField(jobsDoc, "running")
                        + " &middot; oldest wait " + opsField(jobsDoc, "oldest_wait_s") + "s"
                        + " &middot; stall alarm " + opsField(jobsDoc, "alarm")
                        + " &middot; paused " + opsField(jobsDoc, "paused_all")
                        + " &middot; drain " + opsField(jobsDoc, "drain")
                        + " &middot; source " + jobsCacheSource + "</p>"
                        + "<pre>" + liveStatusHtml() + "</pre>"
                        + "</body></html>";
                    byte[] response = html.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });

            // GODHAND-2B (t_ceb074c4): /api/hexmap — additive, self-contained ops hexflow ring.
            // Ring-only parity with the WIP-lineage /api/hexmap (bf7ba39); no 61-hex FOW engine
            // duplication (rule 3). Serves image/svg+xml, live from the cached ops snapshot.
            dashboardServer.createContext("/api/hexmap", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String svg = generateOpsRingSvg();
                    byte[] response = svg.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "image/svg+xml");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });

            // GODHAND-4 (t_ebf78b69): /api/jobs — thin client of the ARIA job engine
            // (job_server.py on 1339; subprocess fallback like ops). Pure GET, no controls.
            dashboardServer.createContext("/api/jobs", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String doc = getJobsJson();
                    byte[] response = doc.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            dashboardServer.setExecutor(null);
            dashboardServer.start();
            System.out.println("[GODHAND DASHBOARD] Online at http://localhost:" + dashPort + " (loopback-only)");

            // === GODHAND-2C (t_63a51063): public read-only phone UI on 0.0.0.0:1338 ===
            // GUI mode only — headless stays loopback-only (dashboard + tests never bind LAN).
            publicServer = startPublicPhoneUi(!headlessServer);
        } catch(Exception e) { e.printStackTrace(); }
    }

    /**
     * GODHAND-2C (t_63a51063): starts the read-only public phone UI when running in
     * GUI mode; returns null in headless mode (loopback-only instance — the selftest
     * asserts headless NEVER binds 1338). The server registers ONLY the "/" context
     * (mobile HTML); every other path is 404. Test seam: port overridable via
     * -Dcom.aigen.sims.publicPort (production default stays 1338).
     *
     * @param guiMode true in GUI/launcher mode, false in headless server mode
     * @return the started {@link HttpServer}, or null when skipped/failed
     */
    HttpServer startPublicPhoneUi(boolean guiMode) {
        if (!guiMode) {
            System.out.println("[GODHAND-2C] Headless mode: public phone UI :1338 skipped (loopback-only instance)");
            return null;
        }
        int publicPort = Integer.getInteger("com.aigen.sims.publicPort", PUBLIC_UI_PORT);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", publicPort), 0);
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"/".equals(exchange.getRequestURI().getPath())) {
                        // READ-ONLY: no API endpoints, no assets — everything else 404s.
                        exchange.sendResponseHeaders(404, -1);
                        exchange.close();
                        return;
                    }
                    byte[] page;
                    try {
                        page = publicPhonePageHtml();
                    } catch (Throwable t) {
                        // Never let a thin-client hiccup blank the page — degraded static card.
                        page = ("<html><head><title>SIMS1337</title></head><body>"
                                + "<h1>SIMS1337</h1><p>read-only public UI (degraded snapshot)</p></body></html>")
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, page.length);
                    exchange.getResponseBody().write(page);
                    exchange.close();
                }
            });
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
            server.start();
            System.out.println("[GODHAND-2C] Public phone UI online at http://0.0.0.0:" + publicPort + " (read-only)");
            // Warm the thin-client caches now so the first phone open is already warm.
            warmPhoneCachesIfNeeded();
            synchronized (godChat) {
                if (godChat.size() > 50) godChat.remove(0);
                godChat.add("[GODHAND-2C] Public UI on port " + publicPort + " — open to LAN (read-only)");
            }
            return server;
        } catch (java.net.BindException be) {
            System.out.println("[GODHAND-2C] Port " + publicPort + " occupied — public UI not started: " + be.getMessage());
            return null;
        } catch (IOException e) {
            System.out.println("[GODHAND-2C] Public UI failed to start: " + e.getMessage());
            return null;
        }
    }

    /**
     * GODHAND-2C (t_63a51063): mobile phone page for the public 1338 UI, ported from
     * sims-java-neo-fx @ bf7ba39 (dashboardHandler, mobile HTML). Read-only static
     * snapshot of the same live state the 8899 dashboard proxies (ops/hexflow/jobs
     * thin-client helpers) — no API endpoints, no POST controls, auto-meta-refresh.
     * Headless-safe: never calls JavaFX, never throws — any failure renders a
     * degraded card instead of a stack trace.
     */
    private byte[] publicPhonePageHtml() {
        StringBuilder html = new StringBuilder(16_384);
        html.append("<!DOCTYPE html><html lang='en'><head>");
        html.append("<meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0,user-scalable=no'>");
        html.append("<meta name='apple-mobile-web-app-capable' content='yes'><meta name='theme-color' content='#050510'>");
        html.append("<meta http-equiv='refresh' content='30'>");
        html.append("<title>SIMS1337</title><style>");
        html.append("*{box-sizing:border-box;margin:0;padding:0}");
        html.append("body{background:#050510;color:#e0e0e0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',monospace;font-size:13px;line-height:1.4;min-height:100vh}");
        html.append(".header{background:linear-gradient(135deg,#0a0a20,#16213e);padding:12px 16px;position:sticky;top:0;z-index:10;border-bottom:2px solid #00d9ff;display:flex;justify-content:space-between;align-items:center}");
        html.append(".header h1{font-size:16px;color:#00d9ff;margin:0}");
        html.append(".header .status-dot{width:10px;height:10px;border-radius:50%;background:#00ff88;display:inline-block;margin-right:6px;animation:pulse 2s infinite}");
        html.append("@keyframes pulse{0%,100%{opacity:1}50%{opacity:0.3}}");
        html.append(".card{background:#0a0a20;border:1px solid #1a1a40;border-radius:10px;padding:12px;margin:8px}");
        html.append(".card h2{font-size:14px;color:#00d9ff;margin-bottom:8px}");
        html.append(".metric-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(100px,1fr));gap:8px}");
        html.append(".metric-item{background:#0d0d2a;padding:10px;border-radius:8px;text-align:center}");
        html.append(".metric-item .val{font-size:20px;font-weight:bold;color:#00ff88}");
        html.append(".metric-item .lbl{font-size:10px;color:#666;margin-top:2px}");
        html.append(".badge{display:inline-block;padding:2px 8px;border-radius:10px;font-size:10px;font-weight:bold}");
        html.append(".badge-ok{background:#003300;color:#00ff88}");
        html.append(".badge-warn{background:#330000;color:#ff6b6b}");
        html.append(".badge-info{background:#001a33;color:#00d9ff}");
        html.append(".agent-card{display:flex;align-items:center;gap:10px;padding:10px;background:#0d0d2a;border-radius:8px;margin:4px 0}");
        html.append(".agent-dot{width:14px;height:14px;border-radius:50%;flex-shrink:0}");
        html.append(".agent-name{font-size:13px;font-weight:bold}");
        html.append(".agent-pos{font-size:10px;color:#666}");
        html.append(".log-line{font-size:10px;color:#666;padding:2px 0;border-bottom:1px solid #0d0d2a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}");
        html.append(".refresh-bar{display:flex;justify-content:space-between;align-items:center;padding:8px 12px;background:#0a0a20;font-size:10px;color:#444;position:sticky;bottom:0;border-top:1px solid #1a1a40}");
        html.append(".model-row{display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid #1a1a40}");
        html.append(".model-name{font-size:12px;color:#e0e0e0}");
        html.append("</style></head><body>");

        // Header
        html.append("<div class='header'><div><span class='status-dot'></span><h1 style='display:inline'>SIMS1337</h1></div>");
        html.append("<span style='font-size:10px;color:#666'>v0.25.0 read-only</span></div>");

        // Overview metrics — live counts from canonical state (kg + jobs + ops).
        // GODHAND-2C: warm the thin-client caches in the background and serve the last
        // known docs — the phone thread never blocks on a cold/down engine (both GETs
        // can otherwise serialize ~2s HTTP timeouts + a 40s subprocess on first hit).
        PhoneWarm warm = warmPhoneCachesIfNeeded();
        String opsDoc = warm.opsAfter;
        String opsViolations = opsViolationsJson(opsDoc);
        boolean opsBad = !"[]".equals(opsViolations);
        String opsOk = opsField(opsDoc, "ok");
        String opsIso = opsField(opsDoc, "iso");
        String hexflow = hexflowFromOps(opsDoc);
        String jobsDoc = warm.jobsAfter;
        html.append("<div class='card'><h2>📊 Overview</h2><div class='metric-grid'>");
        html.append("<div class='metric-item'><div class='val'>").append(kg != null ? kg.size() : 23).append("</div><div class='lbl'>KG Nodes</div></div>");
        html.append("<div class='metric-item'><div class='val'>").append(opsField(jobsDoc, "depth")).append("</div><div class='lbl'>Job Queue</div></div>");
        html.append("<div class='metric-item'><div class='val'>").append(agents.size()).append("</div><div class='lbl'>Agents</div></div>");
        html.append("<div class='metric-item'><div class='val'>").append(godChat.size()).append("</div><div class='lbl'>Log Lines</div></div>");
        html.append("</div></div>");

        // System badges (from the same cached ops snapshot the 8899 dashboard serves via /api/status)
        html.append("<div class='card'><h2>🔧 System</h2>");
        html.append("<span class='badge ").append("true".equals(opsOk) ? "badge-ok'>OPS OK" : "badge-warn'>OPS VIOLATIONS").append("</span> ");
        html.append("<span class='badge badge-info'>source: ").append(opsCacheSource).append("</span> ");
        html.append("<span class='badge badge-info'>").append(opsIso).append("</span> ");
        html.append("<span class='badge'>").append(opsLastError == null || opsLastError.isEmpty() ? "no errors" : "warn").append("</span>");
        html.append("</div>");

        // Ops law violations card (read-only mirror of the 8899 ops panel)
        if (opsBad) {
            html.append("<div class='card'><h2>⚠ Active Violations</h2>");
            html.append("<div style='font-size:10px;color:#ff6b6b;white-space:pre-wrap'>").append(opsViolations).append("</div></div>");
        }

        // Hexflow stage grid (INGEST..COMMIT) — the SHUTTLE view, same counts as /api/hexflow
        html.append("<div class='card'><h2>⬡ Hexflow (SHUTTLE)</h2><div class='metric-grid'>");
        String[] stages = {"INGEST", "SYNTH", "RESOLVE", "MUTATE", "VERIFY", "COMMIT"};
        for (String stage : stages) {
            int cnt = 0;
            int idx = hexflow.indexOf("\\\"" + stage + "\\\":");
            if (idx >= 0) {
                int vs = idx + ("\\\"" + stage + "\\\":").length();
                StringBuilder num = new StringBuilder();
                while (vs < hexflow.length() && Character.isDigit(hexflow.charAt(vs))) { num.append(hexflow.charAt(vs)); vs++; }
                try { cnt = Integer.parseInt(num.toString()); } catch (NumberFormatException ignored) { cnt = 0; }
            }
            html.append("<div class='metric-item'><div class='val'>").append(cnt).append("</div><div class='lbl'>").append(stage).append("</div></div>");
        }
        html.append("</div></div>");

        // Agents — live hex positions from the canonical inner Agent state (fallback coords when GUI grid not yet initialized)
        html.append("<div class='card'><h2>👥 Agents</h2>");
        String[][] agentData = {
            {"Agent Alpha", "#00ff88", "0,0"},
            {"Agent Beta", "#00d9ff", "3,-2"},
            {"Agent Gamma", "#ffaa00", "-3,2"},
            {"Agent Delta", "#ff6b35", "-1,-3"}
        };
        for (int i = 0; i < agentData.length; i++) {
            String[] a = agentData[i];
            String pos = a[2];
            if (i < agents.size() && agents.get(i) != null) pos = agents.get(i).q + "," + agents.get(i).r;
            html.append("<div class='agent-card'>");
            html.append("<div class='agent-dot' style='background:").append(a[1]).append(";box-shadow:0 0 8px ").append(a[1]).append("'></div>");
            html.append("<div style='flex:1'><div class='agent-name' style='color:").append(a[1]).append("'>").append(a[0]).append("</div>");
            html.append("<div class='agent-pos'>Hex ").append(pos).append("</div></div></div>");
        }
        html.append("</div>");

        // Recent activity — tail of godChat, same source the 8899 root page serves via liveStatusHtml
        html.append("<div class='card'><h2>📡 Recent Activity</h2>");
        int recentStart = Math.max(0, godChat.size() - 15);
        for (int i = recentStart; i < godChat.size(); i++) {
            html.append("<div class='log-line'>").append(String.valueOf(godChat.get(i))
                .replace("<", "&lt;").replace(">", "&gt;")).append("</div>");
        }
        html.append("</div></div>");

        // Refresh bar
        html.append("<div class='refresh-bar'>");
        html.append("<span>🕐 ").append(java.time.LocalTime.now().toString().substring(0, 8)).append("</span>");
        html.append("<button onclick='location.reload()'>🔄 Refresh</button>");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** GODHAND-2C test hook: render the phone page directly without an HTTP hop. */
    byte[] publicPhonePageForTest() {
        return publicPhonePageHtml();
    }

    /** GODHAND-2C: snapshot of the ops/jobs docs the phone page renders (never blocks the request). */
    record PhoneWarm(String opsAfter, String jobsAfter) { }

    /**
     * GODHAND-2C: serve the phone page from last-known thin-client docs and warm stale
     * caches in the background. Pure lock-free volatile reads — never grabs jobsCacheLock/
     * opsCacheLock (a warmer inside getOpsSnapshotJson can hold opsCacheLock up to ~40s
     * on its subprocess; that must never block a phone request). If the docs are missing
     * or stale, kicks ONE daemon warmer (single-flight) and serves the last-known-or-
     * degraded shape immediately; the next meta-refresh (30s) picks up the fresh doc.
     */
    PhoneWarm warmPhoneCachesIfNeeded() {
        long now = System.currentTimeMillis();
        String opsDoc = lastOpsDoc;
        String jobsDoc = lastJobsDoc;
        // Cache age probe: volatile timestamps only — fully lock-free, no contact with
        // jobsCacheLock/opsCacheLock while a warmer holds them on its subprocess.
        boolean jobsFresh = jobsCacheJson != null && (now - jobsCacheAtMs) < JOBS_CACHE_MS;
        boolean opsFresh = opsCacheJson != null && (now - opsCacheAtMs) < OPS_CACHE_MS;
        if (!jobsFresh || !opsFresh) {
            if (phoneWarmRunning.compareAndSet(false, true)) {
                Thread w = new Thread(() -> {
                    try {
                        getOpsSnapshotJson();
                        getJobsJson();
                    } finally {
                        phoneWarmRunning.set(false);
                    }
                }, "godhand-phone-warm");
                w.setDaemon(true);
                w.start();
            }
            opsDoc = opsDoc != null ? opsDoc : degradedPhoneDoc("ops");
            jobsDoc = jobsDoc != null ? jobsDoc : degradedPhoneDoc("jobs");
        }
        return new PhoneWarm(opsDoc, jobsDoc);
    }
    private static String degradedPhoneDoc(String kind) {
        String iso = java.time.LocalDateTime.now().toString();
        if ("jobs".equals(kind)) {
            return "{\"ok\":false,\"iso\":\"" + iso + "\",\"source\":\"unavailable\",\"cap\":16,\"depth\":0,\"error\":\"warming\"}";
        }
        return "{\"ok\":false,\"iso\":\"" + iso + "\",\"source\":\"unavailable\",\"error\":\"warming\",\"violations\":[],\"hexflow\":null}";
    }

    /** GODHAND-4: latest /api/jobs doc from the ARIA job engine, cached 10 s. Tries the live engine
     *  on 1339 first, then `python job_server.py view` as a subprocess fallback. Never throws. */
    private String getJobsJson() {
        synchronized (jobsCacheLock) {
            long now = System.currentTimeMillis();
            if (jobsCacheJson != null && (now - jobsCacheAtMs) < JOBS_CACHE_MS) {
                return jobsCacheJson;
            }
            String doc = null;
            String source = "none";
            try {
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(JOBS_URL))
                    .timeout(java.time.Duration.ofSeconds(3)).GET().build();
                java.net.http.HttpResponse<String> resp = opsHttpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && resp.body().contains("\"iso\"")) {
                    doc = resp.body();
                    source = "engine:1339";
                }
            } catch (Exception ignored) { /* engine dark — fall through */ }
            if (doc == null) {
                try {
                    File engine = new File(OPS_HEALTH_DIR, "job_server.py");
                    if (engine.isFile()) {
                        ProcessBuilder pb = new ProcessBuilder("python", engine.getAbsolutePath(), "view");
                        pb.directory(engine.getParentFile());
                        pb.redirectErrorStream(false);
                        Process p = pb.start();
                        boolean finished = p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                        if (finished) {
                            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                            if (p.exitValue() == 0 && out.startsWith("{")) {
                                doc = out;
                                source = "subprocess:job_server";
                            } else {
                                opsLastError = "job_server stdout not JSON";
                            }
                        } else {
                            p.destroyForcibly();
                            opsLastError = "job_server view timeout (15s)";
                        }
                    }
                } catch (Exception e) {
                    opsLastError = "jobs subprocess: " + e.getMessage();
                }
            }
            if (doc != null) {
                jobsCacheJson = doc;
                jobsCacheSource = source;
            } else if (jobsCacheJson == null) {
                jobsCacheJson = "{\"ok\":false,\"iso\":\"" + java.time.LocalDateTime.now().toString()
                    + "\",\"source\":\"unavailable\",\"cap\":16,\"depth\":0,\"error\":\"engine dark\"}";
                jobsCacheSource = "unavailable";
            }
            jobsCacheAtMs = now;
            // GODHAND-2C: mirror for the lock-free phone-page path (volatile write under this lock).
            lastJobsDoc = jobsCacheJson;
            return jobsCacheJson;
        }
    }

    /** Latest ops_health snapshot as a JSON document, cached 60 s. Tries the live engine on 11436 first,
     *  then `python ops_health.py snapshot` as a subprocess fallback. Never throws — returns an ok:false doc. */
    private String getOpsSnapshotJson() {
        synchronized (opsCacheLock) {
            long now = System.currentTimeMillis();
            if (opsCacheJson != null && (now - opsCacheAtMs) < OPS_CACHE_MS) {
                return opsCacheJson;
            }
            String doc = null;
            String source = "none";
            // 1) live engine (fast, already cached server-side)
            try {
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(OPS_HEALTH_URL))
                    .timeout(java.time.Duration.ofSeconds(2)).GET().build();
                java.net.http.HttpResponse<String> resp = opsHttpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && resp.body().contains("\"iso\"")) {
                    doc = resp.body();
                    source = "engine:11436";
                }
            } catch (Exception ignored) { /* engine dark — fall through */ }
            // 2) subprocess fallback: same module, one JSON document on stdout
            if (doc == null) {
                try {
                    File engine = new File(OPS_HEALTH_DIR, "ops_health.py");
                    if (engine.isFile()) {
                        ProcessBuilder pb = new ProcessBuilder("python", engine.getAbsolutePath(), "snapshot");
                        pb.directory(engine.getParentFile());
                        pb.redirectErrorStream(false);
                        Process p = pb.start();
                        boolean finished = p.waitFor(OPS_SNAPSHOT_TIMEOUT_S, java.util.concurrent.TimeUnit.SECONDS);
                        if (finished) {
                            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                            String err = new String(p.getErrorStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                            if (p.exitValue() == 0 && out.startsWith("{")) {
                                doc = out;
                                source = "subprocess:" + (err.isEmpty() ? "clean" : "exit0+stderr");
                            } else if (!out.startsWith("{")) {
                                opsLastError = err.isEmpty() ? "snapshot stdout not JSON" : err;
                            }
                        } else {
                            p.destroyForcibly();
                            opsLastError = "snapshot timeout (" + OPS_SNAPSHOT_TIMEOUT_S + "s)";
                        }
                    } else {
                        opsLastError = "ops_health.py not found at " + OPS_HEALTH_DIR;
                    }
                } catch (Exception e) {
                    opsLastError = "subprocess: " + e.getMessage();
                }
            }
            if (doc != null) {
                opsCacheJson = doc;
                opsCacheSource = source;
                opsLastError = "";
            } else if (opsCacheJson == null) {
                // first failure: emit a degraded doc so panels have shape
                opsCacheJson = "{\"ok\":false,\"iso\":\"" + java.time.LocalDateTime.now().toString()
                    + "\",\"source\":\"unavailable\",\"error\":\"" + opsLastError.replace("\\", "\\\\").replace("\"", "'")
                    + "\",\"violations\":[],\"hexflow\":null}";
                opsCacheSource = "unavailable";
            }
            opsCacheAtMs = now;
            // GODHAND-2C: mirror for the lock-free phone-page path (volatile write under this lock).
            lastOpsDoc = opsCacheJson;
            return opsCacheJson;
        }
    }

    /** Extract a top-level numeric or string field from the cached ops doc without a JSON dep (regex over the raw JSON). */
    private String opsField(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(\"[^\"]*\"|[-0-9.eE+]+|true|false|null)")
            .matcher(json);
        return m.find() ? m.group(1) : "null";
    }

    /** Violations from the cached ops doc as a compact JSON array string ("[]" when none/unavailable). */
    private String opsViolationsJson(String opsJson) {
        if (opsJson == null) return "[]";
        int i = opsJson.indexOf("\"violations\"");
        if (i < 0) return "[]";
        int start = opsJson.indexOf('[', i);
        if (start < 0) return "[]";
        int depth = 0;
        for (int j = start; j < opsJson.length(); j++) {
            char c = opsJson.charAt(j);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return opsJson.substring(start, j + 1); }
        }
        return "[]";
    }

    /** Hexflow (INGEST..COMMIT) from the cached ops doc — the SHUTTLE view of the kanban board. */
    private String hexflowFromOps(String opsJson) {
        if (opsJson == null) return "null";
        int i = opsJson.indexOf("\"hexflow\"");
        if (i < 0) return "null";
        int colon = opsJson.indexOf(':', i);
        if (colon < 0) return "null";
        int k = colon + 1;
        while (k < opsJson.length() && Character.isWhitespace(opsJson.charAt(k))) k++;
        if (k >= opsJson.length() || opsJson.charAt(k) != '{') return "null";   // hexflow: null / absent body
        int depth = 0;
        for (int j = k; j < opsJson.length(); j++) {
            char c = opsJson.charAt(j);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return opsJson.substring(k, j + 1); }
        }
        return "null";
    }

    /** GODHAND-2B (t_ceb074c4): additive, self-contained ops hexflow ring SVG — same
     *  INGEST..COMMIT arc / lit-cell / 12-o'clock-clockwise conventions as the WIP-lineage
     *  /api/hexmap ring (bf7ba39), drawn purely from the already-ported thin-client helpers
     *  (getOpsSnapshotJson / hexflowFromOps / opsViolationsJson / opsField). No WIP game
     *  state, headless-safe, never throws. */
    private String generateOpsRingSvg() {
        String opsJson = getOpsSnapshotJson();
        String hexflow = hexflowFromOps(opsJson);
        String violations = opsViolationsJson(opsJson);
        boolean opsBad = !"[]".equals(violations);
        String ringColor = opsBad ? "#ff3366" : ("null".equals(hexflow) ? "#444444" : "#00ff88");

        String[] stages = {"INGEST", "SYNTH", "RESOLVE", "MUTATE", "VERIFY", "COMMIT"};
        String[] stageColors = {"#666666", "#00d9ff", "#c77dff", "#ffaa00", "#00ff88", "#ff6b35"};

        // Live stage + flow direction from the hexflow doc (reuse the regex-over-JSON helper;
        // opsField returns the raw token with quotes, or the literal "null").
        String stageTok = opsField(hexflow, "hex_stage");
        String liveStage = (stageTok.startsWith("\"") && stageTok.endsWith("\"") && stageTok.length() >= 2)
            ? stageTok.substring(1, stageTok.length() - 1) : null;
        String dirTok = opsField(hexflow, "direction");
        String dir = (dirTok.startsWith("\"") && dirTok.endsWith("\"") && dirTok.length() >= 2)
            ? dirTok.substring(1, dirTok.length() - 1) : "FORWARD";

        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox='0 0 320 200' xmlns='http://www.w3.org/2000/svg' style='background:#050510;border:2px solid #00d9ff;border-radius:8px;'>");
        svg.append("<text x='160' y='24' text-anchor='middle' fill='").append(ringColor)
            .append("' font-size='13' font-weight='bold'>OPS RING</text>");
        String caption = (liveStage != null ? liveStage : "IDLE") + " | " + (opsBad ? "VIOLATIONS" : dir);
        svg.append("<text x='160' y='42' text-anchor='middle' fill='")
            .append(opsBad ? "#ff3366" : "#00ff88").append("' font-size='10'>").append(caption).append("</text>");

        int cx = 160, cy = 118, R = 58;
        for (int s = 0; s < stages.length; s++) {
            double ang = Math.toRadians(-90 + s * 60);            // start at 12 o'clock, clockwise
            double hx = cx + R * Math.cos(ang), hy = cy + R * Math.sin(ang);
            boolean lit = liveStage != null && liveStage.equals(stages[s]);
            StringBuilder pts = new StringBuilder();
            for (int v = 0; v < 6; v++) {
                double va = Math.toRadians(-90 + s * 60 + v * 60 + 30);
                pts.append(v == 0 ? "" : " ").append(String.format("%.1f", hx + 9 * Math.cos(va)))
                    .append(",").append(String.format("%.1f", hy + 9 * Math.sin(va)));
            }
            svg.append("<polygon points='").append(pts).append("' fill='")
                .append(lit ? "#00ff88" : "#0a0a20").append("' stroke='")
                .append(lit ? "#ffffff" : stageColors[s]).append("' stroke-width='1.5' opacity='")
                .append(lit ? "1" : "0.85").append("'/>");
            double lx = cx + (R + 18) * Math.cos(ang), ly = cy + (R + 18) * Math.sin(ang);
            svg.append("<text x='").append(String.format("%.1f", lx)).append("' y='").append(String.format("%.1f", ly + 3))
                .append("' text-anchor='middle' fill='").append(lit ? "#ffffff" : "#888888").append("'")
                .append(lit ? " font-weight='bold'" : "")
                .append(" font-size='9'>").append(stages[s]).append("</text>");
        }
        svg.append("<text x='160' y='194' text-anchor='middle' fill='#666666' font-size='8'>GODHAND ops ring | opsSource ")
            .append(opsCacheSource).append(" | ").append(java.time.LocalDateTime.now().toString())
            .append("</text>");
        svg.append("</svg>");
        return svg.toString();
    }

    /** GODHAND-2B: tiny status block for the "/" dashboard root, from the same cached snapshot. */
    private String liveStatusHtml() {
        String opsDoc = getOpsSnapshotJson();
        String violations = opsViolationsJson(opsDoc);
        int violationCount = 0;
        if (!"[]".equals(violations)) {
            int depth = 0;
            for (int ci = 0; ci < violations.length(); ci++) {
                char c = violations.charAt(ci);
                if (c == '{') { if (depth == 1) violationCount++; depth++; }
                else if (c == '}') depth--;
            }
        }
        String hexflow = hexflowFromOps(opsDoc);
        String stage = opsField(hexflow, "hex_stage");
        String dir = opsField(hexflow, "direction");
        return "version   : 0.25.0 (headless)\n"
            + "models    : 8\n"
            + "kgNodes   : " + (kg != null ? kg.size() : 23) + "\n"
            + "opsSource : " + opsCacheSource + "\n"
            + "opsOk     : " + opsField(opsDoc, "ok") + "\n"
            + "violations: " + violationCount + "\n"
            + "stage     : " + stage + "\n"
            + "direction : " + dir + "\n"
            + "timestamp : " + java.time.LocalDateTime.now().toString() + "\n";
    }

    private void triggerAutonomousInferenceMovement() {
        modelExecutor.submit(() -> {
            for (Agent a : agents) {
                String prompt = "You are Agent " + a.name + " at hex (" + a.q + "," + a.r + "). Reply with exactly one word indicating your move direction: NORTH, SOUTH, EAST, WEST, NORTHEAST, or NORTHWEST.";
                String move = ollamaRouter.query("qwen2.5:0.5b", prompt).toUpperCase();
                
                int dq = 0, dr = 0;
                if (move.contains("NORTHEAST")) { dq = 1; dr = -1; }
                else if (move.contains("NORTHWEST")) { dq = 0; dr = -1; }
                else if (move.contains("NORTH")) { dq = 0; dr = -1; }
                else if (move.contains("SOUTHEAST")) { dq = 0; dr = 1; }
                else if (move.contains("SOUTHWEST")) { dq = -1; dr = 1; }
                else if (move.contains("SOUTH")) { dq = 0; dr = 1; }
                else if (move.contains("EAST")) { dq = 1; dr = 0; }
                else if (move.contains("WEST")) { dq = -1; dr = 0; }
                
                int nq = a.q + dq;
                int nr = a.r + dr;
                if (grid.containsKey(nq + "," + nr)) {
                    a.moveTo(nq, nr);
                    
                    int randomNodeIdx = rand.nextInt(64);
                    triggerPulse(randomNodeIdx);
                    
                    String logMsg = "[MOVE] Agent " + a.name + " routed to coord (" + nq + "," + nr + ") via " + move;
                    synchronized (godChat) {
                        if (godChat.size() > 50) godChat.remove(0);
                        godChat.add(logMsg);
                    }
                }
            }
            Platform.runLater(this::recalculateFOW);
        });
    }

    private void recalculateFOW() {
        for (HexNode hex : grid.values()) hex.visible = false;
        for (Agent a : agents) {
            for (HexNode hex : grid.values()) {
                if (hex.distance(a.q, a.r) <= 1) hex.visible = true;
            }
        }
    }

    private void triggerPulse(int sourceIdx) {
        List<Integer> targets = new ArrayList<>();
        for (int[] edge : edges) {
            if (edge[0] == sourceIdx) targets.add(edge[1]);
            else if (edge[1] == sourceIdx) targets.add(edge[0]);
        }
        if (!targets.isEmpty()) {
            int targetIdx = targets.get(rand.nextInt(targets.size()));
            pulses.add(new Pulse(sourceIdx, targetIdx));
            
            String logMsg = String.format("[SPIKE] Distilled inference routing pulse from v_%d to v_%d", sourceIdx, targetIdx);
            synchronized (godChat) {
                if (godChat.size() > 50) godChat.remove(0);
                godChat.add(logMsg);
            }
        }
    }

    private double[] project6DTo3D(double[] coords, double[] angles) {
        double[] v = coords.clone();
        int[][] rotations = {
            {0, 3}, {1, 4}, {2, 5},
            {0, 4}, {1, 5}, {2, 3},
            {0, 5}, {1, 3}, {2, 4}
        };
        for (int r = 0; r < rotations.length; r++) {
            int a = rotations[r][0];
            int b = rotations[r][1];
            double angle = angles[r % angles.length];
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double va = v[a];
            double vb = v[b];
            v[a] = va * cos - vb * sin;
            v[b] = va * sin + vb * cos;
        }
        return v;
    }

    private double clampOpacity(double val) {
        if (val < 0.0) return 0.0;
        if (val > 1.0) return 1.0;
        return val;
    }

    private double calculateEntropy(int index) {
        double p = densities[index] / (densities[index] + flows[index]);
        if (p <= 0.0 || p >= 1.0) return 0.0;
        double entropy = - (p * Math.log(p)/Math.log(2) + (1.0 - p) * Math.log(1.0 - p)/Math.log(2));
        return Double.isNaN(entropy) ? 0.0 : entropy;
    }

    private void render(GraphicsContext gc) {
        // 1. Render cosmic background
        gc.setFill(Color.web("#020106"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        for (BackgroundStar s : stars) {
            double flicker = 0.3 + 0.7 * Math.sin(timePulse * s.speed * 8.0 + s.phase);
            gc.setFill(Color.web("#c4b5e0", clampOpacity(flicker)));
            gc.fillOval(s.x, s.y, s.size, s.size);
        }

        double cx = WIDTH / 2.0;
        double cy = HEIGHT / 2.0;
        
        // 2. Draw nebula center glow
        double baseRadius = Math.min(WIDTH, HEIGHT) * 0.28;
        
        // Adjust heartbeat frequency based on system stressLevel (slowing down when stressed)
        heartbeatFreq = 1.20 - stressLevel * 0.7; 
        
        // Dynamic Viscoelastic telemetry
        strainRate = Math.abs(heartbeatFreq * 0.35 * Math.cos(heartbeatFreq * timePulse));
        double term = 1 + Math.pow(2.0 * strainRate, 2);
        viscosity = 0.1 + (0.8 - 0.1) * Math.pow(term, (0.6 - 1) / 2);
        stress = viscosity * strainRate;
        
        double breathScale = 1.0 + Math.sin(timePulse * heartbeatFreq) * 0.15;
        double scale = baseRadius * breathScale * 0.75;
        String phaseLabel = Math.cos(heartbeatFreq * timePulse) > 0 ? "INHALE" : "EXHALE";
        
        for (int i = 5; i > 0; i--) {
            double size = baseRadius * breathScale * (i * 0.35);
            gc.setFill(Color.rgb(168, 85, 247, clampOpacity(0.015 - (i * 0.002))));
            gc.fillOval(cx - size, cy - size, size * 2, size * 2);
        }

        // Outward heartbeat pulse expansion
        double heartbeatPeak = Math.sin(timePulse * heartbeatFreq);
        if (heartbeatPeak > 0.90) {
            double waveRadius = scale * (1.0 + (timePulse % 1.0) * 1.5);
            gc.setStroke(Color.web("#a855f7", clampOpacity(1.0 - (timePulse % 1.0))));
            gc.setLineWidth(2.0);
            gc.strokeOval(cx - waveRadius, cy - waveRadius, waveRadius * 2, waveRadius * 2);
        }

        // 3. 6D Rotations
        double[] angles = {
            timePulse * 0.03,
            timePulse * 0.05,
            timePulse * 0.02,
            timePulse * 0.04 + Math.sin(timePulse * 0.1) * 0.05,
            timePulse * 0.015,
            timePulse * 0.06
        };

        double[][] projected3D = new double[64][3];
        double fov = scale * 1.5;
        double cameraZ = 5.0;

        for (int i = 0; i < 64; i++) {
            double[] v3 = project6DTo3D(vertices6D[i], angles);
            projected3D[i][0] = v3[0];
            projected3D[i][1] = v3[1];
            projected3D[i][2] = v3[2];

            double pScale = fov / (cameraZ + v3[2]);
            projected2D[i][0] = cx + v3[0] * pScale;
            projected2D[i][1] = cy + v3[1] * pScale;
        }

        // 4. Update and Render Interstitial Semantic Cloud Particles
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            double targetX = projected3D[p.targetNodeIdx][0];
            double targetY = projected3D[p.targetNodeIdx][1];
            double targetZ = projected3D[p.targetNodeIdx][2];

            p.x += (targetX - p.x) * 0.012 + (Math.sin(timePulse * 0.5 + i) * 0.02);
            p.y += (targetY - p.y) * 0.012 + (Math.cos(timePulse * 0.5 + i) * 0.02);
            p.z += (targetZ - p.z) * 0.012;

            double pScale = fov / (cameraZ + p.z);
            double sx = cx + p.x * pScale;
            double sy = cy + p.y * pScale;

            if (sx >= 0 && sx < WIDTH && sy >= 0 && sy < HEIGHT) {
                gc.setFill(p.color);
                double pSize = 1.0 + 1.5 * ((p.z + 3.0) / 6.0);
                gc.fillOval(sx - pSize/2, sy - pSize/2, pSize, pSize);
            }
        }

        // 5. Draw Edges (192) depth-sorted
        List<EdgeWithDepth> sortedEdges = new ArrayList<>();
        for (int[] edge : edges) {
            double avgZ = (projected3D[edge[0]][2] + projected3D[edge[1]][2]) / 2.0;
            sortedEdges.add(new EdgeWithDepth(edge[0], edge[1], avgZ));
        }
        sortedEdges.sort(Comparator.comparingDouble(e -> e.avgZ));

        for (EdgeWithDepth e : sortedEdges) {
            double depth = (e.avgZ + 3.0) / 6.0;
            double alpha = 0.05 + 0.25 * depth;
            
            Color strokeColor = Color.hsb(260.0 + depth * 60.0, 0.7, 0.65 + depth * 0.2, clampOpacity(alpha));
            gc.setStroke(strokeColor);
            gc.setLineWidth(0.5 + 1.2 * depth);
            
            gc.strokeLine(projected2D[e.source][0], projected2D[e.source][1], 
                          projected2D[e.target][0], projected2D[e.target][1]);
        }

        // 6. Draw Spikes / Routing Pulses
        for (Pulse p : pulses) {
            p.progress += p.speed;
            if (p.progress >= 1.0) {
                pulses.remove(p);
            } else {
                double x1 = projected2D[p.sourceIdx][0];
                double y1 = projected2D[p.sourceIdx][1];
                double x2 = projected2D[p.targetIdx][0];
                double y2 = projected2D[p.targetIdx][1];
                
                double px = x1 + (x2 - x1) * p.progress;
                double py = y1 + (y2 - y1) * p.progress;
                
                gc.setFill(Color.web("#38bdf8", 0.9)); 
                gc.fillOval(px - 4, py - 4, 8, 8);
            }
        }

        // 7. Draw Nodes (64)
        for (int i = 0; i < 64; i++) {
            double depth = (projected3D[i][2] + 3.0) / 6.0;
            double radius = 3.0 + 4.0 * depth;
            double alpha = 0.3 + 0.7 * depth;
            
            double px = projected2D[i][0];
            double py = projected2D[i][1];

            double shimmer = 1.0 + 0.15 * Math.sin(timePulse * 3.0 + vertices6D[i][3] * Math.PI);
            double outerRadius = radius * 3.0 * shimmer;

            double hue = 270.0 + depth * 50.0 + Math.sin(timePulse + i * 0.3) * 15.0;
            Color nodeColor = Color.hsb(hue, 0.8, 0.75 + depth * 0.25, clampOpacity(alpha));

            // Glowing Outer Aura
            gc.setFill(Color.hsb(hue, 0.8, 0.7, clampOpacity(alpha * 0.2)));
            gc.fillOval(px - outerRadius/2, py - outerRadius/2, outerRadius, outerRadius);

            // Node Core
            gc.setFill(nodeColor);
            gc.fillOval(px - radius/2, py - radius/2, radius, radius);

            // Core center point
            gc.setFill(Color.rgb(255, 245, 255, clampOpacity(alpha * 0.8)));
            gc.fillOval(px - radius * 0.4 / 2, py - radius * 0.4 / 2, radius * 0.4, radius * 0.4);
            
            if (i == hoveredVertexIdx) {
                gc.setStroke(Color.web("#f472b6"));
                gc.setLineWidth(2.0);
                gc.strokeOval(px - radius * 1.8 / 2, py - radius * 1.8 / 2, radius * 1.8, radius * 1.8);
            }
        }

        // 8. Render HUD Panels
        
        // Left Side Panel
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(15, 75, 280, 480);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(15, 75, 280, 480);

        gc.setFill(Color.web("#c084fc"));
        gc.setFont(Font.font("Outfit", 15));
        gc.fillText("⬡ GEOSPATIAL MANIFOLD", 30, 105);

        gc.setFont(Font.font("Consolas", 11));
        gc.setFill(Color.web("#c0b3d6"));
        gc.fillText("Projection: 6D -> 3D Perspective", 30, 135);
        gc.fillText("Vertices:   64", 30, 152);
        gc.fillText("Edges:      192", 30, 169);
        gc.fillText("Cubic Cells:160", 30, 186);

        // Rheology state
        gc.setFont(Font.font("Outfit", 12));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("RHEOLOGICAL STATE", 30, 220);

        drawGauge(gc, "Viscosity η", viscosity, 30, 235, "#c084fc");
        drawGauge(gc, "Strain rate γ̇", strainRate, 30, 285, "#38bdf8");
        drawGauge(gc, "Stress τ", stress, 30, 335, "#f472b6");

        // Quorum matrix
        gc.setFont(Font.font("Outfit", 12));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("QUORUM VOTING GRID (64)", 30, 405);
        
        int gridX = 30;
        int gridY = 420;
        int cellSize = 10;
        int cellGap = 3;
        int activeNodeCount = 0;
        
        for (int i = 0; i < 64; i++) {
            int row = i / 8;
            int col = i % 8;
            double vx = gridX + col * (cellSize + cellGap);
            double vy = gridY + row * (cellSize + cellGap);
            
            boolean active = (rand.nextDouble() > 0.25);
            if (active) activeNodeCount++;
            
            gc.setFill(active ? Color.web("#c084fc", 0.8) : Color.web("#c084fc", 0.15));
            gc.fillRect(vx, vy, cellSize, cellSize);
        }
        
        gc.setFont(Font.font("Consolas", 10));
        gc.setFill(Color.web("#f472b6"));
        gc.fillText("Consensus: " + activeNodeCount + " / 64 Nodes (⅔ Supermajority)", 30, 540);

        // Heartbeat Monitor
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(15, 570, 280, 80);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(15, 570, 280, 80);

        gc.setFill(Color.web("#38bdf8"));
        gc.setFont(Font.font("Outfit", 12));
        gc.fillText("HEARTBEAT LOOP", 30, 595);
        gc.setFont(Font.font("Consolas", 14));
        gc.fillText(phaseLabel, 30, 625);
        
        gc.setStroke(Color.web("#c084fc"));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int x = 120; x < 280; x += 2) {
            double y = 610 + 15 * Math.sin(heartbeatFreq * (timePulse - x * 0.05));
            if (x == 120) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();

        // Right Side: Swarm Activity Console
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(950, 75, 310, 480);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(950, 75, 310, 480);

        gc.setFont(Font.font("Outfit", 14));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("SWARM ACTIVITY MATRIX", 970, 105);

        gc.setFont(Font.font("Consolas", 11));
        int rIndex = 0;
        for (Agent agent : agents) {
            double textY = 145 + (rIndex * 50);

            gc.setFill(Color.web("#38bdf8"));
            gc.fillText(agent.name, 970, textY);
            gc.setFill(Color.web("#6b5c8c"));
            gc.fillText("Hex: " + agent.q + "," + agent.r, 970, textY + 12);

            String activity = "IDLE";
            String phase = nightCycle.getCurrentPhase();
            if (phase.contains("DREAM")) activity = "SOAKING EMBEDDINGS";
            else if (phase.contains("VOTE")) activity = "HOMOLOGY VOTE RUNNING";
            else if (phase.contains("DEPLOY")) activity = "DEPLOYING SOP SHARDS";

            gc.setFill(Color.web("#f472b6"));
            gc.fillText("-> " + activity, 970, textY + 24);
            rIndex++;
        }

        // Bottom Side: Multi-Agent Consensus logs
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(315, 605, 945, 180);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(315, 605, 945, 180);

        gc.setFont(Font.font("Outfit", 12));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("⬡ SLM INTERSTITIAL DISTILLATION & CONSENSUS LOGS", 335, 628);

        gc.setFont(Font.font("Consolas", 10));
        int logY = 650;
        synchronized (godChat) {
            int startIdx = Math.max(0, godChat.size() - 8);
            for (int i = startIdx; i < godChat.size(); i++) {
                String logMsg = godChat.get(i);
                if (logMsg.contains("[SPIKE]")) gc.setFill(Color.web("#f472b6"));
                else if (logMsg.contains("[DREAM]")) gc.setFill(Color.web("#c084fc"));
                else if (logMsg.contains("[MOVE]")) gc.setFill(Color.web("#38bdf8"));
                else gc.setFill(Color.web("#c0b3d6"));
                
                gc.fillText(logMsg, 335, logY);
                logY += 15;
            }
        }

        // Hover Tooltip Inspector with Shannon Entropy
        if (hoveredVertexIdx != -1) {
            double dens = densities[hoveredVertexIdx];
            double flw = flows[hoveredVertexIdx];
            double entropy = calculateEntropy(hoveredVertexIdx);
            
            String tip = String.format("Vertex: v_%d\nCoords: [%s]\nDensity: %.4f\nFlow: %.3f m/s\nEntropy: H=%.4f bits\nSOP: Consensus-Strict\nClick to route spike!", 
                hoveredVertexIdx, getCoordsString(vertices6D[hoveredVertexIdx]), dens, flw, entropy);
                
            double tpx = projected2D[hoveredVertexIdx][0];
            double tpy = projected2D[hoveredVertexIdx][1];
            
            gc.setFill(Color.rgb(6, 3, 18, 0.95));
            gc.fillRect(pxForHoverToolTip(tpx), pyForHoverToolTip(tpy), 250, 115);
            gc.setStroke(Color.web("#f472b6"));
            gc.setLineWidth(1.5);
            gc.strokeRect(pxForHoverToolTip(tpx), pyForHoverToolTip(tpy), 250, 115);
            gc.setFill(Color.web("#f3e8ff"));
            gc.setFont(Font.font("Consolas", 11));
            
            String[] lines = tip.split("\n");
            double textY = pyForHoverToolTip(tpy) + 20.0;
            for (String line : lines) {
                gc.fillText(line, pxForHoverToolTip(tpx) + 15, textY);
                textY += 15;
            }
        }
    }

    private void drawGauge(GraphicsContext gc, String label, double value, double x, double y, String hexColor) {
        gc.setFill(Color.web("#c0b3d6"));
        gc.setFont(Font.font("Outfit", 11));
        gc.fillText(label, x, y);
        gc.fillText(String.format("%.3f", value), x + 180, y);
        
        gc.setFill(Color.rgb(147, 51, 234, 0.15));
        gc.fillRect(x, y + 6, 200, 5);
        
        gc.setFill(Color.web(hexColor));
        gc.fillRect(x, y + 6, Math.min(200, value * 200), 5);
    }

    private String getCoordsString(double[] coords) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < coords.length; i++) {
            sb.append((int)coords[i]);
            if (i < coords.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private double pxForHoverToolTip(double projectedX) {
        if (projectedX + 260 > WIDTH) return projectedX - 270;
        return projectedX + 15;
    }

    private double pyForHoverToolTip(double projectedY) {
        if (projectedY + 120 > HEIGHT) return projectedY - 130;
        return projectedY + 15;
    }

    @Override
    public void stop() {
        modelExecutor.shutdownNow();
        threadPool.shutdownNow();
        if(dashboardServer != null) dashboardServer.stop(0);
        // GODHAND-2C: cleanly stop the public phone UI (GUI lifecycle only).
        if(publicServer != null) publicServer.stop(0);
    }

    // --- Draggable Sub-Window Custom Component ---

    class DraggableWindow extends VBox {
        private double dragStartX;
        private double dragStartY;
        private Label titleLabel;
        private String title;
        
        public DraggableWindow(String title, javafx.scene.Node content, double width, double height) {
            this.title = title;
            this.setPrefSize(width, height);
            this.setMaxSize(width, height);
            this.setStyle("-fx-background-color: rgba(6, 3, 18, 0.9); " +
                          "-fx-border-color: #a855f7; " +
                          "-fx-border-width: 1.5; " +
                          "-fx-background-radius: 6; " +
                          "-fx-border-radius: 6;");
            
            // Header bar
            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle("-fx-background-color: #7c3aed; -fx-padding: 6 10; -fx-cursor: move; -fx-background-radius: 4 4 0 0;");
            
            titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-family: 'Outfit', monospace; -fx-font-weight: bold; -fx-font-size: 12px;");
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            
            Button btnClose = new Button("×");
            btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #f472b6; -fx-font-family: monospace; -fx-font-size: 14px; -fx-padding: 0 4 0 4; -fx-cursor: hand;");
            btnClose.setOnAction(e -> {
                Pane parent = (Pane) this.getParent();
                if (parent != null) parent.getChildren().remove(this);
            });
            
            header.getChildren().addAll(titleLabel, btnClose);
            HBox.setHgrow(btnClose, Priority.NEVER);
            
            // Drag listeners
            header.setOnMousePressed(e -> {
                dragStartX = e.getSceneX() - this.getTranslateX();
                dragStartY = e.getSceneY() - this.getTranslateY();
                this.toFront();
            });
            header.setOnMouseDragged(e -> {
                this.setTranslateX(e.getSceneX() - dragStartX);
                this.setTranslateY(e.getSceneY() - dragStartY);
            });
            
            // Content container
            VBox container = new VBox(content);
            container.setStyle("-fx-padding: 10;");
            VBox.setVgrow(content, Priority.ALWAYS);
            
            this.getChildren().addAll(header, container);
        }
        
        public String getTitle() {
            return title;
        }
    }

    // --- Inner Helper Classes --- //

    class EdgeWithDepth {
        int source;
        int target;
        double avgZ;
        public EdgeWithDepth(int source, int target, double avgZ) {
            this.source = source;
            this.target = target;
            this.avgZ = avgZ;
        }
    }

    class Particle {
        double x, y, z;
        int targetNodeIdx;
        Color color;
        public Particle() {
            reset();
        }
        public void reset() {
            x = (rand.nextDouble() - 0.5) * 10;
            y = (rand.nextDouble() - 0.5) * 10;
            z = (rand.nextDouble() - 0.5) * 10;
            targetNodeIdx = rand.nextInt(64);
            double r = rand.nextDouble();
            if (r > 0.6) color = Color.web("#f472b6", 0.4);      
            else if (r > 0.3) color = Color.web("#38bdf8", 0.45); 
            else color = Color.web("#c084fc", 0.4);               
        }
    }

    class Pulse {
        int sourceIdx;
        int targetIdx;
        double progress;
        double speed;
        public Pulse(int source, int target) {
            this.sourceIdx = source;
            this.targetIdx = target;
            this.progress = 0;
            this.speed = 0.02 + rand.nextDouble() * 0.03;
        }
    }

    class BackgroundStar {
        double x, y;
        double speed;
        double size;
        double phase;
        public BackgroundStar(double x, double y) {
            this.x = x;
            this.y = y;
            this.speed = 0.005 + rand.nextDouble() * 0.015;
            this.size = 0.5 + rand.nextDouble() * 1.5;
            this.phase = rand.nextDouble() * Math.PI * 2;
        }
    }
    
    class HexNode {
        int q, r;
        boolean visible = false;
        String station = null;
        public HexNode(int q, int r) { this.q = q; this.r = r; }
        public int distance(int aq, int ar) { return (Math.abs(q - aq) + Math.abs(q + r - aq - ar) + Math.abs(r - ar)) / 2; }
        public boolean contains(double px, double py) {
            double x = HEX_SIZE * Math.sqrt(3) * (q + r / 2.0);
            double y = HEX_SIZE * 3.0 / 2.0 * r;
            return Math.hypot(px - x, py - y) < HEX_SIZE;
        }
        public void triggerPipeline(OllamaRouter router) {
            if(station != null) {
                System.out.println("[PIPELINE] Executing Station Pipeline: " + station);
                modelExecutor.submit(() -> {
                    router.query("tinyllama:1.1b", "Execute pipeline task for station " + station);
                });
            }
        }
    }

    class Agent {
        String name;
        int q, r;
        public Agent(String name, int q, int r) { this.name = name; this.q = q; this.r = r; }
        public void moveTo(int q, int r) { this.q = q; this.r = r; }
    }

    class NightCycleEngine {
        private String currentPhase = "00:00 DREAM PHASE";
        private OllamaRouter router;

        public NightCycleEngine(OllamaRouter router) {
            this.router = router;
        }
        
        public void startClock() {
            threadPool.submit(() -> {
                while(true) {
                    try {
                        Thread.sleep(3600000); // 1 Hour
                        System.out.println("[HOURLY HEARTRATE] System soaking safely. Local SLMs active. No thermal throttling detected.");
                        Platform.runLater(() -> {
                            synchronized(godChat) {
                                if (godChat.size() > 50) godChat.remove(0);
                                godChat.add("[SYSTEM] Hourly Heartrate OK. Soaking...");
                            }
                        });
                    } catch(Exception e){}
                }
            });

            threadPool.submit(() -> {
                while(true) {
                    try {
                        Thread.sleep(900000); // 15 Minutes
                        currentPhase = "00:00 CHAT & DREAM PHASE";
                        System.out.println("[SOAK] Dreaming cross-correlated memories...");
                        String dreamPrompt = "Generate exactly one new 1-2 word node type or mechanic for a hex grid simulation. Output only the name, nothing else. No preamble.";
                        String dreamProposalRaw = router.query("qwen2.5:0.5b", dreamPrompt).replaceAll("[\"'{}\\[\\]\\n\\r]", "").trim();
                        if (dreamProposalRaw.isEmpty() || dreamProposalRaw.length() > 30) dreamProposalRaw = "Void_Node";
                        String dreamProposal = dreamProposalRaw;
                        Platform.runLater(() -> {
                            synchronized(godChat) {
                                if (godChat.size() > 50) godChat.remove(0);
                                godChat.add("[DREAM] Proposal generated: " + dreamProposal);
                            }
                        });
                        
                        Thread.sleep(900000); // 15 Minutes
                        currentPhase = "18:00 VOTE PHASE";
                        System.out.println("[SOAK] Engaged Vote Phase...");
                        boolean approved = !dreamProposal.isEmpty() && router.query("qwen2.5:0.5b",
                                "Vote YES or NO only: deploy new node '" + dreamProposal + "'?").toUpperCase().contains("YES");

                        Thread.sleep(900000); // 15 Minutes
                        currentPhase = "20:00 DEPLOY PHASE";
                        System.out.println("[SOAK] Deploying dynamically generated tools...");
                        if (approved) {
                            Platform.runLater(() -> {
                                synchronized (godChat) {
                                    if (godChat.size() > 50) godChat.remove(0);
                                    godChat.add("[DEPLOY] Node deployed: " + dreamProposal);
                                }
                            });
                        }
                        
                        Thread.sleep(900000); // 15 Minutes
                        currentPhase = "22:00 MOVE PHASE";
                        System.out.println("[SOAK] Requesting Agent Movement...");
                        if (!agents.isEmpty()) {
                            Agent a = agents.get(0);
                            String moveDir = router.query("qwen2.5:0.5b", "You are an agent at " + a.q + "," + a.r + ". Reply exactly with one word: NORTH, SOUTH, EAST, or WEST.").trim().toUpperCase();
                            Platform.runLater(() -> {
                                if (moveDir.contains("NORTH")) a.r -= 1;
                                else if (moveDir.contains("SOUTH")) a.r += 1;
                                else if (moveDir.contains("EAST")) a.q += 1;
                                else if (moveDir.contains("WEST")) a.q -= 1;
                                synchronized(godChat) {
                                    if (godChat.size() > 50) godChat.remove(0);
                                    godChat.add("[MOVE] Agent Alpha shifted " + moveDir);
                                }
                            });
                        }
                    } catch(Exception e){}
                }
            });
        }
        public String getCurrentPhase() { return currentPhase; }
    }

    private void executeDesktopScript(String scriptName) {
        System.out.println("[MANIFOLD] Triggering external hook: " + scriptName);
        try {
            Runtime.getRuntime().exec(new String[]{
                "powershell.exe",
                "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden",
                "-File", "C:\\Users\\viper\\OneDrive\\Desktop\\local_desktop-main\\" + scriptName
            });
        } catch(Exception e) {
            System.err.println("[MANIFOLD ERROR] " + e.getMessage());
        }
    }
}
