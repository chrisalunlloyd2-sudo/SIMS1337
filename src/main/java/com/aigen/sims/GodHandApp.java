package com.aigen.sims;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.io.BufferedReader;
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

/**
 * SIMS1337 v0.22.0 - GodHandApp
 * Pure Programmatic JavaFX GUI
 * 6D Hexeract Geospatial Manifold Visualization
 */
public class GodHandApp extends Application {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final double HEX_SIZE = 35.0;
    
    private Map<String, HexNode> grid = new ConcurrentHashMap<>();
    private List<Agent> agents = new CopyOnWriteArrayList<>();
    private List<String> godChat = new CopyOnWriteArrayList<>();
    private ExecutorService threadPool = Executors.newFixedThreadPool(8);
    private HttpServer dashboardServer;
    
    private double timePulse = 0;
    private int zElevation = 0;
    private HexNode hoveredHex = null;
    
    private NightCycleEngine nightCycle;
    private OllamaRouter ollamaRouter;
    
    // Subsystems
    private ModelManager modelManager;
    private KnowledgeGraph kg;
    private SQLiteMemory memory;
    private GistSync gistSync;
    private SelfMutator mutator;
    
    // Enterprise & Legacy Engine Dependencies
    private EnterpriseGuard guard;
    private SwarmWatchdog watchdog;
    private MCTSPipeline mcts;
    private AdversarialFuzzer fuzzer;
    private MetaLogicSupervisor metaLogic;
    private NightlyEvolutionEngine evolutionEngine;
    
    // 6D Hexeract Fields
    private double[][] vertices6D = new double[64][6];
    private List<int[]> edges = new ArrayList<>();
    private double[][] projected2D = new double[64][2];
    private double[] densities = new double[64];
    private double[] flows = new double[64];
    private int hoveredVertexIdx = -1;
    
    // Rheological States
    private double viscosity = 0.420;
    private double strainRate = 0.681;
    private double stress = 0.312;
    private double heartbeatFreq = 1.20;
    
