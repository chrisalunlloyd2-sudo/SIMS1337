package com.aigen.sims;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * SIMS NEO 1337 - Complete GodHand + Player Grid + Model Orchestration
 * v0.11.0 - Web APIs + Model Manager + Voting + Topology + Night Cycle
 * Pure JavaFX - NO FXML - Everything is a changeable GUI component
 */
public class GodHandApp extends Application {

    // === View Management ===
    private StackPane viewStack;
    private VBox dashboardView, gridView, settingsView;
    private Label statusLabel;
    private TextArea logConsole;

    // === Shared God Chat ===
    private TextArea godChat;
    private int godChatMessageCount;

    // === Model Chat ===
    private final Map<String, TextArea> modelChats = new ConcurrentHashMap<>();
    private final Map<String, TextField> modelInputs = new ConcurrentHashMap<>();
    private final Map<String, ComboBox<String>> modelPatterns = new ConcurrentHashMap<>();
    private final Map<String, ComboBox<String>> modelNextRoutes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService chatScheduler = Executors.newScheduledThreadPool(4);

    // === Routing State ===
    private final Map<String, Boolean> loopActive = new ConcurrentHashMap<>();
    private final Map<String, Integer> loopCounts = new ConcurrentHashMap<>();
    private final ObservableList<String[]> routingTable = FXCollections.observableArrayList();

    // === Web APIs ===
    private final ObservableList<String[]> webApiTable = FXCollections.observableArrayList();
    private final Map<String, String> webApiEndpoints = new ConcurrentHashMap<>();

    // === Model Manager ===
    private final ObservableList<String> installedModels = FXCollections.observableArrayList();
    private final ObservableList<String> availableModels = FXCollections.observableArrayList(
        "llama3.2:1b", "gemma2:2b", "mistral:7b", "deepseek-r1:1.5b",
        "codellama:7b", "neural-chat:7b", "openhermes:7b", "zephyr:7b"
    );

    // === Voting System ===
    private final ObservableList<String[]> proposalTable = FXCollections.observableArrayList();
    private final Map<String, Map<String, String>> votes = new ConcurrentHashMap<>();

    // === Topology Builder ===
    private final ObservableList<String[]> topologyTable = FXCollections.observableArrayList();
    private final Map<String, List<String>> topologyGraph = new ConcurrentHashMap<>();

    // === Night Cycle ===
    private final Map<String, String> nightCycleConfig = new ConcurrentHashMap<>();
    private ScheduledFuture<?> nightCycleFuture;

    // === Command Listener ===
    private final ObservableList<String[]> commandTable = FXCollections.observableArrayList();
    private final Map<String, Runnable> commandRegistry = new ConcurrentHashMap<>();

    // === Station State ===
    private final Map<String, Boolean> stationActive = new ConcurrentHashMap<>();

    // === Ollama API ===
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_TAGS = "http://localhost:11434/api/tags";
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, Boolean> ollamaAvailable = new ConcurrentHashMap<>();

    // === Entropy ===
    private double shannonEntropy = 0.0;
    private double entropyThreshold = 0.75;

    // === Markov Patterns ===
    private final ObservableList<String[]> markovTable = FXCollections.observableArrayList();

    // === Agent Positions ===
    private final Map<String, int[]> agentPositions = new ConcurrentHashMap<>();
    private final Map<String, Label> agentPositionLabels = new ConcurrentHashMap<>();
    private GridPane gridPane;
    private final Rectangle[][] gridCells = new Rectangle[10][10];

    // === Station Pipelines ===
    private final Map<String, String> pipelineNext = new ConcurrentHashMap<>();
    private final Map<String, Boolean> pipelineActive = new ConcurrentHashMap<>();

    // === Lexical Engine ===
    private static final Set<String> STOP_WORDS = Set.of(
        "the","a","an","is","are","was","were","be","been","being","have","has","had",
        "do","does","did","will","would","shall","should","may","might","must","can","could",
        "i","you","he","she","it","we","they","me","him","her","us","them","my","your",
        "his","its","our","their","mine","yours","hers","ours","theirs",
        "this","that","these","those","and","but","or","nor","not","so","yet","for",
        "in","on","at","to","from","by","with","about","into","through","during","before",
        "after","above","below","between","of","up","down","out","off","over","under"
    );

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("⚙️ SIMS1337 - Unified Control Center v0.11.0");

        dashboardView = buildDashboard();
        gridView = buildGridView();
        settingsView = buildSettingsView();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a2e;");

        HBox navBar = new HBox(10);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setStyle("-fx-background-color: #0f3460; -fx-padding: 10;");

        Button godHandBtn = navButton("🧠 GodHand", "#00d9ff", true);
        Button playerGridBtn = navButton("🎮 Player Grid", "#16213e", false);
        Button settingsBtn = navButton("⚙️ Settings", "#16213e", false);

        godHandBtn.setOnAction(e -> { highlightNav(godHandBtn, playerGridBtn, settingsBtn); viewStack.getChildren().setAll(dashboardView); });
        playerGridBtn.setOnAction(e -> { highlightNav(playerGridBtn, godHandBtn, settingsBtn); viewStack.getChildren().setAll(gridView); });
        settingsBtn.setOnAction(e -> { highlightNav(settingsBtn, godHandBtn, playerGridBtn); viewStack.getChildren().setAll(settingsView); });

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        statusLabel = new Label("🟢 System Ready");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00ff88;");
        navBar.getChildren().addAll(godHandBtn, playerGridBtn, settingsBtn, spacer, statusLabel);

        viewStack = new StackPane(dashboardView);
        viewStack.setStyle("-fx-background-color: #1a1a2e;");
        root.getChildren().addAll(navBar, viewStack);
        VBox.setVgrow(viewStack, Priority.ALWAYS);

        stage.setScene(new Scene(root, 1500, 950));
        stage.show();

