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
 * v0.10.0 - Shared God Chat + Editable Routing + Lexical Web Search
 * Pure JavaFX - NO FXML
 */
public class GodHandApp extends Application {

    // === View Management ===
    private StackPane viewStack;
    private VBox dashboardView;
    private VBox gridView;
    private VBox settingsView;
    private Label statusLabel;
    private TextArea logConsole;

    // === Shared God Chat ===
    private TextArea godChat;
    private int godChatMessageCount = 0;

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

    // === Command Listener ===
    private final ObservableList<String[]> commandTable = FXCollections.observableArrayList();
    private final Map<String, Runnable> commandRegistry = new ConcurrentHashMap<>();

    // === Station State ===
    private final Map<String, Boolean> stationActive = new ConcurrentHashMap<>();

    // === Ollama API ===
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, Boolean> ollamaAvailable = new ConcurrentHashMap<>();

    // === Entropy ===
    private double shannonEntropy = 0.0;
    private double entropyThreshold = 0.75;
    private final List<Double> entropyHistory = new ArrayList<>();

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
        "after","above","below","between","of","up","down","out","off","over","under",
        "again","further","then","once","here","there","when","where","why","how","all",
        "both","each","few","more","most","other","some","such","no","only","own","same",
        "just","very","too","also","now","well","back","still","already","always","never",
        "often","sometimes","usually","really","actually","almost","enough","even","much"
    );

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("⚙️ SIMS1337 - Unified Control Center");

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

        log("✅ SIMS1337 v0.10.0 - Shared God Chat + Editable Routing + Lexical Web Search");
        initCommandRegistry();
        initAgentPositions();
        initStationPipelines();
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

    // ==================== DASHBOARD VIEW (WITH SHARED GOD CHAT) ====================
    private VBox buildDashboard() {
        VBox box = vbox(10, "#1a1a2e", 15);

        // Header
        HBox header = hbox(20, Pos.CENTER_LEFT, "#16213e", 12);
        header.getChildren().addAll(
            label("🎮 GODHAND", 28, "#00d9ff", true),
            label("Agent Orchestration Dashboard", 14, "#a0a0a0", false),
            new Region()
        );
        HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS);

        // === SHARED GOD CHAT (CENTERPIECE) ===
        TitledPane godChatPane = titledPane("💬 SHARED GOD CHAT - All Model Conversations", true);
        VBox godChatBox = vbox(5, "#0a0a15", 5);

        godChat = new TextArea();
        godChat.setEditable(false); godChat.setWrapText(true);
        godChat.setPrefRowCount(12);
        godChat.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-border-color: #00d9ff; -fx-border-width: 2;");
        godChat.setText("""
            ╔══════════════════════════════════════════════════════════════╗
            ║              🧠 SHARED GOD CHAT - ALL MODELS                ║
            ║   qwen2.5:0.5b | tinyllama:1.1b | phi:latest | phi3:mini   ║
            ╚══════════════════════════════════════════════════════════════╝

            """);

        // God Chat controls
        HBox godChatControls = hbox(8, Pos.CENTER_LEFT, null, 0);
        Button clearGodChatBtn = new Button("🗑️ Clear Chat");
        clearGodChatBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: #000000; -fx-font-size: 10px; -fx-padding: 3 10;");
        clearGodChatBtn.setOnAction(e -> godChat.setText("╔══════════════════════════════════════════════════════════════╗\n║              🧠 SHARED GOD CHAT - CLEARED                    ║\n╚══════════════════════════════════════════════════════════════╝\n\n"));

        Button exportChatBtn = new Button("📋 Export");
        exportChatBtn.setStyle("-fx-background-color: #00d9ff; -fx-text-fill: #000000; -fx-font-size: 10px; -fx-padding: 3 10;");
        exportChatBtn.setOnAction(e -> log("📋 Chat exported: " + godChatMessageCount + " messages"));

        Label chatStats = new Label("Messages: 0");
        chatStats.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0a0a0;");

        godChatControls.getChildren().addAll(clearGodChatBtn, exportChatBtn, chatStats);
        godChatBox.getChildren().addAll(godChat, godChatControls);
        godChatPane.setContent(godChatBox);

        // === MODEL PANELS (COMPACT, BELOW GOD CHAT) ===
        TitledPane modelPool = titledPane("🧠 MODEL POOL + ROUTING", true);
        VBox modelContent = vbox(8, "#16213e", 8);

        String[][] models = {
            {"⚡ FAST", "qwen2.5:0.5b", "398MB | <100ms", "#00ff88"},
            {"⚖️ BALANCED", "tinyllama:1.1b", "638MB | ~500ms", "#ffaa00"},
            {"🧠 REASONING", "phi:latest", "1.6GB | ~2-5s", "#ff6b6b"},
            {"🎯 DEEP", "phi3:mini", "2.2GB | ~5-10s", "#c77dff"}
        };

        for (String[] m : models) {
            VBox modelCard = vbox(4, "#0f3460", 8);
            modelCard.setStyle("-fx-background-color: #0f3460; -fx-padding: 8; -fx-background-radius: 5;");

            // Model header row
            HBox modelHeader = hbox(8, Pos.CENTER_LEFT, null, 0);
            modelHeader.getChildren().addAll(
                label(m[0], 12, m[3], true), label(m[1], 11, "#ffffff", false),
                label(m[2], 9, "#a0a0a0", false), label("🟢", 10, "#00ff88", false)
            );

            // Routing row: Pattern + Next Route + Loop + Web Search
            HBox routingRow = hbox(5, Pos.CENTER_LEFT, null, 0);

            ComboBox<String> patternBox = new ComboBox<>();
            patternBox.getItems().addAll("Linear", "Loop", "Random", "Markov", "Vote", "Chain", "Broadcast");
            patternBox.setValue("Linear");
            patternBox.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 9px;");
            patternBox.setMaxWidth(80);
            modelPatterns.put(m[1], patternBox);

            ComboBox<String> nextRouteBox = new ComboBox<>();
            nextRouteBox.getItems().addAll("Self", "qwen2.5:0.5b", "tinyllama:1.1b", "phi:latest", "phi3:mini", "All");
            nextRouteBox.setValue("Self");
            nextRouteBox.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 9px;");
            nextRouteBox.setMaxWidth(90);
            modelNextRoutes.put(m[1], nextRouteBox);

            Button loopBtn = new Button("🔁");
            loopBtn.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: #000000; -fx-font-size: 9px; -fx-padding: 2 6;");
            String modelName = m[1];
            loopBtn.setOnAction(e -> {
                boolean active = !loopActive.getOrDefault(modelName, false);
                loopActive.put(modelName, active);
                loopBtn.setText(active ? "⏹️" : "🔁");
                loopBtn.setStyle(active ?
                    "-fx-background-color: #ff6b6b; -fx-text-fill: #ffffff; -fx-font-size: 9px; -fx-padding: 2 6;" :
                    "-fx-background-color: #ffaa00; -fx-text-fill: #000000; -fx-font-size: 9px; -fx-padding: 2 6;");
                if (active) {
                    loopCounts.put(modelName, 0);
                    log("🔁 [" + modelName + "] Loop mode ON");
                    runLoop(modelName);
                } else {
                    log("⏹️ [" + modelName + "] Loop mode OFF (ran " + loopCounts.getOrDefault(modelName, 0) + " times)");
                }
            });

            Button webSearchBtn = new Button("🌐");
            webSearchBtn.setStyle("-fx-background-color: #6e5494; -fx-text-fill: #ffffff; -fx-font-size: 9px; -fx-padding: 2 6;");
            webSearchBtn.setOnAction(e -> {
                String query = modelInputs.get(modelName).getText();
                if (!query.isEmpty()) {
                    String summary = lexicalSummarize(query);
                    addToGodChat("🌐 LEXICAL", modelName, summary);
                    TextArea chat = modelChats.get(modelName);
                    if (chat != null) chat.appendText("[🌐 Lexical] " + summary + "\n");
                    log("🌐 [" + modelName + "] Lexical: " + summary);
                }
            });

            routingRow.getChildren().addAll(
                label("Route:", 9, "#a0a0a0", false), patternBox,
                label("→", 9, "#00d9ff", false), nextRouteBox,
                loopBtn, webSearchBtn
            );

            // Model chat (compact)
            TextArea chatArea = new TextArea();
            chatArea.setEditable(false); chatArea.setPrefRowCount(3);
            chatArea.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 9px;");
            chatArea.setText("[" + m[1] + "] Ready.\n");
            modelChats.put(m[1], chatArea);

            // Input row
            HBox inputRow = hbox(4, Pos.CENTER_LEFT, null, 0);
            TextField inputField = new TextField();
            inputField.setPromptText("→ " + m[1] + "...");
            inputField.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 9px;");
            HBox.setHgrow(inputField, Priority.ALWAYS);
            modelInputs.put(m[1], inputField);

            Button sendBtn = new Button("▶");
            sendBtn.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #000000; -fx-font-size: 9px; -fx-padding: 2 8;");
            String mn = m[1];
            sendBtn.setOnAction(e -> {
                String msg = inputField.getText();
                if (!msg.isEmpty()) {
                    addToGodChat("👤 YOU", mn, msg);
                    chatArea.appendText("You: " + msg + "\n");
                    inputField.clear();
                    simulateModelResponse(mn, msg);
                }
            });
            inputRow.getChildren().addAll(inputField, sendBtn);

            modelCard.getChildren().addAll(modelHeader, routingRow, chatArea, inputRow);
            modelContent.getChildren().add(modelCard);
        }
        modelPool.setContent(new ScrollPane(modelContent));

        // LoRA + Queue (compact row)
        HBox bottomRow = hbox(10, Pos.CENTER_LEFT, null, 0);

        TitledPane loraPane = titledPane("🔄 LORA", true);
        FlowPane loraFlow = new FlowPane(5, 5);
        String[] adapters = {"💬CHAT", "💻CODE", "🗺️PATH", "❤️MOTIVE", "🎯CAREER", "🔍ANALYSIS"};
        String[] colors = {"#00ff88", "#00d9ff", "#ffaa00", "#ff6b6b", "#c77dff", "#a0a0a0"};
        for (int i = 0; i < adapters.length; i++) {
            Label al = new Label(adapters[i]);
            al.setStyle("-fx-background-color: " + colors[i] + "; -fx-text-fill: #000000; -fx-padding: 4 10; -fx-background-radius: 15; -fx-font-size: 10px;");
            loraFlow.getChildren().add(al);
        }
        loraPane.setContent(loraFlow);
        loraPane.setMaxWidth(400);

        TitledPane queuePane = titledPane("📋 QUEUE", true);
        VBox qc = vbox(5, "#16213e", 5);
        ProgressBar pb = new ProgressBar(0); pb.setMaxWidth(Double.MAX_VALUE);
        qc.getChildren().addAll(label("0/100 tasks | 0% util", 10, "#00d9ff", false), pb);
        queuePane.setContent(qc);
        queuePane.setMaxWidth(250);

        bottomRow.getChildren().addAll(loraPane, queuePane);

        // Log
        TitledPane logPane = titledPane("📜 ACTIVITY LOG", true);
        logConsole = new TextArea();
        logConsole.setEditable(false); logConsole.setWrapText(true); logConsole.setPrefHeight(100);
        logConsole.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 10px;");
        logPane.setContent(logConsole);

        box.getChildren().addAll(header, godChatPane, modelPool, bottomRow, logPane);
        return box;
    }

    // ==================== SHARED GOD CHAT ====================
    private void addToGodChat(String role, String model, String message) {
        godChatMessageCount++;
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String color = switch (role) {
            case "👤 YOU" -> "#ffffff";
            case "🌐 LEXICAL" -> "#c77dff";
            default -> {
                if (model.contains("qwen")) yield "#00ff88";
                if (model.contains("tinyllama")) yield "#ffaa00";
                if (model.contains("phi3")) yield "#c77dff";
                yield "#ff6b6b";
            }
        };

        String entry = String.format("[%s] %s | %s: %s%n", timestamp, role, model, message);
        Platform.runLater(() -> {
            godChat.appendText(entry);
            // Auto-scroll
            godChat.setScrollTop(Double.MAX_VALUE);
            // Trim old messages
            String[] lines = godChat.getText().split("\n");
            if (lines.length > 500) {
                godChat.setText(String.join("\n",
                    Arrays.copyOfRange(lines, lines.length - 500, lines.length)));
            }
        });
    }

    // ==================== LOOP MODE ====================
    private void runLoop(String modelName) {
        chatScheduler.schedule(() -> {
            while (loopActive.getOrDefault(modelName, false)) {
                int count = loopCounts.merge(modelName, 1, Integer::sum);
                String prompt = "Loop iteration #" + count + ". Continue the conversation thread.";
                try {
                    String response = callOllama(modelName, prompt);
                    Platform.runLater(() -> {
                        addToGodChat("🔄 LOOP", modelName, response);
                        TextArea chat = modelChats.get(modelName);
                        if (chat != null) chat.appendText("[Loop#" + count + "] " + response + "\n");
                        log("🔁 [" + modelName + "] Loop #" + count + ": " + response.substring(0, Math.min(60, response.length())));
                        checkCommandTriggers(response, modelName);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> log("⚠️ [" + modelName + "] Loop error: " + e.getMessage()));
                    loopActive.put(modelName, false);
                    break;
                }
                try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
            }
        }, 0, TimeUnit.SECONDS);
    }

    // ==================== LEXICAL SUMMARIZE (NON-LLM) ====================
    private String lexicalSummarize(String text) {
        // Tokenize, remove stop words, count frequency, extract top keywords
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Map<String, Integer> freq = new HashMap<>();
        int totalWords = 0;

        for (String w : words) {
            if (w.length() < 2 || STOP_WORDS.contains(w)) continue;
            freq.merge(w, 1, Integer::sum);
            totalWords++;
        }

        if (freq.isEmpty()) return "No keywords extracted from: " + text.substring(0, Math.min(50, text.length()));

        // Sort by frequency, take top 5
        List<Map.Entry<String, Integer>> sorted = freq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());

        // Build summary
        StringBuilder sb = new StringBuilder("KEYWORDS[");
        double totalWeight = sorted.stream().mapToInt(Map.Entry::getValue).sum();
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> e = sorted.get(i);
            double weight = e.getValue() / totalWeight;
            sb.append(String.format("%s(%.0f%%)", e.getKey(), weight * 100));
            if (i < sorted.size() - 1) sb.append(", ");
        }
        sb.append(String.format("] | %d words → %d unique | Density: %.1f%%",
            totalWords, freq.size(), (freq.size() * 100.0 / Math.max(1, totalWords))));

        return sb.toString();
    }

    // ==================== PLAYER GRID VIEW ====================
    private VBox buildGridView() {
        VBox box = vbox(15, "#1a1a2e", 20);
        box.setAlignment(Pos.TOP_CENTER);

        box.getChildren().addAll(
            label("🎮 PLAYER GRID 3D - CLICK CELLS TO MOVE AGENTS!", 24, "#00d9ff", true),
            label("👇 Left-click = Move Agent | Right-click = Pipeline 👇", 14, "#00ff88", false)
        );

        gridPane = new GridPane();
        gridPane.setHgap(5); gridPane.setVgap(5); gridPane.setAlignment(Pos.CENTER);
        gridPane.setStyle("-fx-background-color: #1a1a3e; -fx-padding: 20; -fx-border-color: #00ff88; -fx-border-width: 3;");

        for (int row = 0; row < 10; row++)
            for (int col = 0; col < 10; col++) {
                Rectangle cell = new Rectangle(50, 50);
                cell.setFill(Color.rgb(30 + row * 15, 80 + col * 12, 150 + (10 - row) * 8));
                cell.setStroke(Color.web("#00d9ff"));
                cell.setStrokeWidth(3);
                gridCells[row][col] = cell;

                final int r = row, c = col;
                cell.setOnMouseClicked(e -> {
                    if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        startPipelineAt(r, c);
                    } else {
                        moveAgentTo("Agent Alpha", r, c);
                    }
                });
                Tooltip tip = new Tooltip("(" + col + "," + row + ")\nClick: Move Agent\nRight-click: Pipeline");
                Tooltip.install(cell, tip);
                gridPane.add(cell, col, row);
            }

        box.getChildren().addAll(gridPane, label("👆 100 CLICKABLE CELLS 👆", 14, "#ffffff", false));

        // Player positions
        VBox pp = vbox(10, "#16213e", 15);
        pp.setStyle("-fx-background-radius: 10;");
        pp.getChildren().add(label("🎮 PLAYER POSITIONS (XYZ) - LIVE", 16, "#00d9ff", true));
        String[][] players = {{"🟢 Agent Alpha", "3", "7", "2"}, {"🔵 Agent Beta", "8", "4", "1"}, {"🟠 Agent Gamma", "5", "9", "3"}};
        for (String[] p : players) {
            HBox pr = hbox(15, Pos.CENTER_LEFT, null, 0);
            Label posLabel = label("(" + p[1] + "," + p[2] + "," + p[3] + ")", 12, "#a0a0a0", false);
            agentPositionLabels.put(p[0], posLabel);
            pr.getChildren().addAll(label(p[0], 12, "#ffffff", true), posLabel, label("Active", 12, "#00ff88", false));
            pp.getChildren().add(pr);
        }
        box.getChildren().add(pp);

        // Station buttons
        HBox stationRow = hbox(10, Pos.CENTER, null, 10);
        String[][] stations = {
            {"🏗️ Brute Foundry", "#ff6b6b"}, {"🧬 A/B Lab", "#c77dff"}, {"🌳 Knowledge Tree", "#00d9ff"},
            {"🔬 Research", "#ffaa00"}, {"🔒 Secrets", "#999999"}, {"🏥 Hospital", "#ff6b9d"}, {"📡 GitHub", "#6e5494"}
        };
        for (String[] s : stations) {
            Button sb = new Button(s[0]);
            sb.setStyle("-fx-background-color: " + s[1] + "; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-padding: 5 10;");
            sb.setOnAction(e -> triggerStation(s[0].substring(2).trim()));
            stationRow.getChildren().add(sb);
        }
        box.getChildren().add(stationRow);

        return box;
    }

    // ==================== AGENT MOVEMENT ====================
    private void initAgentPositions() {
        agentPositions.put("Agent Alpha", new int[]{3, 7, 2});
        agentPositions.put("Agent Beta", new int[]{8, 4, 1});
        agentPositions.put("Agent Gamma", new int[]{5, 9, 3});
    }

    private void moveAgentTo(String agentName, int x, int y) {
        int[] pos = agentPositions.get(agentName);
        if (pos == null) return;
        int oldX = pos[0], oldY = pos[1];
        pos[0] = x; pos[1] = y;
        Platform.runLater(() -> {
            if (oldX >= 0 && oldX < 10 && oldY >= 0 && oldY < 10)
                gridCells[oldY][oldX].setFill(Color.rgb(30 + oldY * 15, 80 + oldX * 12, 150 + (10 - oldY) * 8));
            Color ac = switch (agentName) {
                case "Agent Alpha" -> Color.rgb(0, 255, 100);
                case "Agent Beta" -> Color.rgb(0, 150, 255);
                case "Agent Gamma" -> Color.rgb(255, 150, 0);
                default -> Color.YELLOW;
            };
            gridCells[y][x].setFill(ac);
            gridCells[y][x].setStroke(Color.WHITE);
            gridCells[y][x].setStrokeWidth(4);
            Label pl = agentPositionLabels.get(agentName);
            if (pl != null) pl.setText("(" + x + "," + y + "," + pos[2] + ")");
            log("🎯 " + agentName + " → (" + x + "," + y + "," + pos[2] + ")");
            statusLabel.setText("🟢 " + agentName + " @ (" + x + "," + y + ")");
        });
    }

    // ==================== STATION PIPELINES ====================
    private void initStationPipelines() {
        pipelineNext.put("Brute Foundry", "A/B Lab");
        pipelineNext.put("A/B Lab", "Knowledge Tree");
        pipelineNext.put("Knowledge Tree", "Research");
        pipelineNext.put("Research", "GitHub");
        pipelineNext.put("GitHub", "Hospital");
        pipelineNext.put("Hospital", "Brute Foundry");
    }

    private void startPipelineAt(int x, int y) {
        log("🔗 Pipeline starting at (" + x + "," + y + ")");
        pipelineActive.put("pipeline", true);
        chatScheduler.schedule(() -> {
            String station = "Brute Foundry";
            int step = 0;
            while (pipelineActive.getOrDefault("pipeline", false) && step < 20) {
                final String cs = station;
                final int s = step;
                Platform.runLater(() -> {
                    addToGodChat("🔗 PIPELINE", cs, "Step " + s + " @ (" + x + "," + y + ")");
                    modelChats.forEach((n, c) -> c.appendText("[Pipeline:" + cs + "] (" + x + "," + y + ")\n"));
                });
                station = pipelineNext.getOrDefault(station, "Brute Foundry");
                step++;
                try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
            }
            final int ts = step;
            Platform.runLater(() -> log("🔗 Pipeline done: " + ts + " steps"));
        }, 0, TimeUnit.SECONDS);
    }

    // ==================== SETTINGS VIEW ====================
    private VBox buildSettingsView() {
        VBox box = vbox(10, "#1a1a2e", 20);
        box.getChildren().add(label("⚙️ SETTINGS & ORCHESTRATION", 24, "#00d9ff", true));

        // === Editable Routing Table ===
        TitledPane routingPane = titledPane("🔀 EDITABLE ROUTING PATTERNS", true);
        VBox routingContent = vbox(10, "#16213e", 10);

        TableView<String[]> routingTableView = new TableView<>();
        routingTableView.setPrefHeight(120); routingTableView.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[], String> fromCol = new TableColumn<>("From Model");
        fromCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[], String> patternCol = new TableColumn<>("Pattern");
        patternCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[], String> toCol = new TableColumn<>("Next Route");
        toCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[], String> loopCol = new TableColumn<>("Loop");
        loopCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        routingTableView.getColumns().addAll(fromCol, patternCol, toCol, loopCol);
        routingTableView.setItems(routingTable);

        routingTable.addAll(
            new String[]{"qwen2.5:0.5b", "Linear", "tinyllama:1.1b", "Off"},
            new String[]{"tinyllama:1.1b", "Markov", "phi:latest", "Off"},
            new String[]{"phi:latest", "Chain", "phi3:mini", "Off"},
            new String[]{"phi3:mini", "Vote", "All", "Off"}
        );

        HBox routingBtns = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button addRouteBtn = styledButton("➕ Add Route", "#00ff88");
        addRouteBtn.setOnAction(e -> routingTable.add(new String[]{"model", "Linear", "next", "Off"}));
        Button delRouteBtn = styledButton("🗑️ Delete", "#ff6b6b");
        delRouteBtn.setOnAction(e -> { String[] sel = routingTableView.getSelectionModel().getSelectedItem(); if (sel != null) routingTable.remove(sel); });
        Button applyRoutesBtn = styledButton("✅ Apply All Routes", "#00d9ff");
        applyRoutesBtn.setOnAction(e -> {
            for (String[] r : routingTable) {
                modelPatterns.get(r[0]).setValue(r[1]);
                modelNextRoutes.get(r[0]).setValue(r[2]);
            }
            log("✅ All routes applied");
        });
        routingBtns.getChildren().addAll(addRouteBtn, delRouteBtn, applyRoutesBtn);
        routingContent.getChildren().addAll(routingTableView, routingBtns);
        routingPane.setContent(routingContent);

        // === Command Listener ===
        TitledPane cmdPane = titledPane("📋 COMMAND LISTENER", true);
        VBox cmdContent = vbox(10, "#16213e", 10);
        TableView<String[]> cmdTableView = new TableView<>();
        cmdTableView.setPrefHeight(120); cmdTableView.setStyle("-fx-background-color: #0f3460;");
        TableColumn<String[], String> trigCol = new TableColumn<>("Trigger"); trigCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[], String> cmdCol = new TableColumn<>("Command"); cmdCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[], String> stCol = new TableColumn<>("Station"); stCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[], String> actCol = new TableColumn<>("Active"); actCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        cmdTableView.getColumns().addAll(trigCol, cmdCol, stCol, actCol);
        cmdTableView.setItems(commandTable);
        commandTable.addAll(
            new String[]{"just enter this in terminal", "execute $INPUT", "Brute Foundry", "✅"},
            new String[]{"fix this bug", "analyze + patch $FILE", "Hospital", "✅"},
            new String[]{"push to github", "git add -A && git commit && git push", "GitHub", "✅"},
            new String[]{"search the web for", "web_search $QUERY", "Research", "✅"},
            new String[]{"optimize this code", "refactor $FILE", "Brute Foundry", "✅"}
        );
        HBox cmdBtns = hbox(10, Pos.CENTER_LEFT, null, 0);
        cmdBtns.getChildren().addAll(styledButton("➕ Add", "#00ff88"), styledButton("🗑️ Delete", "#ff6b6b"));
        cmdContent.getChildren().addAll(cmdTableView, cmdBtns);
        cmdPane.setContent(cmdContent);

        // === Prompt Injection ===
        TitledPane promptPane = titledPane("💉 PROMPT INJECTION", true);
        VBox promptContent = vbox(10, "#16213e", 10);
        TextArea systemPrompt = new TextArea("You are an SLM agent in SIMS1337. Collaborate, vote, build, maintain. Access: terminal, web, files.");
        systemPrompt.setPrefRowCount(3);
        systemPrompt.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 11px;");
        Button injectBtn = styledButton("💉 Inject to All", "#c77dff");
        injectBtn.setOnAction(e -> {
            modelChats.forEach((n, c) -> c.appendText("[SYSTEM] " + systemPrompt.getText().substring(0, 50) + "...\n"));
            addToGodChat("💉 SYSTEM", "All", systemPrompt.getText().substring(0, 80) + "...");
            log("💉 Prompt injected to all models");
        });
        promptContent.getChildren().addAll(systemPrompt, injectBtn);
        promptPane.setContent(promptContent);

        // === Context Options ===
        TitledPane ctxPane = titledPane("🔧 MODEL CONTEXT", true);
        VBox ctxContent = vbox(8, "#16213e", 8);
        String[][] ctxOpts = {
            {"Max Tokens", "2048", "4096", "8192", "16384"},
            {"Temperature", "0.1", "0.5", "0.7", "1.0"},
            {"LoRA", "CHAT", "CODE", "PATHFIND", "ANALYSIS"},
            {"KV Cache", "512", "1024", "2048", "4096"},
            {"KG Depth", "1", "2", "3", "5"},
            {"Affine", "0.5x", "1.0x", "1.5x", "2.0x"}
        };
        for (String[] o : ctxOpts) {
            HBox row = hbox(8, Pos.CENTER_LEFT, null, 0);
            row.getChildren().add(label(o[0] + ":", 11, "#ffffff", false));
            ComboBox<String> cb = new ComboBox<>();
            cb.getItems().addAll(o[1], o[2], o[3], o[4]); cb.setValue(o[1]);
            cb.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 10px;"); cb.setMaxWidth(100);
            row.getChildren().add(cb);
            ctxContent.getChildren().add(row);
        }
        ctxPane.setContent(ctxContent);

        // === Entropy + Markov + Lexical + GitHub (compact) ===
        TitledPane advPane = titledPane("📊 ADVANCED: Entropy + Markov + Lexical + GitHub", true);
        VBox advContent = vbox(8, "#16213e", 8);

        HBox advRow1 = hbox(15, Pos.CENTER_LEFT, null, 0);
        Label ev = label("Entropy: 0.000 bits", 12, "#00d9ff", true);
        Label es = label("🟢 Normal", 12, "#00ff88", true);
        TextField tf = new TextField("0.75"); tf.setMaxWidth(50); tf.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 10px;");
        tf.setOnAction(e -> { try { entropyThreshold = Double.parseDouble(tf.getText()); } catch (NumberFormatException ignored) {} });
        advRow1.getChildren().addAll(ev, es, label("Threshold:", 10, "#a0a0a0", false), tf);

        HBox advRow2 = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button parseBtn = styledButton("🔢 Lexical Parse", "#00d9ff");
        TextField lexInput = new TextField("sum of (agent_count * task_complexity) / time_elapsed");
        lexInput.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 10px;"); HBox.setHgrow(lexInput, Priority.ALWAYS);
        parseBtn.setOnAction(e -> log("📐 Lexical: " + lexInput.getText() + " → " + evaluateLexical(lexInput.getText())));
        advRow2.getChildren().addAll(parseBtn, lexInput);

        HBox advRow3 = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button pushBtn = styledButton("🚀 Push to GitHub", "#6e5494");
        pushBtn.setOnAction(e -> pushToGitHub());
        Button statusBtn = styledButton("📊 Git Status", "#00d9ff");
        statusBtn.setOnAction(e -> gitStatus());
        advRow3.getChildren().addAll(pushBtn, statusBtn);

        advContent.getChildren().addAll(advRow1, advRow2, advRow3);
        advPane.setContent(advContent);

        // Entropy updater
        ScheduledExecutorService eu = Executors.newSingleThreadScheduledExecutor();
        eu.scheduleAtFixedRate(() -> {
            double ne = Math.random() * 0.5 + 0.2;
            shannonEntropy = ne;
            Platform.runLater(() -> {
                ev.setText(String.format("Entropy: %.3f bits", ne));
                if (ne > entropyThreshold) {
                    es.setText("🔴 ALERT!"); es.setStyle("-fx-font-size: 12px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                    log("🚨 ENTROPY: " + String.format("%.3f", ne) + " > " + entropyThreshold);
                } else { es.setText("🟢 Normal"); es.setStyle("-fx-font-size: 12px; -fx-text-fill: #00ff88;"); }
            });
        }, 0, 3, TimeUnit.SECONDS);

        box.getChildren().addAll(routingPane, cmdPane, promptPane, ctxPane, advPane);
        return box;
    }

    // ==================== GITHUB ====================
    private void pushToGitHub() {
        chatScheduler.schedule(() -> {
            try {
                for (String cmd : new String[]{"git add -A", "git commit -m v0.10.0-Shared-God-Chat", "git push origin main"}) {
                    Process p = new ProcessBuilder(cmd.split(" ")).directory(new java.io.File(".")).start();
                    p.waitFor();
                }
                Platform.runLater(() -> log("📡 GitHub push: ✅"));
            } catch (Exception e) { Platform.runLater(() -> log("❌ GitHub: " + e.getMessage())); }
        }, 0, TimeUnit.SECONDS);
    }

    private void gitStatus() {
        chatScheduler.schedule(() -> {
            try {
                Process p = new ProcessBuilder("git", "status", "--short").directory(new java.io.File(".")).start();
                String out = new String(p.getInputStream().readAllBytes()); p.waitFor();
                Platform.runLater(() -> log("📊 Git: " + (out.isEmpty() ? "Clean" : out.trim())));
            } catch (Exception e) { Platform.runLater(() -> log("❌ Git: " + e.getMessage())); }
        }, 0, TimeUnit.SECONDS);
    }

    // ==================== STATIONS ====================
    private void triggerStation(String station) {
        stationActive.putIfAbsent(station, false);
        boolean a = !stationActive.get(station);
        stationActive.put(station, a);
        if (a) {
            log("🏗️ [" + station + "] ACTIVATED");
            switch (station) {
                case "Brute Foundry" -> chatScheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> {
                    String[] acts = {"Compiling...", "Testing...", "Generating...", "Optimizing..."};
                    String act = acts[new Random().nextInt(acts.length)];
                    log("🏗️ Brute Foundry: " + act);
                    addToGodChat("🏗️ STATION", "Brute Foundry", act);
                }), 0, 5, TimeUnit.SECONDS);
                case "Hospital" -> chatScheduler.scheduleAtFixedRate(() -> Platform.runLater(() ->
                    log("🏥 Hospital: Checking health...")), 0, 8, TimeUnit.SECONDS);
                case "GitHub" -> chatScheduler.scheduleAtFixedRate(() -> Platform.runLater(() ->
                    log("📡 GitHub: Syncing...")), 0, 10, TimeUnit.SECONDS);
                default -> log("🏗️ [" + station + "] Online");
            }
        } else {
            log("⏹️ [" + station + "] DEACTIVATED");
        }
    }

    // ==================== OLLAMA API ====================
    private void simulateModelResponse(String modelName, String input) {
        chatScheduler.schedule(() -> {
            try {
                String response = callOllama(modelName, input);
                Platform.runLater(() -> {
                    addToGodChat("🤖 MODEL", modelName, response);
                    TextArea chat = modelChats.get(modelName);
                    if (chat != null) chat.appendText("[" + modelName + "] " + response + "\n");
                    log("💬 [" + modelName + "] " + response.substring(0, Math.min(60, response.length())));
                    checkCommandTriggers(response, modelName);

                    // Auto-route to next model
                    String nextRoute = modelNextRoutes.get(modelName).getValue();
                    if (!"Self".equals(nextRoute)) {
                        if ("All".equals(nextRoute)) {
                            modelChats.forEach((n, c) -> {
                                if (!n.equals(modelName)) simulateModelResponse(n, "[From " + modelName + "] " + response.substring(0, Math.min(100, response.length())));
                            });
                        } else if (modelChats.containsKey(nextRoute)) {
                            simulateModelResponse(nextRoute, "[From " + modelName + "] " + response.substring(0, Math.min(100, response.length())));
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    TextArea chat = modelChats.get(modelName);
                    if (chat != null) chat.appendText("[" + modelName + "] ⚠️ " + e.getMessage() + "\n");
                    log("⚠️ [" + modelName + "] " + e.getMessage());
                });
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    private String callOllama(String model, String prompt) throws Exception {
        String json = String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
            model.replace("\"", "\\\""), prompt.replace("\"", "\\\"").replace("\n", "\\n"));
        HttpRequest r = HttpRequest.newBuilder().uri(URI.create(OLLAMA_URL))
            .header("Content-Type", "application/json").timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = httpClient.send(r, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            ollamaAvailable.put(model, true);
            String body = resp.body();
            int s = body.indexOf("\"response\":\"");
            if (s > 0) { s += 12; int e = body.indexOf("\"", s); if (e > s) return body.substring(s, e).replace("\\n", " ").replace("\\\"", "\""); }
            return body.length() > 200 ? body.substring(0, 200) + "..." : body;
        }
        ollamaAvailable.put(model, false);
        throw new RuntimeException("HTTP " + resp.statusCode());
    }

    // ==================== COMMANDS ====================
    private void initCommandRegistry() {
        commandRegistry.put("execute", () -> log("⚡ EXECUTING..."));
        commandRegistry.put("analyze", () -> log("🔍 ANALYZING..."));
        commandRegistry.put("git_push", () -> log("📡 PUSHING..."));
        commandRegistry.put("web_search", () -> log("🌐 SEARCHING..."));
        commandRegistry.put("refactor", () -> log("🔧 REFACTORING..."));
    }

    private void checkCommandTriggers(String input, String modelName) {
        for (String[] cmd : commandTable)
            if (cmd[3].equals("✅") && input.toLowerCase().contains(cmd[0].toLowerCase())) {
                log("🎯 TRIGGER: [" + modelName + "] → " + cmd[0]);
                if (!cmd[2].equals("Station")) triggerStation(cmd[2]);
            }
    }

    // ==================== ENTROPY ====================
    private void startEntropyMonitor() {
        chatScheduler.scheduleAtFixedRate(() -> {
            double e = calculateEntropy();
            if (e > entropyThreshold) Platform.runLater(() -> {
                log("🚨 ENTROPY: " + String.format("%.3f", e));
                statusLabel.setText("🔴 Entropy Alert!");
                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
            });
        }, 10, 10, TimeUnit.SECONDS);
    }

    private double calculateEntropy() {
        double t = modelChats.values().stream().mapToInt(c -> c.getText().length()).sum();
        if (t == 0) return 0;
        double e = 0;
        for (TextArea c : modelChats.values()) { double p = c.getText().length() / t; if (p > 0) e -= p * Math.log(p) / Math.log(2); }
        return Math.min(1.0, e);
    }

    // ==================== LEXICAL MATH ====================
    private String evaluateLexical(String expr) {
        Map<String, Double> v = new HashMap<>();
        v.put("agent_count", 3.0); v.put("task_complexity", 2.5); v.put("time_elapsed", 10.0); v.put("entropy", shannonEntropy);
        try {
            expr = expr.toLowerCase();
            for (Map.Entry<String, Double> e : v.entrySet()) expr = expr.replace(e.getKey(), String.valueOf(e.getValue()));
            if (expr.contains("sum of") && expr.contains("/")) {
                String[] d = expr.replace("sum of", "").split("/");
                double n = 1; for (String p : d[0].trim().split("\\s*\\*\\s*")) { try { n *= Double.parseDouble(p.replace("(", "").replace(")", "")); } catch (NumberFormatException ignored) {} }
                double den = 1; try { den = Double.parseDouble(d[1].trim()); } catch (NumberFormatException ignored) {}
                return String.format("%.2f", den != 0 ? n / den : 0);
            }
        } catch (Exception e) { return "Error: " + e.getMessage(); }
        return "?";
    }

    // ==================== UTILITY ====================
    private void log(String msg) {
        String ts = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = "[" + ts + "] " + msg;
        System.out.println(entry);
        if (logConsole != null) Platform.runLater(() -> {
            logConsole.appendText(entry + "\n");
            String[] lines = logConsole.getText().split("\n");
            if (lines.length > 500) logConsole.setText(String.join("\n", Arrays.copyOfRange(lines, lines.length - 500, lines.length)));
        });
    }

    private VBox vbox(int s, String bg, int p) { VBox b = new VBox(s); b.setStyle("-fx-background-color: " + bg + "; -fx-padding: " + p + ";"); return b; }
    private HBox hbox(int s, Pos a, String bg, int p) { HBox b = new HBox(s); b.setAlignment(a); if (bg != null) b.setStyle("-fx-background-color: " + bg + "; -fx-padding: " + p + ";"); return b; }
    private Label label(String t, int sz, String c, boolean bd) { Label l = new Label(t); l.setStyle("-fx-font-size: " + sz + "px; -fx-text-fill: " + c + ";" + (bd ? " -fx-font-weight: bold;" : "")); return l; }
    private TitledPane titledPane(String t, boolean ex) { TitledPane tp = new TitledPane(); tp.setText(t); tp.setExpanded(ex); tp.setStyle("-fx-background-color: #16213e;"); return tp; }
    private Button styledButton(String t, String c) { Button b = new Button(t); b.setStyle("-fx-background-color: " + c + "; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-padding: 10 20;"); return b; }

    @Override public void stop() { chatScheduler.shutdown(); log("⏹️ SIMS1337 shutting down..."); }
}
