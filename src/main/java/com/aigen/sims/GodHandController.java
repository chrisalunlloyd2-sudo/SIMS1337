package com.aigen.sims;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import com.aigen.sims.routing.*;
import com.aigen.sims.tasks.Task;
import com.aigen.sims.tasks.Complexity;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GodHand Controller - Connects GUI to routing system
 */
public class GodHandController {
    private static final String GODHAND_FXML = "/fxml/GodHand.fxml";
    private Scene mainScene;
    private static final Logger logger = LoggerFactory.getLogger(GodHandController.class);
    
    // Model status labels
    @FXML private Label statusLabel;
    @FXML private Label fastModelName, fastModelStatus;
    @FXML private Label balancedModelName, balancedModelStatus;
    @FXML private Label reasoningModelName, reasoningModelStatus;
    @FXML private Label deepModelName, deepModelStatus;
    
    // LoRA adapters
    @FXML private Label adapterChat, adapterCode, adapterPathfind;
    @FXML private Label adapterMotives, adapterCareer, adapterAnalysis;
    
    // Task queue
    @FXML private Label queueSize, queueUtilization;
    @FXML private ProgressBar queueProgress;
    @FXML private ListView<String> taskListView;
    
    // Controls
    @FXML private Button startButton, pauseButton, stopButton;
    
    // Log console
    @FXML private TextArea logConsole;
    
    // Core routing components
    private ModelPool modelPool;
    private ModelRouter modelRouter;
    private TaskQueue taskQueue;
    private LoRASwitcher loraSwitcher;
    
    private int adapterIndex = 0;
    private final String[] adapterNames = {"CHAT", "CODE", "PATHFIND", "MOTIVES", "CAREER", "ANALYSIS"};
    
    @FXML
    public void initialize() {
        log("GodHand interface initialized");
        log("Connecting to routing system...");
        
        // Get shared instances from MainApp
        modelPool = MainApp.modelPool;
        modelRouter = MainApp.modelRouter;
        taskQueue = MainApp.taskQueue;
        loraSwitcher = MainApp.loraSwitcher;
        
        if (modelPool != null) {
            log("✓ ModelPool connected (" + modelPool.getModelCount() + " models)");
            updateModelStatus();
        } else {
            log("⚠ ModelPool not initialized");
        }
        
        if (loraSwitcher != null) {
            log("✓ LoRA Switcher connected (" + loraSwitcher.getAvailableAdapters().length + " adapters)");
            updateAdapterStatus();
        }
        
        updateQueueStats();
        
        log("GodHand ready");
    }
    
    @FXML
    private void startAgent() {
        log("▶️ Starting agent...");
        statusLabel.setText("🟢 Running");
        
        // Demo: Add a test task
        Task task = new Task("demo_task", Complexity.MEDIUM);
        if (taskQueue.enqueue(task)) {
            log("✓ Task enqueued: " + task.getName());
            updateQueueStats();
        }
    }
    
    @FXML
    private void pauseAgent() {
        log("⏸️ Pausing agent...");
        statusLabel.setText("🟡 Paused");
    }
    
    @FXML
    private void stopAgent() {
        log("⏹️ Stopping agent...");
        statusLabel.setText("🔴 Stopped");
    }
    
    @FXML
    private void switchAdapter() {
        adapterIndex = (adapterIndex + 1) % adapterNames.length;
        String adapterName = adapterNames[adapterIndex];
        
        log("🔄 Switching to " + adapterName + " adapter...");
        
        // Highlight selected adapter
        updateAdapterStatus();
        
        log("✓ Adapter switched: " + adapterName);
    }
    
    @FXML
    private void refreshStats() {
        log("📊 Refreshing statistics...");
        updateModelStatus();
        updateAdapterStatus();
        updateQueueStats();
        log("✓ Statistics refreshed");
    }
    
    private void updateModelStatus() {
        if (modelPool == null) return;
        
        Platform.runLater(() -> {
            try {
                var fast = modelPool.getQwen2_0_5b();
                var balanced = modelPool.getTinyllama_1_1b();
                var reasoning = modelPool.getPhiLatest();
                var deep = modelPool.getPhi3Mini();
                
                fastModelStatus.setText(fast != null && fast.isWarm() ? "🟢 Warm" : "🟡 Warming...");
                balancedModelStatus.setText(balanced != null && balanced.isWarm() ? "🟢 Warm" : "🟡 Warming...");
                reasoningModelStatus.setText(reasoning != null && reasoning.isWarm() ? "🟢 Warm" : "🟡 Warming...");
                deepModelStatus.setText(deep != null && deep.isWarm() ? "🟢 Warm" : "🟡 Warming...");
            } catch (Exception e) {
                logger.debug("Model status update (models still warming): {}", e.getMessage());
            }
        });
    }
    