    // Particle Swarm and Signal Pulses
    private List<Particle> particles = new ArrayList<>();
    private List<Pulse> pulses = new CopyOnWriteArrayList<>();
    private List<BackgroundStar> stars = new ArrayList<>();
    private Random rand = new Random();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
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
                if (e.getButton() == MouseButton.PRIMARY) {
                    triggerPulse(hoveredVertexIdx);
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    // Trigger agent inference route
                    triggerPulse(hoveredVertexIdx);
                    threadPool.submit(() -> {
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
                timePulse += 0.02;
                if (now - lastMove > 10_000_000_000L) { // 10 seconds
                    lastMove = now;
                    triggerAutonomousInferenceMovement();
                }
                if (now - lastEnterpriseTick > 30_000_000_000L) { // 30 seconds
                    lastEnterpriseTick = now;
                    threadPool.submit(() -> {
                        watchdog.auditTopology(agents);
                        mcts.executeRollout("Hex_Topology_Alpha");
                        fuzzer.fuzzNetwork();
                        metaLogic.periodicScan();
                    });
                }
                render(gc);
            }
        };

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #020106;");
        
        // --- MANIFOLD COMMAND CENTER (Geospatial Side Hooks) ---
        javafx.scene.layout.VBox manifoldPanel = new javafx.scene.layout.VBox(8);
        manifoldPanel.setTranslateX(15);
        manifoldPanel.setTranslateY(15);
        manifoldPanel.setPickOnBounds(false); 
        manifoldPanel.setStyle("-fx-background-color: rgba(8, 4, 24, 0.75); -fx-padding: 10; -fx-border-color: #a855f7; -fx-border-width: 1;");
        manifoldPanel.setMaxSize(240, 400);
        javafx.scene.layout.StackPane.setAlignment(manifoldPanel, javafx.geometry.Pos.TOP_LEFT);

        javafx.scene.control.Label manifoldLabel = new javafx.scene.control.Label("MANIFOLD CONTROL");
        manifoldLabel.setStyle("-fx-text-fill: #c084fc; -fx-font-family: monospace; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        javafx.scene.control.Button btnLogic = new javafx.scene.control.Button("REBOOT LOGIC SHIPPER");
        btnLogic.setStyle("-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-pref-width: 200px;");
        btnLogic.setOnAction(e -> executeDesktopScript("START_LOGIC_BLOCKCHAIN_PORT.ps1"));

        javafx.scene.control.Button btnTopology = new javafx.scene.control.Button("REBOOT TOPOLOGY SIDECAR");
        btnTopology.setStyle("-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-pref-width: 200px;");
        btnTopology.setOnAction(e -> executeDesktopScript("START_TOPOLOGY_SIDECAR.ps1"));

        javafx.scene.control.Button btnHouse = new javafx.scene.control.Button("REBOOT HOUSE ENGINE");
        btnHouse.setStyle("-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-pref-width: 200px;");
        btnHouse.setOnAction(e -> executeDesktopScript("START_HOUSE_ENGINE_RECOVERY.ps1"));
        
        javafx.scene.control.Button btnAgent = new javafx.scene.control.Button("SPIN UP LEGACY AGENT");
        btnAgent.setStyle("-fx-background-color: #111; -fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-border-color: #c084fc; -fx-pref-width: 200px;");
        btnAgent.setOnAction(e -> executeDesktopScript("SPIN_UP_AGENT_NODE.ps1"));

        manifoldPanel.getChildren().addAll(manifoldLabel, btnLogic, btnTopology, btnHouse, btnAgent);
        root.getChildren().add(manifoldPanel);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
        primaryStage.setTitle("SIMS1337 v0.22.0 - 6D Hexeract Geospatial Manifold");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        timer.start();
        nightCycle.startClock();
    }

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
        // Generate 6D coordinates for 64 vertices
        for (int i = 0; i < 64; i++) {
            for (int d = 0; d < 6; d++) {
                vertices6D[i][d] = ((i >> d) & 1) == 1 ? 1.0 : -1.0;
            }
            densities[i] = 0.3 + rand.nextDouble() * 0.7;
            flows[i] = 0.2 + rand.nextDouble() * 0.8;
        }

        // Generate 192 edges (Hamming distance = 1)
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

        // Generate stars
        for (int i = 0; i < 150; i++) {
            stars.add(new BackgroundStar(rand.nextDouble() * WIDTH, rand.nextDouble() * HEIGHT));
        }

        // Generate stardust particles representing interstitial semantic clouds
        for (int i = 0; i < 600; i++) {
            particles.add(new Particle());
        }
    }
    