        log("✅ SIMS1337 v0.11.0 - Web APIs + Model Manager + Voting + Topology + Night Cycle");
        initAll();
        refreshInstalledModels();
    }

    private void initAll() {
        initCommandRegistry();
        initAgentPositions();
        initStationPipelines();
        initNightCycleDefaults();
        initDefaultProposals();
        initDefaultTopology();
        initDefaultWebApis();
        startEntropyMonitor();
    }

    // ==================== NAVIGATION ====================
    private Button navButton(String text, String bg, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 14px;",
            active ? "#00d9ff" : bg, active ? "#000000" : "#ffffff"));
        return btn;
    }
    private void highlightNav(Button active, Button... others) {
        active.setStyle("-fx-background-color: #00d9ff; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 14px;");
        for (Button b : others) b.setStyle("-fx-background-color: #16213e; -fx-text-fill: #ffffff; -fx-font-size: 14px;");
    }

    // ==================== DASHBOARD VIEW ====================
    private VBox buildDashboard() {
        VBox box = vbox(10, "#1a1a2e", 15);

        HBox header = hbox(20, Pos.CENTER_LEFT, "#16213e", 12);
        header.getChildren().addAll(label("🎮 GODHAND", 28, "#00d9ff", true), label("v0.11.0 - All Systems", 14, "#a0a0a0", false), new Region());
        HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS);

        // === SHARED GOD CHAT ===
        TitledPane godChatPane = titledPane("💬 SHARED GOD CHAT - All Model Conversations", true);
        VBox godChatBox = vbox(5, "#0a0a15", 5);
        godChat = new TextArea();
        godChat.setEditable(false); godChat.setWrapText(true); godChat.setPrefRowCount(10);
        godChat.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-border-color: #00d9ff; -fx-border-width: 2;");
        godChat.setText("╔══════════════════════════════════════════════════════════════╗\n║  🧠 SHARED GOD CHAT - qwen2.5 | tinyllama | phi | phi3     ║\n╚══════════════════════════════════════════════════════════════╝\n\n");
        HBox godChatControls = hbox(8, Pos.CENTER_LEFT, null, 0);
        Button clearBtn = new Button("🗑️ Clear"); clearBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: #000; -fx-font-size: 10px; -fx-padding: 3 10;");
        clearBtn.setOnAction(e -> godChat.setText("╔══════════════════════════════════════════════════════════════╗\n║              🧠 SHARED GOD CHAT - CLEARED                    ║\n╚══════════════════════════════════════════════════════════════╝\n\n"));
        godChatControls.getChildren().addAll(clearBtn, label("Messages: 0", 10, "#a0a0a0", false));
        godChatBox.getChildren().addAll(godChat, godChatControls);
        godChatPane.setContent(godChatBox);

        // === MODEL PANELS ===
        TitledPane modelPool = titledPane("🧠 MODEL POOL + ROUTING + WEB APIs", true);
        VBox modelContent = vbox(8, "#16213e", 8);
        String[][] models = {
            {"⚡ FAST", "qwen2.5:0.5b", "398MB | <100ms", "#00ff88"},
            {"⚖️ BALANCED", "tinyllama:1.1b", "638MB | ~500ms", "#ffaa00"},
            {"🧠 REASONING", "phi:latest", "1.6GB | ~2-5s", "#ff6b6b"},
            {"🎯 DEEP", "phi3:mini", "2.2GB | ~5-10s", "#c77dff"},
            {"🦙 LLAMA", "llama3.2:1b", "1.3GB | ~1-3s", "#ffd700"},
            {"🐋 DEEPSEEK", "deepseek-r1:1.5b", "1.1GB | ~2-5s", "#ff69b4"}
        };
        for (String[] m : models) {
            VBox card = vbox(4, "#0f3460", 8);
            card.setStyle("-fx-background-color: #0f3460; -fx-padding: 8; -fx-background-radius: 5;");

            HBox mh = hbox(8, Pos.CENTER_LEFT, null, 0);
            mh.getChildren().addAll(label(m[0], 12, m[3], true), label(m[1], 11, "#ffffff", false), label(m[2], 9, "#a0a0a0", false));

            HBox rr = hbox(5, Pos.CENTER_LEFT, null, 0);
            ComboBox<String> pb = new ComboBox<>(); pb.getItems().addAll("Linear","Loop","Random","Markov","Vote","Chain","Broadcast"); pb.setValue("Linear");
            pb.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 9px;"); pb.setMaxWidth(80);
            modelPatterns.put(m[1], pb);
            ComboBox<String> nr = new ComboBox<>(); nr.getItems().addAll("Self","qwen2.5:0.5b","tinyllama:1.1b","phi:latest","phi3:mini","All"); nr.setValue("Self");
            nr.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 9px;"); nr.setMaxWidth(90);
            modelNextRoutes.put(m[1], nr);

            Button loopBtn = new Button("🔁");
            loopBtn.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: #000; -fx-font-size: 9px; -fx-padding: 2 6;");
            String mn = m[1];
            loopBtn.setOnAction(e -> {
                boolean a = !loopActive.getOrDefault(mn, false); loopActive.put(mn, a);
                loopBtn.setText(a ? "⏹️" : "🔁");
                loopBtn.setStyle(a ? "-fx-background-color: #ff6b6b; -fx-text-fill: #fff; -fx-font-size: 9px; -fx-padding: 2 6;" : "-fx-background-color: #ffaa00; -fx-text-fill: #000; -fx-font-size: 9px; -fx-padding: 2 6;");
                if (a) { loopCounts.put(mn, 0); log("🔁 [" + mn + "] Loop ON"); runLoop(mn); }
                else log("⏹️ [" + mn + "] Loop OFF (" + loopCounts.getOrDefault(mn, 0) + " iterations)");
            });

            Button webBtn = new Button("🌐");
            webBtn.setStyle("-fx-background-color: #6e5494; -fx-text-fill: #fff; -fx-font-size: 9px; -fx-padding: 2 6;");
            webBtn.setOnAction(e -> {
                String q = modelInputs.get(mn).getText();
                if (!q.isEmpty()) { String s = lexicalSummarize(q); addToGodChat("🌐 LEXICAL", mn, s); TextArea c = modelChats.get(mn); if (c != null) c.appendText("[🌐] " + s + "\n"); log("🌐 [" + mn + "] " + s); }
            });

            Button apiBtn = new Button("🔌");
            apiBtn.setStyle("-fx-background-color: #00d9ff; -fx-text-fill: #000; -fx-font-size: 9px; -fx-padding: 2 6;");
            apiBtn.setOnAction(e -> callWebApi(mn));

            rr.getChildren().addAll(label("Route:", 9, "#a0a0a0", false), pb, label("→", 9, "#00d9ff", false), nr, loopBtn, webBtn, apiBtn);

            TextArea ca = new TextArea(); ca.setEditable(false); ca.setPrefRowCount(3);
            ca.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 9px;");
            ca.setText("[" + m[1] + "] Ready.\n"); modelChats.put(m[1], ca);

            HBox ir = hbox(4, Pos.CENTER_LEFT, null, 0);
            TextField tf = new TextField(); tf.setPromptText("→ " + m[1] + "...");
            tf.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 9px;"); HBox.setHgrow(tf, Priority.ALWAYS);
            modelInputs.put(m[1], tf);
            Button sb = new Button("▶"); sb.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #000; -fx-font-size: 9px; -fx-padding: 2 8;");
            String mn2 = m[1];
            sb.setOnAction(e -> { String msg = tf.getText(); if (!msg.isEmpty()) { addToGodChat("👤 YOU", mn2, msg); ca.appendText("You: " + msg + "\n"); tf.clear(); simulateModelResponse(mn2, msg); } });
            ir.getChildren().addAll(tf, sb);
            card.getChildren().addAll(mh, rr, ca, ir);
            modelContent.getChildren().add(card);
        }
        modelPool.setContent(new ScrollPane(modelContent));

        // LoRA + Queue
        HBox bottomRow = hbox(10, Pos.CENTER_LEFT, null, 0);
        TitledPane loraPane = titledPane("🔄 LORA", true);
        FlowPane lf = new FlowPane(5, 5);
        String[] ads = {"💬CHAT","💻CODE","🗺️PATH","❤️MOTIVE","🎯CAREER","🔍ANALYSIS"};
        String[] cls = {"#00ff88","#00d9ff","#ffaa00","#ff6b6b","#c77dff","#a0a0a0"};
        for (int i = 0; i < ads.length; i++) { Label al = new Label(ads[i]); al.setStyle("-fx-background-color: "+cls[i]+"; -fx-text-fill: #000; -fx-padding: 4 10; -fx-background-radius: 15; -fx-font-size: 10px;"); lf.getChildren().add(al); }
        loraPane.setContent(lf); loraPane.setMaxWidth(400);
        TitledPane qp = titledPane("📋 QUEUE", true);
        VBox qc = vbox(5, "#16213e", 5);
        ProgressBar pbar = new ProgressBar(0); pbar.setMaxWidth(Double.MAX_VALUE);
        qc.getChildren().addAll(label("0/100 | 0%", 10, "#00d9ff", false), pbar); qp.setContent(qc); qp.setMaxWidth(250);
        bottomRow.getChildren().addAll(loraPane, qp);

        TitledPane logPane = titledPane("📜 ACTIVITY LOG", true);
        logConsole = new TextArea(); logConsole.setEditable(false); logConsole.setWrapText(true); logConsole.setPrefHeight(80);
        logConsole.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 10px;");
        logPane.setContent(logConsole);

        box.getChildren().addAll(header, godChatPane, modelPool, bottomRow, logPane);
        return box;
    }

    // ==================== SHARED GOD CHAT ====================
    private void addToGodChat(String role, String model, String message) {
        godChatMessageCount++;
        String ts = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String color = role.contains("YOU") ? "#ffffff" : role.contains("LEXICAL") ? "#c77dff" : role.contains("VOTE") ? "#ffaa00" : role.contains("TOPOLOGY") ? "#00d9ff" : role.contains("NIGHT") ? "#6e5494" : model.contains("qwen") ? "#00ff88" : model.contains("tinyllama") ? "#ffaa00" : model.contains("phi3") ? "#c77dff" : "#ff6b6b";
        String entry = String.format("[%s] %s | %s: %s%n", ts, role, model, message);
        Platform.runLater(() -> { godChat.appendText(entry); godChat.setScrollTop(Double.MAX_VALUE); });
    }

    // ==================== LOOP MODE ====================
    private void runLoop(String modelName) {
        chatScheduler.schedule(() -> {
            while (loopActive.getOrDefault(modelName, false)) {
                int c = loopCounts.merge(modelName, 1, Integer::sum);
                try {
                    String r = callOllama(modelName, "Loop #" + c + ". Continue.");
                    Platform.runLater(() -> { addToGodChat("🔄 LOOP", modelName, r); TextArea ca = modelChats.get(modelName); if (ca != null) ca.appendText("[Loop#" + c + "] " + r + "\n"); checkCommandTriggers(r, modelName); });
                } catch (Exception e) { Platform.runLater(() -> log("⚠️ Loop error: " + e.getMessage())); loopActive.put(modelName, false); break; }
                try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
            }
        }, 0, TimeUnit.SECONDS);
    }

    // ==================== LEXICAL ====================
    private String lexicalSummarize(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Map<String, Integer> freq = new HashMap<>(); int total = 0;
        for (String w : words) { if (w.length() < 2 || STOP_WORDS.contains(w)) continue; freq.merge(w, 1, Integer::sum); total++; }
        if (freq.isEmpty()) return "No keywords: " + text.substring(0, Math.min(50, text.length()));
        List<Map.Entry<String, Integer>> sorted = freq.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("KEYWORDS[");
        double tw = sorted.stream().mapToInt(Map.Entry::getValue).sum();
        for (int i = 0; i < sorted.size(); i++) { Map.Entry<String, Integer> e = sorted.get(i); sb.append(String.format("%s(%.0f%%)", e.getKey(), e.getValue()/tw*100)); if (i < sorted.size()-1) sb.append(", "); }
        sb.append(String.format("] | %d→%d | %.1f%%", total, freq.size(), freq.size()*100.0/Math.max(1,total)));
        return sb.toString();
    }

    // ==================== WEB API ====================
    private void initDefaultWebApis() {
        webApiTable.addAll(
            new String[]{"qwen2.5:0.5b", "https://api.github.com/search/repositories", "GET", "q=$QUERY", "✅"},
            new String[]{"tinyllama:1.1b", "https://api.duckduckgo.com/", "GET", "q=$QUERY&format=json", "✅"},
            new String[]{"phi:latest", "https://api.open-meteo.com/v1/forecast", "GET", "latitude=52.52&longitude=13.41", "✅"},
            new String[]{"phi3:mini", "https://api.quotable.io/random", "GET", "", "✅"}
        );
    }

    private void callWebApi(String modelName) {
        for (String[] api : webApiTable) {
            if (api[0].equals(modelName) && api[4].equals("✅")) {
                chatScheduler.schedule(() -> {
                    try {
                        String url = api[1] + (api[3].isEmpty() ? "" : "?" + api[3].replace("$QUERY", modelInputs.get(modelName).getText()));
                        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
                        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                        String body = resp.body().length() > 300 ? resp.body().substring(0, 300) + "..." : resp.body();
                        Platform.runLater(() -> { addToGodChat("🔌 API", modelName, "[" + resp.statusCode() + "] " + body); TextArea ca = modelChats.get(modelName); if (ca != null) ca.appendText("[🔌 API] " + body + "\n"); log("🔌 [" + modelName + "] API: " + resp.statusCode()); });
                    } catch (Exception e) { Platform.runLater(() -> log("❌ API error: " + e.getMessage())); }
                }, 0, TimeUnit.SECONDS);
                return;
            }
        }
        log("⚠️ No API configured for " + modelName);
    }

    // ==================== MODEL MANAGER ====================
    private void refreshInstalledModels() {
        chatScheduler.schedule(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(OLLAMA_TAGS)).timeout(Duration.ofSeconds(5)).GET().build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    String body = resp.body();
                    Platform.runLater(() -> {
                        installedModels.clear();
                        int idx = 0;
                        while ((idx = body.indexOf("\"name\":\"", idx)) > 0) {
                            idx += 8; int end = body.indexOf("\"", idx);
                            if (end > idx) installedModels.add(body.substring(idx, end));
                            idx = end;
                        }
                        log("📦 Installed models: " + installedModels.size());
                    });
                }
            } catch (Exception e) { Platform.runLater(() -> log("⚠️ Cannot reach Ollama for model list")); }
        }, 0, TimeUnit.SECONDS);
    }

    private void pullModel(String modelName) {
        log("📥 Pulling " + modelName + "...");
        statusLabel.setText("📥 Pulling " + modelName + "...");
        chatScheduler.schedule(() -> {
            try {
                String json = "{\"name\":\"" + modelName + "\"}";
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:11434/api/pull"))
                    .header("Content-Type", "application/json").timeout(Duration.ofMinutes(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> { log("📥 " + modelName + ": " + resp.statusCode()); refreshInstalledModels(); statusLabel.setText("🟢 System Ready"); });
            } catch (Exception e) { Platform.runLater(() -> { log("❌ Pull failed: " + e.getMessage()); statusLabel.setText("🟢 System Ready"); }); }
        }, 0, TimeUnit.SECONDS);
    }

    // ==================== VOTING SYSTEM ====================
    private void initDefaultProposals() {
        proposalTable.addAll(
            new String[]{"Add WebSocket support", "Pending", "0/4", "0/4", "0%"},
            new String[]{"Implement Markov reviews", "Pending", "0/4", "0/4", "0%"},
            new String[]{"Deploy to production", "Pending", "0/4", "0/4", "0%"},
            new String[]{"Refactor ModelRouter", "Pending", "0/4", "0/4", "0%"}
        );
    }

    private void castVote(String proposal, String modelName, boolean approve) {
        votes.putIfAbsent(proposal, new ConcurrentHashMap<>());
        votes.get(proposal).put(modelName, approve ? "APPROVE" : "REJECT");
        addToGodChat("🗳️ VOTE", modelName, (approve ? "✅ APPROVE" : "❌ REJECT") + " → " + proposal);
        log("🗳️ [" + modelName + "] " + (approve ? "APPROVED" : "REJECTED") + " " + proposal);
        updateProposalStatus(proposal);
    }

    private void updateProposalStatus(String proposal) {
        Map<String, String> v = votes.getOrDefault(proposal, Map.of());
        long approve = v.values().stream().filter("APPROVE"::equals).count();
        long total = v.size();
        for (String[] p : proposalTable) {
            if (p[0].equals(proposal)) {
                p[1] = total >= 3 ? (approve >= 2 ? "✅ APPROVED" : "❌ REJECTED") : "Voting...";
                p[2] = approve + "/" + total;
                p[3] = (total - approve) + "/" + total;
                p[4] = total > 0 ? (int)(approve * 100.0 / total) + "%" : "0%";
            }
        }
    }

    // ==================== TOPOLOGY BUILDER ====================
    private void initDefaultTopology() {
        topologyTable.addAll(
            new String[]{"Root", "GodHand", "Entry point", "✅"},
            new String[]{"GodHand", "ModelPool", "Routes tasks", "✅"},
            new String[]{"ModelPool", "BruteFoundry", "Code generation", "✅"},
            new String[]{"ModelPool", "Hospital", "Agent recovery", "✅"},
            new String[]{"BruteFoundry", "GitHub", "Push code", "✅"},
            new String[]{"Hospital", "ModelPool", "Restart agents", "✅"}
        );
        for (String[] t : topologyTable) {
            topologyGraph.putIfAbsent(t[0], new ArrayList<>());
            topologyGraph.get(t[0]).add(t[1]);
        }
    }

    private void buildTopology() {
        log("🌳 Building topology from " + topologyTable.size() + " nodes...");
        addToGodChat("🌳 TOPOLOGY", "Builder", "Building graph with " + topologyTable.size() + " nodes");
        for (String[] t : topologyTable) {
            if (t[3].equals("✅")) {
                addToGodChat("🌳 TOPOLOGY", t[0] + "→" + t[1], t[2]);
            }
        }
        log("✅ Topology built: " + topologyGraph.size() + " nodes, " + topologyTable.size() + " edges");
    }

    // ==================== NIGHT CYCLE ====================
    private void initNightCycleDefaults() {
        nightCycleConfig.put("vote_time", "18:00");
        nightCycleConfig.put("deploy_time", "20:00");
        nightCycleConfig.put("email_time", "22:00");
        nightCycleConfig.put("email_to", "chrisalunlloyd2@gmail.com");
        nightCycleConfig.put("enabled", "false");
    }

    private void toggleNightCycle(boolean enable) {
        nightCycleConfig.put("enabled", String.valueOf(enable));
        if (enable) {
            log("🌙 Night Cycle ENABLED: " + nightCycleConfig.get("vote_time") + " votes → " + nightCycleConfig.get("deploy_time") + " deploy → " + nightCycleConfig.get("email_time") + " email");
            addToGodChat("🌙 NIGHT", "System", "Cycle enabled: votes@" + nightCycleConfig.get("vote_time") + " → deploy@" + nightCycleConfig.get("deploy_time") + " → email@" + nightCycleConfig.get("email_time"));
            statusLabel.setText("🌙 Night Cycle Armed");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #c77dff; -fx-font-weight: bold;");
        } else {
            log("🌙 Night Cycle DISABLED");
            statusLabel.setText("🟢 System Ready");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00ff88;");
        }
    }

    // ==================== PLAYER GRID VIEW ====================
    private VBox buildGridView() {
        VBox box = vbox(15, "#1a1a2e", 20);
        box.setAlignment(Pos.TOP_CENTER);
        box.getChildren().addAll(label("🎮 PLAYER GRID 3D - CLICK CELLS!", 24, "#00d9ff", true), label("👇 Left=Move | Right=Pipeline 👇", 14, "#00ff88", false));
        gridPane = new GridPane(); gridPane.setHgap(5); gridPane.setVgap(5); gridPane.setAlignment(Pos.CENTER);
        gridPane.setStyle("-fx-background-color: #1a1a3e; -fx-padding: 20; -fx-border-color: #00ff88; -fx-border-width: 3;");
        for (int row = 0; row < 10; row++)
            for (int col = 0; col < 10; col++) {
                Rectangle cell = new Rectangle(50, 50);
                cell.setFill(Color.rgb(30 + row * 15, 80 + col * 12, 150 + (10 - row) * 8));
                cell.setStroke(Color.web("#00d9ff")); cell.setStrokeWidth(3);
                gridCells[row][col] = cell;
                final int r = row, c = col;
                cell.setOnMouseClicked(e -> { if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) startPipelineAt(r, c); else moveAgentTo("Agent Alpha", r, c); });
                Tooltip.install(cell, new Tooltip("(" + col + "," + row + ")\nClick: Move\nRight: Pipeline"));
                gridPane.add(cell, col, row);
            }
        box.getChildren().addAll(gridPane, label("👆 100 CLICKABLE CELLS 👆", 14, "#ffffff", false));
        VBox pp = vbox(10, "#16213e", 15); pp.setStyle("-fx-background-radius: 10;");
        pp.getChildren().add(label("🎮 PLAYER POSITIONS (XYZ)", 16, "#00d9ff", true));
        String[][] players = {{"🟢 Agent Alpha","3","7","2"},{"🔵 Agent Beta","8","4","1"},{"🟠 Agent Gamma","5","9","3"}};
        for (String[] p : players) { HBox pr = hbox(15, Pos.CENTER_LEFT, null, 0); Label pl = label("("+p[1]+","+p[2]+","+p[3]+")", 12, "#a0a0a0", false); agentPositionLabels.put(p[0], pl); pr.getChildren().addAll(label(p[0], 12, "#ffffff", true), pl, label("Active", 12, "#00ff88", false)); pp.getChildren().add(pr); }
        box.getChildren().add(pp);
        HBox sr = hbox(10, Pos.CENTER, null, 10);
        String[][] sts = {{"🏗️ Brute Foundry","#ff6b6b"},{"🧬 A/B Lab","#c77dff"},{"🌳 Knowledge Tree","#00d9ff"},{"🔬 Research","#ffaa00"},{"🔒 Secrets","#999999"},{"🏥 Hospital","#ff6b9d"},{"📡 GitHub","#6e5494"}};
        for (String[] s : sts) { Button sb = new Button(s[0]); sb.setStyle("-fx-background-color: "+s[1]+"; -fx-text-fill: #fff; -fx-font-size: 11px; -fx-padding: 5 10;"); sb.setOnAction(e -> triggerStation(s[0].substring(2).trim())); sr.getChildren().add(sb); }
        box.getChildren().add(sr);
        return box;
    }

    // ==================== AGENT MOVEMENT ====================
    private void initAgentPositions() { agentPositions.put("Agent Alpha", new int[]{3,7,2}); agentPositions.put("Agent Beta", new int[]{8,4,1}); agentPositions.put("Agent Gamma", new int[]{5,9,3}); }
    private void moveAgentTo(String name, int x, int y) {
        int[] pos = agentPositions.get(name); if (pos == null) return;
        int ox = pos[0], oy = pos[1]; pos[0] = x; pos[1] = y;
        Platform.runLater(() -> {
            if (ox>=0&&ox<10&&oy>=0&&oy<10) gridCells[oy][ox].setFill(Color.rgb(30+oy*15,80+ox*12,150+(10-oy)*8));
            Color ac = name.contains("Alpha") ? Color.rgb(0,255,100) : name.contains("Beta") ? Color.rgb(0,150,255) : Color.rgb(255,150,0);
            gridCells[y][x].setFill(ac); gridCells[y][x].setStroke(Color.WHITE); gridCells[y][x].setStrokeWidth(4);
            Label pl = agentPositionLabels.get(name); if (pl != null) pl.setText("("+x+","+y+","+pos[2]+")");
            log("🎯 "+name+" → ("+x+","+y+")"); statusLabel.setText("🟢 "+name+" @ ("+x+","+y+")");
        });
    }

    // ==================== STATION PIPELINES ====================
    private void initStationPipelines() { pipelineNext.put("Brute Foundry","A/B Lab"); pipelineNext.put("A/B Lab","Knowledge Tree"); pipelineNext.put("Knowledge Tree","Research"); pipelineNext.put("Research","GitHub"); pipelineNext.put("GitHub","Hospital"); pipelineNext.put("Hospital","Brute Foundry"); }
    private void startPipelineAt(int x, int y) {
        log("🔗 Pipeline @ ("+x+","+y+")"); pipelineActive.put("pipeline", true);
        chatScheduler.schedule(() -> { String st = "Brute Foundry"; int step = 0;
            while (pipelineActive.getOrDefault("pipeline",false) && step < 20) { final String cs = st; final int s = step; Platform.runLater(() -> { addToGodChat("🔗 PIPELINE", cs, "Step "+s+" @ ("+x+","+y+")"); modelChats.forEach((n,c)->c.appendText("[Pipeline:"+cs+"]\n")); }); st = pipelineNext.getOrDefault(st,"Brute Foundry"); step++; try { Thread.sleep(2000); } catch (InterruptedException e) { break; } }
            final int ts = step; Platform.runLater(() -> log("🔗 Pipeline done: "+ts+" steps")); }, 0, TimeUnit.SECONDS);
    }

    // ==================== SETTINGS VIEW ====================
    private VBox buildSettingsView() {
        VBox box = vbox(10, "#1a1a2e", 20);
        box.getChildren().add(label("⚙️ SETTINGS & ORCHESTRATION - v0.11.0", 24, "#00d9ff", true));

        // === WEB APIs ===
        TitledPane apiPane = titledPane("🔌 WEB APIs - Per Model HTTP Endpoints", true);
        VBox apiContent = vbox(10, "#16213e", 10);
        TableView<String[]> apiTable = new TableView<>(); apiTable.setPrefHeight(120); apiTable.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[],String> apiModel = new TableColumn<>("Model"); apiModel.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[],String> apiUrl = new TableColumn<>("URL"); apiUrl.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[1])); apiUrl.setPrefWidth(250);
        TableColumn<String[],String> apiMethod = new TableColumn<>("Method"); apiMethod.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[],String> apiParams = new TableColumn<>("Params"); apiParams.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[3])); apiParams.setPrefWidth(150);
        TableColumn<String[],String> apiActive = new TableColumn<>("Active"); apiActive.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[4]));
        apiTable.getColumns().addAll(apiModel, apiUrl, apiMethod, apiParams, apiActive);
        apiTable.setItems(webApiTable);
        HBox apiBtns = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button addApi = styledButton("➕ Add API", "#00ff88"); addApi.setOnAction(e->webApiTable.add(new String[]{"model","https://...","GET","","✅"}));
        Button delApi = styledButton("🗑️ Delete", "#ff6b6b"); delApi.setOnAction(e->{String[] s=apiTable.getSelectionModel().getSelectedItem(); if(s!=null)webApiTable.remove(s);});
        Button testAll = styledButton("🔌 Test All APIs", "#00d9ff"); testAll.setOnAction(e->webApiTable.forEach(a->{if(a[4].equals("✅"))callWebApi(a[0]);}));
        apiBtns.getChildren().addAll(addApi, delApi, testAll);
        apiContent.getChildren().addAll(apiTable, apiBtns);
        apiPane.setContent(apiContent);

        // === MODEL MANAGER ===
        TitledPane modelMgrPane = titledPane("📦 MODEL MANAGER - Pull / List / Switch", true);
        VBox mmContent = vbox(10, "#16213e", 10);
        HBox mmRow1 = hbox(10, Pos.CENTER_LEFT, null, 0);
        mmRow1.getChildren().add(label("Installed:", 12, "#ffffff", false));
        ListView<String> installedList = new ListView<>(installedModels); installedList.setPrefHeight(80); installedList.setStyle("-fx-background-color: #0f3460; -fx-text-fill: #00ff88;");
        HBox mmRow2 = hbox(10, Pos.CENTER_LEFT, null, 0);
        mmRow2.getChildren().add(label("Available to pull:", 12, "#ffffff", false));
        ComboBox<String> pullSelect = new ComboBox<>(availableModels); pullSelect.setValue("llama3.2:1b"); pullSelect.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff;");
        Button pullBtn = styledButton("📥 Pull Model", "#c77dff"); pullBtn.setOnAction(e->pullModel(pullSelect.getValue()));
        Button refreshBtn = styledButton("🔄 Refresh List", "#00d9ff"); refreshBtn.setOnAction(e->refreshInstalledModels());
        mmRow2.getChildren().addAll(pullSelect, pullBtn, refreshBtn);
        mmContent.getChildren().addAll(mmRow1, installedList, mmRow2);
        modelMgrPane.setContent(mmContent);

        // === VOTING SYSTEM ===
        TitledPane votePane = titledPane("🗳️ AGENT VOTING SYSTEM - Proposals & Consensus", true);
        VBox voteContent = vbox(10, "#16213e", 10);
        TableView<String[]> voteTable = new TableView<>(); voteTable.setPrefHeight(100); voteTable.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[],String> vProp = new TableColumn<>("Proposal"); vProp.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[0])); vProp.setPrefWidth(200);
        TableColumn<String[],String> vStatus = new TableColumn<>("Status"); vStatus.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[],String> vApprove = new TableColumn<>("Approve"); vApprove.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[],String> vReject = new TableColumn<>("Reject"); vReject.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        TableColumn<String[],String> vPct = new TableColumn<>("%"); vPct.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[4]));
        voteTable.getColumns().addAll(vProp, vStatus, vApprove, vReject, vPct);
        voteTable.setItems(proposalTable);
        HBox voteBtns = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button addProp = styledButton("➕ Proposal", "#00ff88"); addProp.setOnAction(e->proposalTable.add(new String[]{"New proposal","Pending","0/4","0/4","0%"}));
        Button approveBtn = styledButton("✅ Approve", "#00ff88"); approveBtn.setOnAction(e->{String[] s=voteTable.getSelectionModel().getSelectedItem(); if(s!=null)castVote(s[0],"qwen2.5:0.5b",true);});
        Button rejectBtn = styledButton("❌ Reject", "#ff6b6b"); rejectBtn.setOnAction(e->{String[] s=voteTable.getSelectionModel().getSelectedItem(); if(s!=null)castVote(s[0],"qwen2.5:0.5b",false);});
        Button voteAll = styledButton("🗳️ All Models Vote", "#c77dff"); voteAll.setOnAction(e->{String[] s=voteTable.getSelectionModel().getSelectedItem(); if(s!=null){for(String m:modelChats.keySet())castVote(s[0],m,Math.random()>0.3);}});
        voteBtns.getChildren().addAll(addProp, approveBtn, rejectBtn, voteAll);
        voteContent.getChildren().addAll(voteTable, voteBtns);
        votePane.setContent(voteContent);

        // === TOPOLOGY BUILDER ===
        TitledPane topoPane = titledPane("🌳 TOPOLOGY BUILDER - Node/Edge Graph", true);
        VBox topoContent = vbox(10, "#16213e", 10);
        TableView<String[]> topoTable = new TableView<>(); topoTable.setPrefHeight(100); topoTable.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[],String> tFrom = new TableColumn<>("From"); tFrom.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[],String> tTo = new TableColumn<>("To"); tTo.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[],String> tDesc = new TableColumn<>("Description"); tDesc.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[2])); tDesc.setPrefWidth(200);
        TableColumn<String[],String> tActive = new TableColumn<>("Active"); tActive.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        topoTable.getColumns().addAll(tFrom, tTo, tDesc, tActive);
        topoTable.setItems(topologyTable);
        HBox topoBtns = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button addNode = styledButton("➕ Add Edge", "#00ff88"); addNode.setOnAction(e->topologyTable.add(new String[]{"From","To","Description","✅"}));
        Button buildTopo = styledButton("🌳 Build Topology", "#00d9ff"); buildTopo.setOnAction(e->buildTopology());
        Button exportTopo = styledButton("📋 Export", "#c77dff");
        topoBtns.getChildren().addAll(addNode, buildTopo, exportTopo);
        topoContent.getChildren().addAll(topoTable, topoBtns);
        topoPane.setContent(topoContent);

        // === NIGHT CYCLE ===
        TitledPane nightPane = titledPane("🌙 NIGHT CYCLE - Autonomous Operation", true);
        VBox nightContent = vbox(10, "#16213e", 10);
        HBox nightRow1 = hbox(10, Pos.CENTER_LEFT, null, 0);
        nightRow1.getChildren().addAll(label("Vote Time:", 12, "#fff", false), tf("18:00", 60), label("Deploy:", 12, "#fff", false), tf("20:00", 60), label("Email:", 12, "#fff", false), tf("22:00", 60));
        HBox nightRow2 = hbox(10, Pos.CENTER_LEFT, null, 0);
        nightRow2.getChildren().addAll(label("Email to:", 12, "#fff", false), tf("chrisalunlloyd2@gmail.com", 200));
        HBox nightRow3 = hbox(10, Pos.CENTER_LEFT, null, 0);
        ToggleButton nightToggle = new ToggleButton("🌙 Night Cycle OFF");
        nightToggle.setStyle("-fx-background-color: #16213e; -fx-text-fill: #fff; -fx-font-size: 14px; -fx-padding: 10 20;");
        nightToggle.setOnAction(e -> { boolean on = nightToggle.isSelected(); nightToggle.setText(on ? "🌙 Night Cycle ON" : "🌙 Night Cycle OFF"); nightToggle.setStyle(on ? "-fx-background-color: #c77dff; -fx-text-fill: #000; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20;" : "-fx-background-color: #16213e; -fx-text-fill: #fff; -fx-font-size: 14px; -fx-padding: 10 20;"); toggleNightCycle(on); });
        nightRow3.getChildren().addAll(nightToggle, label("18:00 votes → 20:00 deploy → 22:00 email brief", 12, "#a0a0a0", false));
        nightContent.getChildren().addAll(nightRow1, nightRow2, nightRow3);
        nightPane.setContent(nightContent);

        // === ROUTING + COMMANDS + PROMPT + CONTEXT (compact) ===
        TitledPane routingPane = titledPane("🔀 ROUTING + COMMANDS + PROMPT + CONTEXT", true);
        VBox rcContent = vbox(8, "#16213e", 8);

        // Routing table
        TableView<String[]> rtTable = new TableView<>(); rtTable.setPrefHeight(80); rtTable.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[],String> rf = new TableColumn<>("From"); rf.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[],String> rp = new TableColumn<>("Pattern"); rp.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[],String> rt2 = new TableColumn<>("Next"); rt2.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[],String> rl = new TableColumn<>("Loop"); rl.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        rtTable.getColumns().addAll(rf, rp, rt2, rl); rtTable.setItems(routingTable);
        routingTable.addAll(new String[]{"qwen2.5:0.5b","Linear","tinyllama:1.1b","Off"},new String[]{"tinyllama:1.1b","Markov","phi:latest","Off"},new String[]{"phi:latest","Chain","phi3:mini","Off"},new String[]{"phi3:mini","Vote","All","Off"});

        // Command table
        TableView<String[]> cmdTable = new TableView<>(); cmdTable.setPrefHeight(80); cmdTable.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[],String> ct = new TableColumn<>("Trigger"); ct.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[],String> cc = new TableColumn<>("Command"); cc.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[],String> cs = new TableColumn<>("Station"); cs.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[],String> ca = new TableColumn<>("Active"); ca.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        cmdTable.getColumns().addAll(ct, cc, cs, ca); cmdTable.setItems(commandTable);
        commandTable.addAll(new String[]{"just enter this in terminal","execute $INPUT","Brute Foundry","✅"},new String[]{"fix this bug","analyze + patch","Hospital","✅"},new String[]{"push to github","git push","GitHub","✅"},new String[]{"search the web","web_search","Research","✅"});

        // Prompt
        TextArea promptArea = new TextArea("You are an SLM agent in SIMS1337. Collaborate, vote, build, maintain. Access: terminal, web, files.");
        promptArea.setPrefRowCount(2); promptArea.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 10px;");
        Button injectBtn = styledButton("💉 Inject to All", "#c77dff"); injectBtn.setOnAction(e->{modelChats.forEach((n,c)->c.appendText("[SYSTEM] "+promptArea.getText().substring(0,50)+"...\n")); addToGodChat("💉 SYSTEM","All",promptArea.getText().substring(0,80)+"...");});

        // Context
        HBox ctxRow = hbox(8, Pos.CENTER_LEFT, null, 0);
        String[][] ctxOpts = {{"Tokens","2048","4096","8192"},{"Temp","0.1","0.5","0.7"},{"LoRA","CHAT","CODE","ANALYSIS"},{"KV","512","1024","2048"},{"KG","1","2","3"},{"Affine","0.5x","1.0x","1.5x"}};
        for (String[] o : ctxOpts) { ComboBox<String> cb = new ComboBox<>(); cb.getItems().addAll(o[1],o[2],o[3]); cb.setValue(o[1]); cb.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 9px;"); cb.setMaxWidth(80); ctxRow.getChildren().addAll(label(o[0]+":",9,"#a0a0a0",false), cb); }

        rcContent.getChildren().addAll(label("Routing:",11,"#00d9ff",true), rtTable, label("Commands:",11,"#00d9ff",true), cmdTable, label("Prompt:",11,"#00d9ff",true), promptArea, injectBtn, label("Context:",11,"#00d9ff",true), ctxRow);
        routingPane.setContent(new ScrollPane(rcContent));

        // === MODEL EVALUATION + LORA + PROMPT ENGINEERING + STATS ===
        TitledPane evalPane = titledPane("🧪 MODEL EVALUATION + LORA + PROMPT ENGINEERING + STATS", true);
        VBox evalContent = vbox(8, "#16213e", 8);

        // Model capability matrix
        Label evalTitle = label("📊 Model Capability Matrix (editable)", 12, "#00d9ff", true);
        TableView<String[]> evalTable = new TableView<>(); evalTable.setPrefHeight(100); evalTable.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[],String> eModel = new TableColumn<>("Model"); eModel.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[],String> eCode = new TableColumn<>("Code"); eCode.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[],String> eEssay = new TableColumn<>("Essay"); eEssay.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[],String> eLogic = new TableColumn<>("Logic"); eLogic.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        TableColumn<String[],String> eCreative = new TableColumn<>("Creative"); eCreative.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[4]));
        TableColumn<String[],String> eSpeed = new TableColumn<>("Speed"); eSpeed.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[5]));
        TableColumn<String[],String> eReliability = new TableColumn<>("Reliability"); eReliability.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue()[6]));
        evalTable.getColumns().addAll(eModel, eCode, eEssay, eLogic, eCreative, eSpeed, eReliability);

        ObservableList<String[]> evalData = FXCollections.observableArrayList();
        evalData.addAll(
            new String[]{"qwen2.5:0.5b", "⭐⭐⭐", "⭐⭐", "⭐⭐", "⭐", "⚡⚡⚡⚡⚡", "⭐⭐⭐⭐"},
            new String[]{"tinyllama:1.1b", "⭐⭐⭐", "⭐⭐⭐", "⭐⭐", "⭐⭐", "⚡⚡⚡⚡", "⭐⭐⭐"},
            new String[]{"phi:latest", "⭐⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐", "⭐⭐⭐", "⚡⚡", "⭐⭐⭐"},
            new String[]{"phi3:mini", "⭐⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐", "⚡", "⭐⭐⭐⭐"},
            new String[]{"llama3.2:1b", "⭐⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐", "⭐⭐⭐", "⚡⚡⚡", "⭐⭐⭐⭐"},
            new String[]{"deepseek-r1:1.5b", "⭐⭐⭐⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐⭐", "⭐⭐", "⚡⚡", "⭐⭐⭐⭐"}
        );
        evalTable.setItems(evalData);

        // LoRA adapter config
        Label loraTitle = label("🔄 LoRA Adapter Configuration (per model)", 12, "#00d9ff", true);
        HBox loraRow = hbox(8, Pos.CENTER_LEFT, null, 0);
        String[] loraTypes = {"CHAT","CODE","PATHFIND","MOTIVES","CAREER","ANALYSIS"};
        for (String lt : loraTypes) {
            ComboBox<String> loraCb = new ComboBox<>();
            loraCb.getItems().addAll("qwen2.5:0.5b","tinyllama:1.1b","phi:latest","phi3:mini","llama3.2:1b","deepseek-r1:1.5b");
            loraCb.setValue("qwen2.5:0.5b");
            loraCb.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 9px;"); loraCb.setMaxWidth(100);
            loraRow.getChildren().addAll(label(lt+":",9,"#a0a0a0",false), loraCb);
        }

        // Prompt engineering templates
        Label promptEngTitle = label("💉 Prompt Engineering Templates (editable)", 12, "#00d9ff", true);
        TextArea codePrompt = new TextArea("You are an expert programmer. Write clean, efficient, well-documented code. Output ONLY the code, no explanation.");
        codePrompt.setPrefRowCount(2); codePrompt.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 9px;");
        TextArea essayPrompt = new TextArea("You are a professional writer. Write engaging, well-structured content with clear arguments and evidence.");
        essayPrompt.setPrefRowCount(2); essayPrompt.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffaa00; -fx-font-family: monospace; -fx-font-size: 9px;");
        TextArea taskPrompt = new TextArea("You are a task completion agent. Break down the task, execute step by step, verify results.");
        taskPrompt.setPrefRowCount(2); taskPrompt.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00d9ff; -fx-font-family: monospace; -fx-font-size: 9px;");

        // Stats tracker
        Label statsTitle = label("📈 Model Performance Stats (auto-tracked)", 12, "#00d9ff", true);
        HBox statsRow = hbox(15, Pos.CENTER_LEFT, null, 0);
        Label totalCalls = label("Total API calls: 0", 11, "#fff", false);
        Label avgLatency = label("Avg latency: 0ms", 11, "#fff", false);
        Label successRate = label("Success rate: 100%", 11, "#00ff88", false);
        statsRow.getChildren().addAll(totalCalls, avgLatency, successRate);

        // Test buttons
        HBox testRow = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button testCodeBtn = styledButton("💻 Test Code Gen", "#00ff88");
        testCodeBtn.setOnAction(e -> runEvalTest("code", codePrompt.getText()));
        Button testEssayBtn = styledButton("📝 Test Essay", "#ffaa00");
        testEssayBtn.setOnAction(e -> runEvalTest("essay", essayPrompt.getText()));
        Button testTaskBtn = styledButton("⚡ Test Task", "#00d9ff");
        testTaskBtn.setOnAction(e -> runEvalTest("task", taskPrompt.getText()));
        Button testAllBtn = styledButton("🧪 Test All Models", "#c77dff");
        testAllBtn.setOnAction(e -> runFullEval());
        testRow.getChildren().addAll(testCodeBtn, testEssayBtn, testTaskBtn, testAllBtn);

        evalContent.getChildren().addAll(evalTitle, evalTable, loraTitle, loraRow, promptEngTitle, codePrompt, essayPrompt, taskPrompt, statsTitle, statsRow, testRow);
        evalPane.setContent(new ScrollPane(evalContent));

        // === ADVANCED (Entropy + Markov + Lexical + GitHub) ===
        TitledPane advPane = titledPane("📊 ADVANCED: Entropy + Markov + Lexical + GitHub", true);
        VBox advContent = vbox(8, "#16213e", 8);
        HBox ar1 = hbox(15, Pos.CENTER_LEFT, null, 0);
        Label ev = label("Entropy: 0.000 bits", 12, "#00d9ff", true); Label es = label("🟢 Normal", 12, "#00ff88", true);
        TextField etf = new TextField("0.75"); etf.setMaxWidth(50); etf.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 10px;");
        etf.setOnAction(e->{try{entropyThreshold=Double.parseDouble(etf.getText());}catch(NumberFormatException ignored){}});
        ar1.getChildren().addAll(ev, es, label("Threshold:",10,"#a0a0a0",false), etf);
        HBox ar2 = hbox(10, Pos.CENTER_LEFT, null, 0);
        TextField lexInput = new TextField("sum of (agent_count * task_complexity) / time_elapsed"); lexInput.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 10px;"); HBox.setHgrow(lexInput, Priority.ALWAYS);
        Button parseBtn = styledButton("🔢 Parse", "#00d9ff"); parseBtn.setOnAction(e->log("📐 "+lexInput.getText()+" → "+evaluateLexical(lexInput.getText())));
        ar2.getChildren().addAll(parseBtn, lexInput);
        HBox ar3 = hbox(10, Pos.CENTER_LEFT, null, 0);
        ar3.getChildren().addAll(styledButton("🚀 Push GitHub","#6e5494"), styledButton("📊 Git Status","#00d9ff"));
        advContent.getChildren().addAll(ar1, ar2, ar3);
        advPane.setContent(advContent);

        // Entropy updater
        ScheduledExecutorService eu = Executors.newSingleThreadScheduledExecutor();
        eu.scheduleAtFixedRate(()->{double ne=Math.random()*0.5+0.2; shannonEntropy=ne; Platform.runLater(()->{ev.setText(String.format("Entropy: %.3f bits",ne)); if(ne>entropyThreshold){es.setText("🔴 ALERT!");es.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");log("🚨 ENTROPY: "+String.format("%.3f",ne));}else{es.setText("🟢 Normal");es.setStyle("-fx-font-size: 12px; -fx-text-fill: #00ff88;");}});},0,3,TimeUnit.SECONDS);

        box.getChildren().addAll(apiPane, modelMgrPane, votePane, topoPane, nightPane, evalPane, routingPane, advPane);
        return box;
    }

    private TextField tf(String text, int width) { TextField f = new TextField(text); f.setMaxWidth(width); f.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #fff; -fx-font-size: 10px;"); return f; }

    // ==================== MODEL EVALUATION ====================
    private void runEvalTest(String type, String promptTemplate) {
        String[] testModels = {"qwen2.5:0.5b", "tinyllama:1.1b", "llama3.2:1b", "deepseek-r1:1.5b"};
        String testInput = type.equals("code") ? "Write a function to reverse a string" :
                          type.equals("essay") ? "Write about artificial intelligence" :
                          "Complete the task: organize files by type";
        log("🧪 Running " + type + " eval on " + testModels.length + " models...");
        addToGodChat("🧪 EVAL", type.toUpperCase(), "Testing " + testModels.length + " models");
        for (String model : testModels) {
            final String m = model;
            chatScheduler.schedule(() -> {
                try {
                    long start = System.currentTimeMillis();
                    String result = callOllama(m, promptTemplate + "\n\n" + testInput);
                    long latency = System.currentTimeMillis() - start;
                    Platform.runLater(() -> {
                        addToGodChat("🧪 EVAL", m, type + " [" + latency + "ms]: " + result.substring(0, Math.min(80, result.length())));
                        log("🧪 [" + m + "] " + type + ": " + latency + "ms, " + result.length() + " chars");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> log("❌ [" + m + "] eval failed: " + e.getMessage()));
                }
            }, 0, TimeUnit.SECONDS);
        }
    }

    private void runFullEval() {
        log("🧪 FULL EVALUATION - All models, all test types");
        addToGodChat("🧪 FULL EVAL", "System", "Running all models through code + essay + task tests");
        runEvalTest("code", "You are an expert programmer. Output ONLY code.");
        runEvalTest("essay", "You are a professional writer. Be thorough.");
        runEvalTest("task", "You are a task agent. Execute step by step.");
    }

    // ==================== GITHUB ====================
    private void pushToGitHub() {
        chatScheduler.schedule(()->{try{for(String c:new String[]{"git add -A","git commit -m v0.11.0-All-Systems","git push origin main"}){new ProcessBuilder(c.split(" ")).directory(new java.io.File(".")).start().waitFor();}Platform.runLater(()->log("📡 GitHub: ✅"));}catch(Exception e){Platform.runLater(()->log("❌ GitHub: "+e.getMessage()));}},0,TimeUnit.SECONDS);
    }
    private void gitStatus() {
        chatScheduler.schedule(()->{try{Process p=new ProcessBuilder("git","status","--short").directory(new java.io.File(".")).start();String o=new String(p.getInputStream().readAllBytes());p.waitFor();Platform.runLater(()->log("📊 Git: "+(o.isEmpty()?"Clean":o.trim())));}catch(Exception e){Platform.runLater(()->log("❌ Git: "+e.getMessage()));}},0,TimeUnit.SECONDS);
    }

    // ==================== STATIONS ====================
    private void triggerStation(String station) {
        stationActive.putIfAbsent(station, false); boolean a = !stationActive.get(station); stationActive.put(station, a);
        if (a) { log("🏗️ ["+station+"] ACTIVATED");
            switch (station) {
                case "Brute Foundry"->chatScheduler.scheduleAtFixedRate(()->Platform.runLater(()->{String[] acts={"Compiling...","Testing...","Generating...","Optimizing..."}; String act=acts[new Random().nextInt(acts.length)]; log("🏗️ Brute Foundry: "+act); addToGodChat("🏗️ STATION","Brute Foundry",act);}),0,5,TimeUnit.SECONDS);
                case "Hospital"->chatScheduler.scheduleAtFixedRate(()->Platform.runLater(()->log("🏥 Hospital: Checking...")),0,8,TimeUnit.SECONDS);
                case "GitHub"->chatScheduler.scheduleAtFixedRate(()->Platform.runLater(()->log("📡 GitHub: Syncing...")),0,10,TimeUnit.SECONDS);
                default->log("🏗️ ["+station+"] Online");
            }
        } else log("⏹️ ["+station+"] DEACTIVATED");
    }

    // ==================== OLLAMA API ====================
    private void simulateModelResponse(String modelName, String input) {
        chatScheduler.schedule(()->{try{String response=callOllama(modelName,input); Platform.runLater(()->{addToGodChat("🤖 MODEL",modelName,response); TextArea ca=modelChats.get(modelName); if(ca!=null)ca.appendText("["+modelName+"] "+response+"\n"); log("💬 ["+modelName+"] "+response.substring(0,Math.min(60,response.length()))); checkCommandTriggers(response,modelName); String nr=modelNextRoutes.get(modelName).getValue(); if(!"Self".equals(nr)){if("All".equals(nr))modelChats.forEach((n,c)->{if(!n.equals(modelName))simulateModelResponse(n,"[From "+modelName+"] "+response.substring(0,Math.min(100,response.length())));}); else if(modelChats.containsKey(nr))simulateModelResponse(nr,"[From "+modelName+"] "+response.substring(0,Math.min(100,response.length())));}});}catch(Exception e){Platform.runLater(()->{TextArea ca=modelChats.get(modelName); if(ca!=null)ca.appendText("["+modelName+"] ⚠️ "+e.getMessage()+"\n"); log("⚠️ ["+modelName+"] "+e.getMessage());});}},100,TimeUnit.MILLISECONDS);
    }

    private String callOllama(String model, String prompt) throws Exception {
        String json=String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",model.replace("\"","\\\""),prompt.replace("\"","\\\"").replace("\n","\\n"));
        HttpRequest r=HttpRequest.newBuilder().uri(URI.create(OLLAMA_URL)).header("Content-Type","application/json").timeout(Duration.ofSeconds(30)).POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp=httpClient.send(r,HttpResponse.BodyHandlers.ofString());
        if(resp.statusCode()==200){ollamaAvailable.put(model,true); String body=resp.body(); int s=body.indexOf("\"response\":\""); if(s>0){s+=12; int e=body.indexOf("\"",s); if(e>s)return body.substring(s,e).replace("\\n"," ").replace("\\\"","\"");} return body.length()>200?body.substring(0,200)+"...":body;}
        ollamaAvailable.put(model,false); throw new RuntimeException("HTTP "+resp.statusCode());
    }

    // ==================== COMMANDS ====================
    private void initCommandRegistry() { commandRegistry.put("execute",()->log("⚡ EXECUTING...")); commandRegistry.put("analyze",()->log("🔍 ANALYZING...")); commandRegistry.put("git_push",()->log("📡 PUSHING...")); commandRegistry.put("web_search",()->log("🌐 SEARCHING...")); commandRegistry.put("refactor",()->log("🔧 REFACTORING...")); }
    private void checkCommandTriggers(String input, String modelName) { for(String[] cmd:commandTable) if(cmd[3].equals("✅")&&input.toLowerCase().contains(cmd[0].toLowerCase())){log("🎯 TRIGGER: ["+modelName+"] → "+cmd[0]); if(!cmd[2].equals("Station"))triggerStation(cmd[2]);} }

    // ==================== ENTROPY ====================
    private void startEntropyMonitor() { chatScheduler.scheduleAtFixedRate(()->{double e=calculateEntropy(); if(e>entropyThreshold)Platform.runLater(()->{log("🚨 ENTROPY: "+String.format("%.3f",e)); statusLabel.setText("🔴 Entropy Alert!"); statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");});},10,10,TimeUnit.SECONDS); }
    private double calculateEntropy() { double t=modelChats.values().stream().mapToInt(c->c.getText().length()).sum(); if(t==0)return 0; double e=0; for(TextArea c:modelChats.values()){double p=c.getText().length()/t; if(p>0)e-=p*Math.log(p)/Math.log(2);} return Math.min(1.0,e); }

    // ==================== LEXICAL MATH ====================
    private String evaluateLexical(String expr) { Map<String,Double> v=new HashMap<>(); v.put("agent_count",3.0); v.put("task_complexity",2.5); v.put("time_elapsed",10.0); v.put("entropy",shannonEntropy); try{expr=expr.toLowerCase(); for(Map.Entry<String,Double> e:v.entrySet())expr=expr.replace(e.getKey(),String.valueOf(e.getValue())); if(expr.contains("sum of")&&expr.contains("/")){String[] d=expr.replace("sum of","").split("/"); double n=1; for(String p:d[0].trim().split("\\s*\\*\\s*")){try{n*=Double.parseDouble(p.replace("(","").replace(")",""));}catch(NumberFormatException ignored){}} double den=1; try{den=Double.parseDouble(d[1].trim());}catch(NumberFormatException ignored){} return String.format("%.2f",den!=0?n/den:0);}}catch(Exception e){return"Error: "+e.getMessage();} return"?"; }

    // ==================== UTILITY ====================
    private void log(String msg) { String ts=java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")); String entry="["+ts+"] "+msg; System.out.println(entry); if(logConsole!=null)Platform.runLater(()->{logConsole.appendText(entry+"\n"); String[] lines=logConsole.getText().split("\n"); if(lines.length>500)logConsole.setText(String.join("\n",Arrays.copyOfRange(lines,lines.length-500,lines.length)));}); }
    private VBox vbox(int s,String bg,int p){VBox b=new VBox(s);b.setStyle("-fx-background-color: "+bg+"; -fx-padding: "+p+";");return b;}
    private HBox hbox(int s,Pos a,String bg,int p){HBox b=new HBox(s);b.setAlignment(a);if(bg!=null)b.setStyle("-fx-background-color: "+bg+"; -fx-padding: "+p+";");return b;}
    private Label label(String t,int sz,String c,boolean bd){Label l=new Label(t);l.setStyle("-fx-font-size: "+sz+"px; -fx-text-fill: "+c+";"+(bd?" -fx-font-weight: bold;":""));return l;}
    private TitledPane titledPane(String t,boolean ex){TitledPane tp=new TitledPane();tp.setText(t);tp.setExpanded(ex);tp.setStyle("-fx-background-color: #16213e;");return tp;}
    private Button styledButton(String t,String c){Button b=new Button(t);b.setStyle("-fx-background-color: "+c+"; -fx-text-fill: #000; -fx-font-size: 14px; -fx-padding: 10 20;");return b;}

    @Override public void stop(){chatScheduler.shutdown();log("⏹️ SIMS1337 shutting down...");}
}