    private void updateAdapterStatus() {
        Platform.runLater(() -> {
            // Reset all to default
            adapterChat.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            adapterCode.setStyle("-fx-background-color: #00d9ff; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            adapterPathfind.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            adapterMotives.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            adapterCareer.setStyle("-fx-background-color: #c77dff; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            adapterAnalysis.setStyle("-fx-background-color: #a0a0a0; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20;");
            
            // Highlight current
            Label current = getAdapterLabel(adapterIndex);
            if (current != null) {
                current.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #000000; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold;");
            }
        });
    }
    
    private Label getAdapterLabel(int index) {
        return switch (index) {
            case 0 -> adapterChat;
            case 1 -> adapterCode;
            case 2 -> adapterPathfind;
            case 3 -> adapterMotives;
            case 4 -> adapterCareer;
            case 5 -> adapterAnalysis;
            default -> null;
        };
    }
    
    private void updateQueueStats() {
        if (taskQueue == null) return;
        
        Platform.runLater(() -> {
            int currentSize = taskQueue.size();
            int maxCapacity = taskQueue.getCapacity();
            double utilization = (double) currentSize / maxCapacity;
            
            queueSize.setText(currentSize + "/" + maxCapacity);
            queueUtilization.setText(String.format("%.1f%%", utilization * 100));
            queueProgress.setProgress(utilization);
            
            // Update task list
            taskListView.getItems().clear();
            taskListView.getItems().add("Current size: " + currentSize);
            taskListView.getItems().add("Capacity: " + maxCapacity);
            taskListView.getItems().add("Utilization: " + String.format("%.1f%%", utilization * 100));
        });
    }
    
    private void log(String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            logConsole.appendText("[" + timestamp + "] " + message + "\n");
        });
    }

    @FXML
    public void showPlayerGridView() {
        logger.info("🎮 Player Grid button clicked - building 10x10 RED grid");
        try {
            VBox gridRoot = new VBox(15);
            gridRoot.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20;");
            
            Label titleLabel = new Label("🎮 PLAYER GRID 3D - 10x10x5");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #00d9ff;");
            
            Label gridLabel = new Label("👇 10x10 GRID - Blue gradient with cyan borders 👇");
            gridLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #00ff88;");
            
            GridPane grid = new GridPane();
            grid.setHgap(5);
            grid.setVgap(5);
            grid.setAlignment(javafx.geometry.Pos.CENTER);
            
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    Rectangle cell = new Rectangle(50, 50);
                    // Blue gradient based on position
javafx.scene.paint.Color cellColor = javafx.scene.paint.Color.web(String.format("#%02X%02X%02X", 
    30 + (row * 15), 
    80 + (col * 12), 
    150 + ((10-row) * 8)));
cell.setFill(cellColor);
                    cell.setStroke(javafx.scene.paint.Color.web("#00d9ff"));
                    cell.setStrokeWidth(3);
                    grid.add(cell, col, row);
                }
            }
            
            Label bottomLabel = new Label("👆 100 BLUE CELLS (10x10) WITH CYAN BORDERS 👆");
            bottomLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff;");
            
            gridRoot.getChildren().addAll(titleLabel, gridLabel, grid, bottomLabel);
            
            // Switch the scene root
            if (mainScene != null) {
                statusLabel.getScene().setRoot(gridRoot);
            }
            
            logger.info("✅ Grid displayed successfully!");
        } catch (Exception e) {
            logger.error("❌ Failed to show Player Grid: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    public void showGodHandView() {
        logger.info("🧠 GodHand button clicked - reloading dashboard");
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GodHand.fxml"));
            Parent root = loader.load();
            
            // Get the current scene and set new root
            if (mainScene != null) {
                statusLabel.getScene().setRoot(root);
                logger.info("✅ Dashboard reloaded successfully!");
            } else {
                logger.warn("⚠️ statusLabel or scene is null");
            }
        } catch (Exception e) {
            logger.error("❌ Failed to reload GodHand: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

}
