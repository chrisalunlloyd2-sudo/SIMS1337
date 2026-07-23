package com.aigen.sims;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Self-contained GodHand + Player Grid - NO FXML, NO controller binding issues.
 * Everything built programmatically. View switching via StackPane child swap.
 */
public class GodHandApp extends Application {

    private StackPane viewStack;
    private VBox dashboardView;
    private VBox gridView;
    private Label statusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("GodHand - Agent Orchestration Dashboard");

        // === Build Dashboard View ===
        dashboardView = buildDashboard();

        // === Build Grid View ===
        gridView = buildGridView();

        // === Root: Nav bar + StackPane ===
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1a2e;");

        // Navigation bar
        HBox navBar = new HBox(10);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setStyle("-fx-background-color: #0f3460; -fx-padding: 10;");

        Button godHandBtn = new Button("🧠 GodHand");
        godHandBtn.setStyle("-fx-background-color: #00d9ff; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 14px;");
        godHandBtn.setOnAction(e -> viewStack.getChildren().setAll(dashboardView));

        Button playerGridBtn = new Button("🎮 Player Grid");
        playerGridBtn.setStyle("-fx-background-color: #16213e; -fx-text-fill: #ffffff; -fx-font-size: 14px;");
        playerGridBtn.setOnAction(e -> viewStack.getChildren().setAll(gridView));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("🟢 System Ready");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00ff88;");

        navBar.getChildren().addAll(godHandBtn, playerGridBtn, spacer, statusLabel);

        // View stack
        viewStack = new StackPane(dashboardView);
        viewStack.setStyle("-fx-background-color: #1a1a2e;");

        root.getChildren().addAll(navBar, viewStack);
        VBox.setVgrow(viewStack, Priority.ALWAYS);

        stage.setScene(new Scene(root, 1200, 800));
        stage.show();

