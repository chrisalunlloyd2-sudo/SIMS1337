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

/**
 * SIMS NEO 1337 - Complete GodHand + Player Grid + Model Orchestration
 * v0.7.0 - Models, Chats, Boolean Orchestrators, Menus, Stations, Entropy, Markov
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

    // === Model Chat ===
    private final Map<String, TextArea> modelChats = new ConcurrentHashMap<>();
    private final Map<String, TextField> modelInputs = new ConcurrentHashMap<>();
    private final Map<String, ComboBox<String>> modelPatterns = new ConcurrentHashMap<>();
    private final ScheduledExecutorService chatScheduler = Executors.newScheduledThreadPool(4);

    // === Command Listener ===
    private final ObservableList<String[]> commandTable = FXCollections.observableArrayList();
    private final Map<String, Runnable> commandRegistry = new ConcurrentHashMap<>();

    // === Station State ===
    private final Map<String, Boolean> stationActive = new ConcurrentHashMap<>();
    private final Map<String, TextArea> stationOutputs = new ConcurrentHashMap<>();

    // === Ollama API ===
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Map<String, Boolean> ollamaAvailable = new ConcurrentHashMap<>();
    private double shannonEntropy = 0.0;
    private double entropyThreshold = 0.75;
    private final List<Double> entropyHistory = new ArrayList<>();

    // === Markov Patterns ===
    private final ObservableList<String[]> markovTable = FXCollections.observableArrayList();

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("⚙️ SIMS1337 - Unified Control Center");

        dashboardView = buildDashboard();
        gridView = buildGridView();
        settingsView = buildSettingsView();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a2e;");

        // === Navigation Bar ===
        HBox navBar = new HBox(10);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setStyle("-fx-background-color: #0f3460; -fx-padding: 10;");

        Button godHandBtn = navButton("🧠 GodHand", "#00d9ff", true);
        godHandBtn.setOnAction(e -> viewStack.getChildren().setAll(dashboardView));

        Button playerGridBtn = navButton("🎮 Player Grid", "#16213e", false);
        playerGridBtn.setOnAction(e -> viewStack.getChildren().setAll(gridView));

        Button settingsBtn = navButton("⚙️ Settings", "#16213e", false);
        settingsBtn.setOnAction(e -> viewStack.getChildren().setAll(settingsView));

        // Highlight active button
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

        stage.setScene(new Scene(root, 1400, 900));
        stage.show();

        log("✅ SIMS1337 v0.7.0 launched - Models, Chats, Orchestrators, Stations, Entropy, Markov");
        initCommandRegistry();
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
        VBox box = vbox(10, "#1a1a2e", 20);

        // Header
        HBox header = hbox(20, Pos.CENTER_LEFT, "#16213e", 15);
        Label title = label("🎮 GODHAND", 32, "#00d9ff", true);
        Label subtitle = label("Agent Orchestration Dashboard", 16, "#a0a0a0", false);
        Region hs = new Region(); HBox.setHgrow(hs, Priority.ALWAYS);
        header.getChildren().addAll(title, subtitle, hs);

        // === Model Pool with Chat Integration ===
        TitledPane modelPool = titledPane("🧠 MODEL POOL + CHAT ROUTING", true);
        VBox modelContent = vbox(10, "#16213e", 10);

        String[][] models = {
            {"⚡ FAST", "qwen2.5:0.5b", "398MB | <100ms", "#00ff88"},
            {"⚖️ BALANCED", "tinyllama:1.1b", "638MB | ~500ms", "#ffaa00"},
            {"🧠 REASONING", "phi:latest", "1.6GB | ~2-5s", "#ff6b6b"},
            {"🎯 DEEP", "phi3:mini", "2.2GB | ~5-10s", "#c77dff"}
        };

        for (String[] m : models) {
            VBox modelCard = vbox(5, "#0f3460", 10);
            modelCard.setStyle("-fx-background-color: #0f3460; -fx-padding: 10; -fx-background-radius: 5;");

            HBox modelHeader = hbox(10, Pos.CENTER_LEFT, null, 0);
            Label tierLabel = label(m[0], 14, m[3], true);
            Label nameLabel = label(m[1], 12, "#ffffff", false);
            Label sizeLabel = label(m[2], 10, "#a0a0a0", false);
            Label warmLabel = label("🟢 Warm", 11, "#00ff88", false);
            modelHeader.getChildren().addAll(tierLabel, nameLabel, sizeLabel, warmLabel);

            // Chat routing pattern selector
            HBox routingRow = hbox(8, Pos.CENTER_LEFT, null, 0);
            Label routingLabel = label("Pattern:", 10, "#a0a0a0", false);
            ComboBox<String> patternBox = new ComboBox<>();
            patternBox.getItems().addAll("Linear", "Loop", "Random", "Markov", "Vote", "Chain", "Broadcast");
            patternBox.setValue("Linear");
            patternBox.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 10px;");
            patternBox.setMaxWidth(100);
            modelPatterns.put(m[1], patternBox);

            // Web search button
            Button webSearchBtn = new Button("🌐 Search");
            webSearchBtn.setStyle("-fx-background-color: #6e5494; -fx-text-fill: #ffffff; -fx-font-size: 10px; -fx-padding: 2 8;");
            webSearchBtn.setOnAction(e -> {
                String query = modelInputs.get(m[1]).getText();
                if (!query.isEmpty()) {
                    log("🔍 [" + m[1] + "] Web search: " + query);
                    simulateModelResponse(m[1], "Web results for: " + query);
                }
            });

            routingRow.getChildren().addAll(routingLabel, patternBox, webSearchBtn);

            // Chat area
            TextArea chatArea = new TextArea();
            chatArea.setEditable(false);
            chatArea.setPrefRowCount(4);
            chatArea.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 10px;");
            chatArea.setText("[" + m[1] + "] Ready. Pattern: Linear\n");
            modelChats.put(m[1], chatArea);

            // Input row
            HBox inputRow = hbox(5, Pos.CENTER_LEFT, null, 0);
            TextField inputField = new TextField();
            inputField.setPromptText("Send to " + m[1] + "...");
            inputField.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 10px;");
            HBox.setHgrow(inputField, Priority.ALWAYS);
            modelInputs.put(m[1], inputField);

            Button sendBtn = new Button("▶");
            sendBtn.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #000000; -fx-font-size: 10px; -fx-padding: 2 10;");
            String modelName = m[1];
            sendBtn.setOnAction(e -> {
                String msg = inputField.getText();
                if (!msg.isEmpty()) {
                    chatArea.appendText("You: " + msg + "\n");
                    inputField.clear();
                    simulateModelResponse(modelName, msg);
                }
            });

            inputRow.getChildren().addAll(inputField, sendBtn);
            modelCard.getChildren().addAll(modelHeader, routingRow, chatArea, inputRow);
            modelContent.getChildren().add(modelCard);
        }
        modelPool.setContent(new ScrollPane(modelContent));

        // LoRA Adapters
        TitledPane loraPane = titledPane("🔄 LORA ADAPTERS", true);
        FlowPane loraFlow = new FlowPane(10, 10);
        String[] adapters = {"💬 CHAT", "💻 CODE", "🗺️ PATHFIND", "❤️ MOTIVES", "🎯 CAREER", "🔍 ANALYSIS"};
        String[] colors = {"#00ff88", "#00d9ff", "#ffaa00", "#ff6b6b", "#c77dff", "#a0a0a0"};
        for (int i = 0; i < adapters.length; i++) {
            Label al = new Label(adapters[i]);
            al.setStyle("-fx-background-color: " + colors[i] + "; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            loraFlow.getChildren().add(al);
        }
        loraPane.setContent(loraFlow);

        // Task Queue
        TitledPane queuePane = titledPane("📋 TASK QUEUE", true);
        VBox qc = vbox(10, "#16213e", 0);
        HBox qs = hbox(20, Pos.CENTER_LEFT, null, 0);
        qs.getChildren().addAll(label("Queue: 0/100", 12, "#00d9ff", true), new Region(), label("Util: 0%", 12, "#00ff88", true));
        HBox.setHgrow(qs.getChildren().get(1), Priority.ALWAYS);
        ProgressBar pb = new ProgressBar(0); pb.setMaxWidth(Double.MAX_VALUE);
        ListView<String> tl = new ListView<>(); tl.setPrefHeight(80); tl.setStyle("-fx-background-color: #0f3460;");
        qc.getChildren().addAll(qs, pb, tl);
        queuePane.setContent(qc);

        // Controls
        HBox controls = hbox(10, Pos.CENTER, null, 10);
        Button[] ctrlBtns = {
            styledButton("▶️ Start Agent", "#00ff88"),
            styledButton("⏸️ Pause", "#ffaa00"),
            styledButton("⏹️ Stop", "#ff6b6b"),
            styledButton("🔄 Switch Adapter", "#00d9ff"),
            styledButton("📊 Refresh", "#c77dff")
        };
        controls.getChildren().addAll(ctrlBtns);

        // Log
        TitledPane logPane = titledPane("📜 ACTIVITY LOG", true);
        logConsole = new TextArea();
        logConsole.setEditable(false); logConsole.setWrapText(true); logConsole.setPrefHeight(150);
        logConsole.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 11px;");
        logPane.setContent(logConsole);

        box.getChildren().addAll(header, modelPool, loraPane, queuePane, controls, logPane);
        return box;
    }

    // ==================== PLAYER GRID VIEW ====================
    private VBox buildGridView() {
        VBox box = vbox(15, "#1a1a2e", 20);
        box.setAlignment(Pos.TOP_CENTER);

        box.getChildren().addAll(
            label("🎮 PLAYER GRID 3D - 10x10x5", 24, "#00d9ff", true),
            label("👇 10x10 GRID - Blue gradient with cyan borders 👇", 16, "#00ff88", false)
        );

        GridPane grid = new GridPane();
        grid.setHgap(5); grid.setVgap(5); grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: #1a1a3e; -fx-padding: 20; -fx-border-color: #00ff88; -fx-border-width: 3;");

        for (int row = 0; row < 10; row++)
            for (int col = 0; col < 10; col++) {
                Rectangle cell = new Rectangle(50, 50);
                cell.setFill(Color.rgb(30 + row * 15, 80 + col * 12, 150 + (10 - row) * 8));
                cell.setStroke(Color.web("#00d9ff"));
                cell.setStrokeWidth(3);
                grid.add(cell, col, row);
            }

        box.getChildren().addAll(grid, label("👆 100 BLUE CELLS (10x10) WITH CYAN BORDERS 👆", 16, "#ffffff", false));

        // Player positions
        VBox pp = vbox(10, "#16213e", 15);
        pp.setStyle("-fx-background-radius: 10;");
        pp.getChildren().add(label("🎮 PLAYER POSITIONS (XYZ)", 16, "#00d9ff", true));
        String[][] players = {{"🟢 Agent Alpha", "(3,7,2)", "Active"}, {"🔵 Agent Beta", "(8,4,1)", "Active"}, {"🟠 Agent Gamma", "(5,9,3)", "Active"}};
        for (String[] p : players) {
            HBox pr = hbox(15, Pos.CENTER_LEFT, null, 0);
            pr.getChildren().addAll(label(p[0], 12, "#ffffff", true), label(p[1], 12, "#a0a0a0", false), label(p[2], 12, "#00ff88", false));
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
            String stationName = s[0].substring(2).trim();
            sb.setOnAction(e -> triggerStation(stationName));
            stationRow.getChildren().add(sb);
        }
        box.getChildren().add(stationRow);

        return box;
    }

    // ==================== SETTINGS VIEW ====================
    private VBox buildSettingsView() {
        VBox box = vbox(10, "#1a1a2e", 20);

        box.getChildren().add(label("⚙️ SETTINGS & ORCHESTRATION", 24, "#00d9ff", true));

        // === Command Listener Table ===
        TitledPane cmdPane = titledPane("📋 COMMAND LISTENER (Preset → Terminal Trigger)", true);
        VBox cmdContent = vbox(10, "#16213e", 10);

        TableView<String[]> cmdTableView = new TableView<>();
        cmdTableView.setPrefHeight(150);
        cmdTableView.setStyle("-fx-background-color: #0f3460;");

        TableColumn<String[], String> triggerCol = new TableColumn<>("Trigger Phrase");
        triggerCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        triggerCol.setPrefWidth(200);

        TableColumn<String[], String> commandCol = new TableColumn<>("Command");
        commandCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        commandCol.setPrefWidth(300);

        TableColumn<String[], String> stationCol = new TableColumn<>("Station");
        stationCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        stationCol.setPrefWidth(120);

        TableColumn<String[], String> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        activeCol.setPrefWidth(60);

        cmdTableView.getColumns().addAll(triggerCol, commandCol, stationCol, activeCol);
        cmdTableView.setItems(commandTable);

        // Default commands
        commandTable.addAll(
            new String[]{"just enter this in terminal", "execute $INPUT", "Brute Foundry", "✅"},
            new String[]{"fix this bug", "analyze + patch $FILE", "Hospital", "✅"},
            new String[]{"push to github", "git add -A && git commit -m '$MSG' && git push", "GitHub", "✅"},
            new String[]{"search the web for", "web_search $QUERY", "Research", "✅"},
            new String[]{"optimize this code", "refactor $FILE for performance", "Brute Foundry", "✅"}
        );

        HBox cmdButtons = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button addCmdBtn = styledButton("➕ Add Command", "#00ff88");
        addCmdBtn.setOnAction(e -> commandTable.add(new String[]{"new trigger", "command here", "Station", "✅"}));
        Button delCmdBtn = styledButton("🗑️ Delete Selected", "#ff6b6b");
        delCmdBtn.setOnAction(e -> { String[] sel = cmdTableView.getSelectionModel().getSelectedItem(); if (sel != null) commandTable.remove(sel); });
        cmdButtons.getChildren().addAll(addCmdBtn, delCmdBtn);
        cmdContent.getChildren().addAll(cmdTableView, cmdButtons);
        cmdPane.setContent(cmdContent);

        // === Prompt Injection Menu ===
        TitledPane promptPane = titledPane("💉 PROMPT INJECTION", true);
        VBox promptContent = vbox(10, "#16213e", 10);

        TextArea systemPrompt = new TextArea(
            "You are an SLM agent in the SIMS1337 grid. You collaborate with other agents to build software, " +
            "vote on code changes, and maintain the system. You have access to terminal commands, web search, " +
            "and file operations. Always respond with actionable output."
        );
        systemPrompt.setPrefRowCount(4);
        systemPrompt.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 11px;");

        HBox promptButtons = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button injectAllBtn = styledButton("💉 Inject to All Models", "#c77dff");
        injectAllBtn.setOnAction(e -> {
            String prompt = systemPrompt.getText();
            modelChats.forEach((name, chat) -> chat.appendText("[SYSTEM PROMPT INJECTED] " + prompt.substring(0, 50) + "...\n"));
            log("💉 System prompt injected to all " + modelChats.size() + " models");
        });
        Button injectOneBtn = styledButton("💉 Inject to Selected", "#00d9ff");
        promptButtons.getChildren().addAll(injectAllBtn, injectOneBtn);
        promptContent.getChildren().addAll(systemPrompt, promptButtons);
        promptPane.setContent(promptContent);

        // === Model Context Options ===
        TitledPane contextPane = titledPane("🔧 MODEL CONTEXT OPTIONS", true);
        VBox contextContent = vbox(10, "#16213e", 10);

        String[][] contextOptions = {
            {"Max Tokens", "2048", "4096", "8192", "16384"},
            {"Temperature", "0.1", "0.5", "0.7", "1.0"},
            {"LoRA Adapter", "CHAT", "CODE", "PATHFIND", "ANALYSIS"},
            {"KV Cache Size", "512", "1024", "2048", "4096"},
            {"KG Node Depth", "1", "2", "3", "5"},
            {"Affine Scale", "0.5x", "1.0x", "1.5x", "2.0x"}
        };

        for (String[] opt : contextOptions) {
            HBox row = hbox(10, Pos.CENTER_LEFT, null, 0);
            row.getChildren().add(label(opt[0] + ":", 12, "#ffffff", false));
            ComboBox<String> cb = new ComboBox<>();
            cb.getItems().addAll(opt[1], opt[2], opt[3], opt[4]);
            cb.setValue(opt[1]);
            cb.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 11px;");
            cb.setMaxWidth(120);
            row.getChildren().add(cb);
            contextContent.getChildren().add(row);
        }
        contextPane.setContent(contextContent);

        // === Shannon Entropy Monitor ===
        TitledPane entropyPane = titledPane("📊 SHANNON ENTROPY MONITOR", true);
        VBox entropyContent = vbox(10, "#16213e", 10);

        HBox entropyRow = hbox(20, Pos.CENTER_LEFT, null, 0);
        Label entropyValue = label("Current: 0.000 bits", 14, "#00d9ff", true);
        Label entropyStatus = label("🟢 Normal", 14, "#00ff88", true);
        entropyRow.getChildren().addAll(entropyValue, entropyStatus);

        HBox thresholdRow = hbox(10, Pos.CENTER_LEFT, null, 0);
        thresholdRow.getChildren().add(label("Alert Threshold:", 12, "#ffffff", false));
        TextField thresholdField = new TextField("0.75");
        thresholdField.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #ffffff; -fx-font-size: 11px;");
        thresholdField.setMaxWidth(60);
        thresholdField.setOnAction(e -> {
            try { entropyThreshold = Double.parseDouble(thresholdField.getText()); log("📊 Entropy threshold set to: " + entropyThreshold); }
            catch (NumberFormatException ex) { log("❌ Invalid threshold"); }
        });
        thresholdRow.getChildren().addAll(thresholdField, label("(> threshold triggers alert)", 10, "#a0a0a0", false));

        entropyContent.getChildren().addAll(entropyRow, thresholdRow);

        // Update entropy periodically
        ScheduledExecutorService entropyUpdater = Executors.newSingleThreadScheduledExecutor();
        entropyUpdater.scheduleAtFixedRate(() -> {
            double newEntropy = Math.random() * 0.5 + 0.2; // Simulated
            shannonEntropy = newEntropy;
            entropyHistory.add(newEntropy);
            if (entropyHistory.size() > 100) entropyHistory.remove(0);
            Platform.runLater(() -> {
                entropyValue.setText(String.format("Current: %.3f bits", newEntropy));
                if (newEntropy > entropyThreshold) {
                    entropyStatus.setText("🔴 ALERT - Entropy breach!");
                    entropyStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                    log("🚨 SHANNON ENTROPY BREACH: " + String.format("%.3f", newEntropy) + " > " + entropyThreshold);
                } else {
                    entropyStatus.setText("🟢 Normal");
                    entropyStatus.setStyle("-fx-font-size: 14px; -fx-text-fill: #00ff88;");
                }
            });
        }, 0, 3, TimeUnit.SECONDS);
        entropyPane.setContent(entropyContent);

        // === Markov Chain Patterns ===
        TitledPane markovPane = titledPane("🔗 MARKOV CHAIN PATTERNS", true);
        VBox markovContent = vbox(10, "#16213e", 10);

        TableView<String[]> markovTableView = new TableView<>();
        markovTableView.setPrefHeight(120);
        markovTableView.setStyle("-fx-background-color: #0f3460;");

        TableColumn<String[], String> fromCol = new TableColumn<>("From State");
        fromCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[], String> toCol = new TableColumn<>("To State");
        toCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[], String> probCol = new TableColumn<>("Probability");
        probCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[], String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));

        markovTableView.getColumns().addAll(fromCol, toCol, probCol, actionCol);
        markovTableView.setItems(markovTable);

        markovTable.addAll(
            new String[]{"Observation", "Analysis", "0.8", "analyze_input"},
            new String[]{"Analysis", "Synthesis", "0.7", "synthesize_findings"},
            new String[]{"Synthesis", "Vote", "0.9", "cast_vote"},
            new String[]{"Vote", "Deploy", "0.6", "deploy_changes"},
            new String[]{"Deploy", "Observation", "0.5", "monitor_results"}
        );

        HBox markovButtons = hbox(10, Pos.CENTER_LEFT, null, 0);
        markovButtons.getChildren().addAll(
            styledButton("➕ Add Pattern", "#00ff88"),
            styledButton("🗑️ Delete", "#ff6b6b"),
            styledButton("🔄 Reset Chain", "#ffaa00")
        );
        markovContent.getChildren().addAll(markovTableView, markovButtons);
        markovPane.setContent(markovContent);

        // === Lexical Math English ===
        TitledPane lexicalPane = titledPane("📐 LEXICAL MATH ENGLISH (NLP)", true);
        VBox lexicalContent = vbox(10, "#16213e", 10);

        TextArea lexicalInput = new TextArea("sum of (agent_count * task_complexity) / time_elapsed");
        lexicalInput.setPrefRowCount(3);
        lexicalInput.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 11px;");

        HBox lexicalButtons = hbox(10, Pos.CENTER_LEFT, null, 0);
        Button parseBtn = styledButton("🔢 Parse Expression", "#00d9ff");
        parseBtn.setOnAction(e -> {
            String expr = lexicalInput.getText();
            log("📐 Lexical parse: " + expr + " → " + evaluateLexical(expr));
        });
        lexicalButtons.getChildren().addAll(parseBtn, styledButton("📋 Save Expression", "#c77dff"));
        lexicalContent.getChildren().addAll(lexicalInput, lexicalButtons);
        lexicalPane.setContent(lexicalContent);

        box.getChildren().addAll(cmdPane, promptPane, contextPane, entropyPane, markovPane, lexicalPane);
        return box;
    }

    // ==================== STATION ORCHESTRATION ====================
    private void triggerStation(String station) {
        stationActive.putIfAbsent(station, false);
        boolean active = !stationActive.get(station);
        stationActive.put(station, active);

        if (active) {
            log("🏗️ [" + station + "] ACTIVATED");
            switch (station) {
                case "Brute Foundry" -> simulateBruteFoundry();
                case "Hospital" -> simulateHospital();
                case "GitHub" -> simulateGitHubStation();
                case "A/B Lab" -> log("🧬 A/B Lab: Comparing model outputs...");
                case "Knowledge Tree" -> log("🌳 Knowledge Tree: Building graph...");
                case "Research" -> log("🔬 Research: Crawling web...");
                case "Secrets" -> log("🔒 Secrets: Encrypting storage...");
            }
        } else {
            log("⏹️ [" + station + "] DEACTIVATED");
        }
    }

    private void simulateBruteFoundry() {
        chatScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                String[] actions = {"Compiling...", "Running tests...", "Generating code...", "Optimizing...", "Building artifacts..."};
                String action = actions[new Random().nextInt(actions.length)];
                log("🏗️ Brute Foundry: " + action);
                modelChats.values().forEach(chat -> chat.appendText("[Brute Foundry] " + action + "\n"));
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void simulateHospital() {
        chatScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                String[] actions = {"Checking agent health...", "Recovering agent state...", "Restarting failed agent...", "Memory cleanup..."};
                log("🏥 Hospital: " + actions[new Random().nextInt(actions.length)]);
            });
        }, 0, 8, TimeUnit.SECONDS);
    }

    private void simulateGitHubStation() {
        chatScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                log("📡 GitHub: Syncing repos, checking PRs, updating issues...");
            });
        }, 0, 10, TimeUnit.SECONDS);
    }

    // ==================== MODEL CHAT (REAL OLLAMA API) ====================
    private void simulateModelResponse(String modelName, String input) {
        chatScheduler.schedule(() -> {
            try {
                String response = callOllama(modelName, input);
                Platform.runLater(() -> {
                    TextArea chat = modelChats.get(modelName);
                    if (chat != null) {
                        chat.appendText("[" + modelName + "] " + response + "\n");
                    }
                    log("💬 [" + modelName + "] " + response);
                    checkCommandTriggers(response, modelName);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    TextArea chat = modelChats.get(modelName);
                    if (chat != null) {
                        chat.appendText("[" + modelName + "] ⚠️ API unavailable: " + e.getMessage() + "\n");
                    }
                    log("⚠️ [" + modelName + "] Ollama API error: " + e.getMessage());
                });
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    private String callOllama(String model, String prompt) throws Exception {
        String json = String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                model.replace("\"", "\\\""),
                prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ollamaAvailable.put(model, true);
            // Extract just the response text from Ollama JSON
            String body = response.body();
            int start = body.indexOf("\"response\":\"");
            if (start > 0) {
                start += 12;
                int end = body.indexOf("\"", start);
                if (end > start) {
                    return body.substring(start, end)
                            .replace("\\n", " ")
                            .replace("\\\"", "\"");
                }
            }
            return body.length() > 200 ? body.substring(0, 200) + "..." : body;
        } else {
            ollamaAvailable.put(model, false);
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body().substring(0, Math.min(100, response.body().length())));
        }
    }

    // ==================== COMMAND SYSTEM ====================
    private void initCommandRegistry() {
        commandRegistry.put("execute", () -> log("⚡ EXECUTING terminal command..."));
        commandRegistry.put("analyze", () -> log("🔍 ANALYZING code for issues..."));
        commandRegistry.put("git_push", () -> log("📡 PUSHING to GitHub..."));
        commandRegistry.put("web_search", () -> log("🌐 SEARCHING web..."));
        commandRegistry.put("refactor", () -> log("🔧 REFACTORING code..."));
    }

    private void checkCommandTriggers(String input, String modelName) {
        for (String[] cmd : commandTable) {
            if (cmd[3].equals("✅") && input.toLowerCase().contains(cmd[0].toLowerCase())) {
                log("🎯 COMMAND TRIGGERED: [" + modelName + "] → " + cmd[0] + " → " + cmd[1]);
                String station = cmd[2];
                if (!station.equals("Station")) {
                    triggerStation(station);
                }
            }
        }
    }

    // ==================== ENTROPY ====================
    private void startEntropyMonitor() {
        chatScheduler.scheduleAtFixedRate(() -> {
            double entropy = calculateEntropy();
            if (entropy > entropyThreshold) {
                Platform.runLater(() -> {
                    log("🚨 ENTROPY ALERT: " + String.format("%.3f", entropy) + " > threshold " + entropyThreshold);
                    statusLabel.setText("🔴 Entropy Alert!");
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                });
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private double calculateEntropy() {
        // Simplified Shannon entropy from chat activity
        double total = modelChats.values().stream().mapToInt(c -> c.getText().length()).sum();
        if (total == 0) return 0;
        double entropy = 0;
        for (TextArea chat : modelChats.values()) {
            double p = chat.getText().length() / total;
            if (p > 0) entropy -= p * Math.log(p) / Math.log(2);
        }
        return Math.min(1.0, entropy);
    }

    // ==================== LEXICAL MATH ====================
    private String evaluateLexical(String expr) {
        // Simple lexical math evaluator
        Map<String, Double> vars = new HashMap<>();
        vars.put("agent_count", 3.0);
        vars.put("task_complexity", 2.5);
        vars.put("time_elapsed", 10.0);
        vars.put("entropy", shannonEntropy);

        try {
            expr = expr.toLowerCase();
            for (Map.Entry<String, Double> e : vars.entrySet())
                expr = expr.replace(e.getKey(), String.valueOf(e.getValue()));

            // Simple evaluation: sum of (a * b) / c
            if (expr.contains("sum of") && expr.contains("/")) {
                String inner = expr.replace("sum of", "").replace("/", " ").trim();
                String[] parts = inner.split("\\s+");
                double result = 1;
                for (String p : parts) {
                    try { result *= Double.parseDouble(p.replace("(", "").replace(")", "")); }
                    catch (NumberFormatException ignored) {}
                }
                if (inner.contains("/")) {
                    String[] div = inner.split("/");
                    double num = 1, den = 1;
                    for (String p : div[0].trim().split("\\s*\\*\\s*")) {
                        try { num *= Double.parseDouble(p.replace("(", "").replace(")", "")); }
                        catch (NumberFormatException ignored) {}
                    }
                    try { den = Double.parseDouble(div[1].trim()); } catch (NumberFormatException ignored) {}
                    result = den != 0 ? num / den : 0;
                }
                return String.format("%.2f", result);
            }
        } catch (Exception e) {
            return "Parse error: " + e.getMessage();
        }
        return "?";
    }

    // ==================== UTILITY METHODS ====================
    private void log(String msg) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = "[" + timestamp + "] " + msg;
        System.out.println(entry);
        if (logConsole != null) {
            Platform.runLater(() -> {
                logConsole.appendText(entry + "\n");
                // Keep last 500 lines
                String[] lines = logConsole.getText().split("\n");
                if (lines.length > 500) {
                    logConsole.setText(String.join("\n", Arrays.copyOfRange(lines, lines.length - 500, lines.length)));
                }
            });
        }
    }

    private VBox vbox(int spacing, String bg, int padding) {
        VBox box = new VBox(spacing);
        box.setStyle("-fx-background-color: " + bg + "; -fx-padding: " + padding + ";");
        return box;
    }

    private HBox hbox(int spacing, Pos align, String bg, int padding) {
        HBox box = new HBox(spacing);
        box.setAlignment(align);
        if (bg != null) box.setStyle("-fx-background-color: " + bg + "; -fx-padding: " + padding + ";");
        return box;
    }

    private Label label(String text, int size, String color, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "px; -fx-text-fill: " + color + ";" + (bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }

    private TitledPane titledPane(String title, boolean expanded) {
        TitledPane tp = new TitledPane();
        tp.setText(title);
        tp.setExpanded(expanded);
        tp.setStyle("-fx-background-color: #16213e;");
        return tp;
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-padding: 10 20;");
        return btn;
    }

    @Override
    public void stop() {
        chatScheduler.shutdown();
        log("⏹️ SIMS1337 shutting down...");
    }
}