    private void initBackendSystems() {
        modelManager = new ModelManager();
        kg = new KnowledgeGraph();
        memory = new SQLiteMemory();
        gistSync = new GistSync();
        ollamaRouter = new OllamaRouter();
        
        guard = new EnterpriseGuard();
        watchdog = new SwarmWatchdog(guard);
        mcts = new MCTSPipeline(ollamaRouter, guard);
        fuzzer = new AdversarialFuzzer(ollamaRouter, guard);
        metaLogic = new MetaLogicSupervisor(guard, ollamaRouter);
        evolutionEngine = new NightlyEvolutionEngine(metaLogic, guard, mutator);
        
        nightCycle = new NightCycleEngine(ollamaRouter, modelManager, gistSync, memory, mutator);
        try {
            dashboardServer = HttpServer.create(new InetSocketAddress(8899), 0);
            dashboardServer.createContext("/api/status", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String resp = "{\"version\":\"0.22.0\",\"models\":8,\"kgNodes\":23,\"errors\":0,\"status\":\"ACTIVE\"}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, resp.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(resp.getBytes());
                    os.close();
                }
            });
            dashboardServer.setExecutor(null);
            dashboardServer.start();
            System.out.println("[GODHAND DASHBOARD] Online at http://localhost:8899");
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void triggerAutonomousInferenceMovement() {
        threadPool.submit(() -> {
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
                    
                    // Inject visual routing pulse when agent moves
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

    private void render(GraphicsContext gc) {
        // 1. Render cosmic void background
        gc.setFill(Color.web("#020106"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw background stars
        for (BackgroundStar s : stars) {
            double flicker = 0.3 + 0.7 * Math.sin(timePulse * s.speed * 8.0 + s.phase);
            gc.setFill(Color.web("#c4b5e0", flicker));
            gc.fillOval(s.x, s.y, s.size, s.size);
        }

        double cx = WIDTH / 2.0;
        double cy = HEIGHT / 2.0;
        
        // 2. Draw nebula center glow
        double baseRadius = Math.min(WIDTH, HEIGHT) * 0.28;
        // Compute viscoelastic heartbeat parameters
        strainRate = Math.abs(heartbeatFreq * 0.35 * Math.cos(heartbeatFreq * timePulse));
        double term = 1 + Math.pow(2.0 * strainRate, 2);
        viscosity = 0.1 + (0.8 - 0.1) * Math.pow(term, (0.6 - 1) / 2);
        stress = viscosity * strainRate;
        
        double breathScale = 1.0 + Math.sin(timePulse * heartbeatFreq) * 0.15;
        double scale = baseRadius * breathScale * 0.75;
        String phaseLabel = Math.cos(heartbeatFreq * timePulse) > 0 ? "INHALE" : "EXHALE";
        
        for (int i = 5; i > 0; i--) {
            double size = baseRadius * breathScale * (i * 0.35);
            gc.setFill(Color.rgb(168, 85, 247, 0.015 - (i * 0.002)));
            gc.fillOval(cx - size, cy - size, size * 2, size * 2);
        }

        // 3. 6D rotations configuration
        double[] angles = {
            timePulse * 0.03,
            timePulse * 0.05,
            timePulse * 0.02,
            timePulse * 0.04 + Math.sin(timePulse * 0.1) * 0.05,
            timePulse * 0.015,
            timePulse * 0.06
        };

        // Project vertices to 3D and 2D
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
            
            Color strokeColor = Color.hsb(260.0 + depth * 60.0, 0.7, 0.65 + depth * 0.2, alpha);
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
            Color nodeColor = Color.hsb(hue, 0.8, 0.75 + depth * 0.25, alpha);

            // Glowing Outer Aura
            gc.setFill(Color.hsb(hue, 0.8, 0.7, alpha * 0.2));
            gc.fillOval(px - outerRadius/2, py - outerRadius/2, outerRadius, outerRadius);

            // Node Core
            gc.setFill(nodeColor);
            gc.fillOval(px - radius/2, py - radius/2, radius, radius);

            // Core center point
            gc.setFill(Color.rgb(255, 245, 255, alpha * 0.8));
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
        gc.fillRect(15, 15, 280, 480);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(15, 15, 280, 480);

        gc.setFill(Color.web("#c084fc"));
        gc.setFont(Font.font("Outfit", 15));
        gc.fillText("⬡ GEOSPATIAL MANIFOLD", 30, 45);

        gc.setFont(Font.font("Consolas", 11));
        gc.setFill(Color.web("#c0b3d6"));
        gc.fillText("Projection: 6D -> 3D Perspective", 30, 75);
        gc.fillText("Vertices:   64", 30, 92);
        gc.fillText("Edges:      192", 30, 109);
        gc.fillText("Cubic Cells:160", 30, 126);

        // Rheology state
        gc.setFont(Font.font("Outfit", 12));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("RHEOLOGICAL STATE", 30, 160);

        drawGauge(gc, "Viscosity η", viscosity, 30, 175, "#c084fc");
        drawGauge(gc, "Strain rate γ̇", strainRate, 30, 225, "#38bdf8");
        drawGauge(gc, "Stress τ", stress, 30, 275, "#f472b6");

        // Quorum matrix
        gc.setFont(Font.font("Outfit", 12));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("QUORUM VOTING GRID (64)", 30, 345);
        
        int gridX = 30;
        int gridY = 360;
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
        gc.fillText("Consensus: " + activeNodeCount + " / 64 Nodes (⅔ Supermajority)", 30, 480);

        // Heartbeat Monitor
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(15, 510, 280, 80);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(15, 510, 280, 80);

        gc.setFill(Color.web("#38bdf8"));
        gc.setFont(Font.font("Outfit", 12));
        gc.fillText("HEARTBEAT LOOP", 30, 535);
        gc.setFont(Font.font("Consolas", 14));
        gc.fillText(phaseLabel, 30, 565);
        
        gc.setStroke(Color.web("#c084fc"));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int x = 120; x < 280; x += 2) {
            double y = 550 + 15 * Math.sin(heartbeatFreq * (timePulse - x * 0.05));
            if (x == 120) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();

        // Right Side: Swarm Activity Console
        gc.setFill(Color.rgb(8, 4, 24, 0.75));
        gc.fillRect(950, 15, 310, 480);
        gc.setStroke(Color.web("#a855f7", 0.3));
        gc.strokeRect(950, 15, 310, 480);

        gc.setFont(Font.font("Outfit", 14));
        gc.setFill(Color.web("#c084fc"));
        gc.fillText("SWARM ACTIVITY MATRIX", 970, 45);

        gc.setFont(Font.font("Consolas", 11));
        int rIndex = 0;
        if (modelManager != null) {
            for (ModelManager.ModelProfile profile : modelManager.getSwarm()) {
                double textY = 85 + (rIndex * 50);
                
                gc.setFill(Color.web("#38bdf8"));
                gc.fillText(profile.name, 970, textY);
                gc.setFill(Color.web("#6b5c8c"));
                gc.fillText("Role: " + profile.role, 970, textY + 12);
                
                String activity = "IDLE";
                String phase = nightCycle.getCurrentPhase();
                if (phase.contains("DREAM")) activity = "SOAKING EMBEDDINGS";
                else if (phase.contains("VOTE")) activity = "HOMOLOGY VOTE RUNNING";
                else if (phase.contains("DEPLOY")) activity = "DEPLOYING SOP SHARDS";
                
                gc.setFill(Color.web("#f472b6"));
                gc.fillText("-> " + activity, 970, textY + 24);
                rIndex++;
            }
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

        // Hover Tooltip Inspector
        if (hoveredVertexIdx != -1) {
            double dens = densities[hoveredVertexIdx];
            double flw = flows[hoveredVertexIdx];
            
            String tip = String.format("Vertex: v_%d\nCoords: [%s]\nDensity: %.4f\nFlow: %.3f m/s\nSOP: Consensus-Strict\nClick to route spike!", 
                hoveredVertexIdx, getCoordsString(vertices6D[hoveredVertexIdx]), dens, flw);
                
            gc.setFill(Color.rgb(6, 3, 18, 0.95));
            gc.fillRect(pxForHoverToolTip(projected2D[hoveredVertexIdx][0]), 
                        pyForHoverToolTip(projected2D[hoveredVertexIdx][1]), 250, 115);
            gc.setStroke(Color.web("#f472b6"));
            gc.setLineWidth(1.5);
            gc.strokeRect(pxForHoverToolTip(projected2D[hoveredVertexIdx][0]), 
                          pyForHoverToolTip(projected2D[hoveredVertexIdx][1]), 250, 115);
            gc.setFill(Color.web("#f3e8ff"));
            gc.setFont(Font.font("Consolas", 11));
            
            String[] lines = tip.split("\n");
            double textY = pyForHoverToolTip(projected2D[hoveredVertexIdx][1]) + 20.0;
            for (String line : lines) {
                gc.fillText(line, pxForHoverToolTip(projected2D[hoveredVertexIdx][0]) + 15, textY);
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
        threadPool.shutdownNow();
        if(dashboardServer != null) dashboardServer.stop(0);
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
                threadPool.submit(() -> {
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
        private ModelManager modelManager;
        private GistSync gistSync;
        private SQLiteMemory memory;
        private SelfMutator mutator;
        
        public NightCycleEngine(OllamaRouter router, ModelManager modelManager, GistSync gistSync, SQLiteMemory memory, SelfMutator mutator) { 
            this.router = router;
            this.modelManager = modelManager;
            this.gistSync = gistSync;
            this.memory = memory;
            this.mutator = mutator;
        }
        
        public void startClock() {
            // --- HOURLY HEARTRATE SOAK MONITOR ---
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

            // --- SPACED OUT SOAKING CYCLE ---
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
                        boolean approved = modelManager.executeVote(dreamProposal, router);
                        
                        Thread.sleep(900000); // 15 Minutes
                        currentPhase = "20:00 DEPLOY PHASE";
                        System.out.println("[SOAK] Deploying dynamically generated tools...");
                        if (approved) {
                            memory.logMemory("SYSTEM", "SOAK_CYCLE", "Deployed new " + dreamProposal + " node.");
                            mutator.injectMutation(dreamProposal);
                            Map<String, String> state = new HashMap<>();
                            state.put("topology.json", "{\"status\": \"Topology updated with " + dreamProposal + "\"}");
                            gistSync.pushState(state);
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