        System.out.println("✅ GodHandApp launched - NO FXML, pure JavaFX");
    }

    private VBox buildDashboard() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20;");

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #16213e; -fx-padding: 15;");

        Label title = new Label("🎮 GODHAND");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #00d9ff;");

        Label subtitle = new Label("Agent Orchestration Dashboard");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #a0a0a0;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        header.getChildren().addAll(title, subtitle, hSpacer);

        // Model Pool
        TitledPane modelPool = new TitledPane();
        modelPool.setText("🧠 MODEL POOL");
        modelPool.setExpanded(true);
        modelPool.setStyle("-fx-background-color: #16213e;");

        GridPane modelGrid = new GridPane();
        modelGrid.setHgap(20);
        modelGrid.setVgap(10);

        String[][] models = {
            {"⚡ FAST TIER", "qwen2.5:0.5b", "398MB | <100ms", "🟢 Warm", "#00ff88"},
            {"⚖️ BALANCED", "tinyllama:1.1b", "638MB | ~500ms", "🟢 Warm", "#ffaa00"},
            {"🧠 REASONING", "phi:latest", "1.6GB | ~2-5s", "🟢 Warm", "#ff6b6b"},
            {"🎯 DEEP", "phi3:mini", "2.2GB | ~5-10s", "🟢 Warm", "#c77dff"}
        };

        for (int i = 0; i < models.length; i++) {
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color: #0f3460; -fx-padding: 15;");

            Label tierLabel = new Label(models[i][0]);
            tierLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + models[i][4] + ";");

            Label nameLabel = new Label(models[i][1]);
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff;");

            Label sizeLabel = new Label(models[i][2]);
            sizeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0a0a0;");

            Label statusLabel = new Label(models[i][3]);
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00ff88;");

            card.getChildren().addAll(tierLabel, nameLabel, sizeLabel, statusLabel);
            modelGrid.add(card, i, 0);
        }

        modelPool.setContent(modelGrid);

        // LoRA Adapters
        TitledPane loraPane = new TitledPane();
        loraPane.setText("🔄 LORA ADAPTERS");
        loraPane.setExpanded(true);
        loraPane.setStyle("-fx-background-color: #16213e;");

        FlowPane loraFlow = new FlowPane(10, 10);
        String[] adapters = {"💬 CHAT", "💻 CODE", "🗺️ PATHFIND", "❤️ MOTIVES", "🎯 CAREER", "🔍 ANALYSIS"};
        String[] adapterColors = {"#00ff88", "#00d9ff", "#ffaa00", "#ff6b6b", "#c77dff", "#a0a0a0"};

        for (int i = 0; i < adapters.length; i++) {
            Label adapterLabel = new Label(adapters[i]);
            adapterLabel.setStyle("-fx-background-color: " + adapterColors[i] +
                "; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            loraFlow.getChildren().add(adapterLabel);
        }

        loraPane.setContent(loraFlow);

        // Task Queue
        TitledPane queuePane = new TitledPane();
        queuePane.setText("📋 TASK QUEUE");
        queuePane.setExpanded(true);
        queuePane.setStyle("-fx-background-color: #16213e;");

        VBox queueContent = new VBox(10);
        HBox queueStats = new HBox(20);
        queueStats.setAlignment(Pos.CENTER_LEFT);

        Label queueSizeLabel = new Label("Queue Size:");
        queueSizeLabel.setStyle("-fx-text-fill: #ffffff;");
        Label queueSize = new Label("0/100");
        queueSize.setStyle("-fx-font-weight: bold; -fx-text-fill: #00d9ff;");

        Region qSpacer = new Region();
        HBox.setHgrow(qSpacer, Priority.ALWAYS);

        Label utilLabel = new Label("Utilization:");
        utilLabel.setStyle("-fx-text-fill: #ffffff;");
        Label utilValue = new Label("0%");
        utilValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #00ff88;");

        queueStats.getChildren().addAll(queueSizeLabel, queueSize, qSpacer, utilLabel, utilValue);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        ListView<String> taskList = new ListView<>();
        taskList.setPrefHeight(150);
        taskList.setStyle("-fx-background-color: #0f3460;");

        queueContent.getChildren().addAll(queueStats, progressBar, taskList);
        queuePane.setContent(queueContent);

        // Controls
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-padding: 10;");

        Button startBtn = new Button("▶️ Start Agent");
        startBtn.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-padding: 10 20;");

        Button pauseBtn = new Button("⏸️ Pause");
        pauseBtn.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-padding: 10 20;");

        Button stopBtn = new Button("⏹️ Stop");
        stopBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-padding: 10 20;");

        Button switchBtn = new Button("🔄 Switch Adapter");
        switchBtn.setStyle("-fx-background-color: #00d9ff; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-padding: 10 20;");

        Button refreshBtn = new Button("📊 Refresh Stats");
        refreshBtn.setStyle("-fx-background-color: #c77dff; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-padding: 10 20;");

        controls.getChildren().addAll(startBtn, pauseBtn, stopBtn, switchBtn, refreshBtn);

        // Log
        TitledPane logPane = new TitledPane();
        logPane.setText("📜 ACTIVITY LOG");
        logPane.setExpanded(true);
        logPane.setStyle("-fx-background-color: #16213e;");

        TextArea logConsole = new TextArea();
        logConsole.setEditable(false);
        logConsole.setWrapText(true);
        logConsole.setPrefHeight(200);
        logConsole.setStyle("-fx-background-color: #0a0a15; -fx-text-fill: #00ff88; -fx-font-family: monospace; -fx-font-size: 11px;");
        logConsole.setText("🟢 System Ready\n✅ Model Pool initialized\n✅ LoRA Adapters loaded\n✅ Task Queue ready\n");

        logPane.setContent(logConsole);

        box.getChildren().addAll(header, modelPool, loraPane, queuePane, controls, logPane);
        return box;
    }

    private VBox buildGridView() {
        VBox box = new VBox(15);
        box.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20;");
        box.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("🎮 PLAYER GRID 3D - 10x10x5");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #00d9ff;");

        Label gridLabel = new Label("👇 10x10 GRID - Blue gradient with cyan borders 👇");
        gridLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #00ff88;");

        // Build 10x10 grid
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: #1a1a3e; -fx-padding: 20; -fx-border-color: #00ff88; -fx-border-width: 3;");

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Rectangle cell = new Rectangle(50, 50);
                int r = 30 + (row * 15);
                int g = 80 + (col * 12);
                int b = 150 + ((10 - row) * 8);
                cell.setFill(Color.rgb(r, g, b));
                cell.setStroke(Color.web("#00d9ff"));
                cell.setStrokeWidth(3);
                grid.add(cell, col, row);
            }
        }

        Label bottomLabel = new Label("👆 100 BLUE CELLS (10x10) WITH CYAN BORDERS 👆");
        bottomLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff;");

        // Player positions
        VBox playerPanel = new VBox(10);
        playerPanel.setStyle("-fx-background-color: #16213e; -fx-padding: 15; -fx-background-radius: 10;");

        Label playerTitle = new Label("🎮 PLAYER POSITIONS (XYZ)");
        playerTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00d9ff;");

        String[][] players = {
            {"🟢 Agent Alpha", "(3, 7, 2)", "Active"},
            {"🔵 Agent Beta", "(8, 4, 1)", "Active"},
            {"🟠 Agent Gamma", "(5, 9, 3)", "Active"}
        };

        for (String[] player : players) {
            HBox playerRow = new HBox(15);
            playerRow.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(player[0]);
            nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");

            Label posLabel = new Label(player[1]);
            posLabel.setStyle("-fx-text-fill: #a0a0a0;");

            Label statusLabel = new Label(player[2]);
            statusLabel.setStyle("-fx-text-fill: #00ff88;");

            playerRow.getChildren().addAll(nameLabel, posLabel, statusLabel);
            playerPanel.getChildren().add(playerRow);
        }

        playerPanel.getChildren().add(0, playerTitle);

        box.getChildren().addAll(titleLabel, gridLabel, grid, bottomLabel, playerPanel);
        return box;
    }
}
