package com.aigen.sims;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.Bloom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Tabbed GodHand Controller - All GUIs in one window with tabs
 */
public class TabbedGodHandController {
    private static final Logger logger = LoggerFactory.getLogger(TabbedGodHandController.class);
    
    @FXML private Label statusLabel;
    @FXML private TabPane mainTabPane;
    @FXML private VBox modelPanel, adapterPanel, queuePanel;
    @FXML private GridPane simpleGrid;
    @FXML private HBox skylineBox;
    @FXML private VBox playerPanel, chatPanel, chatBox, facilitiesPanel;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField chatInput;
    
    // Simple 10x10 grid
    private static final int GRID_SIZE = 10;
    
    @FXML
    public void initialize() {
        logger.info("Tabbed GodHand initialized - 3 tabs");
        
        // Tab 1: GodHand
        initGodHandTab();
        
        // Tab 2: Player Grid
        initPlayerGridTab();
        
        // Tab 3: Facilities
        initFacilitiesTab();
        
        addChatMessage("System", "⚙️ Unified Control Center initialized");
        addChatMessage("System", "✅ Tab 1: GodHand Dashboard");
        addChatMessage("System", "✅ Tab 2: Player Grid 3D (10x10x5)");
        addChatMessage("System", "✅ Tab 3: Facilities Control");
        
        // Start model chat
        simulateModelChat();
    }
    
    private void initGodHandTab() {
        logger.info("Initializing GodHand tab...");
        
        // Models
        String[] models = {
            "qwen2.5:0.5b - 🟢 Warm (Fast Tier)",
            "tinyllama:1.1b - 🟢 Warm (Balanced Tier)",
            "phi:latest - 🟢 Warm (Reasoning Tier)",
            "phi3:mini - 🟡 Cold (Deep Tier)"
        };
        
        for (String model : models) {
            Label lbl = new Label(model);
            lbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px;");
            modelPanel.getChildren().add(lbl);
        }
        
        // LoRA Adapters
        String[] adapters = {"CHAT", "CODE", "PATHFIND", "MOTIVES", "CAREER", "ANALYSIS"};
        for (String adapter : adapters) {
            Label lbl = new Label("⚪ " + adapter);
            lbl.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 12px;");
            adapterPanel.getChildren().add(lbl);
        }
        
        // Queue
        Label queueInfo = new Label("Tasks: 0 | Utilization: 0% | Active: 0");
        queueInfo.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px;");
        queuePanel.getChildren().add(queueInfo);
    }
    
    private void initPlayerGridTab() {
        logger.info("Initializing Player Grid tab - 10x10 SIMPLE RED GRID...");
        
        // Build SIMPLE 10x10 red grid with yellow borders
        buildSimpleGrid();
        
        // Build skyline
        buildSkyline();
        
        // Add players
        addPlayers();
    }
    
    private void buildSimpleGrid() {
        logger.info("Building 10x10 RED grid with YELLOW borders...");
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle cell = new Rectangle(50, 50);
                cell.setFill(Color.RED);
                cell.setStroke(Color.YELLOW);
                cell.setStrokeWidth(3);
                
                simpleGrid.add(cell, col, row);
                logger.debug("Red cell [{},{}] added", row, col);
            }
        }
        
        logger.info("✅ SIMPLE GRID BUILT: {} red cells", GRID_SIZE * GRID_SIZE);
    }
    
    private void buildSkyline() {
        int[] heights = {200, 280, 350, 300, 220};
        Color[] fills = {
            Color.web("#1a1a3e"), Color.web("#2a2a4e"),
            Color.web("#1a1a3e"), Color.web("#2a2a4e"),
            Color.web("#1a1a3e")
        };
        Color[] strokes = {
            Color.web("#00d9ff"), Color.web("#00ff88"),
            Color.web("#ffaa00"), Color.web("#c77dff"),
            Color.web("#ff6b6b")
        };
        
        for (int i = 0; i < 5; i++) {
            Rectangle building = new Rectangle(50, heights[i]);
            building.setFill(fills[i]);
            building.setStroke(strokes[i]);
            building.setStrokeWidth(2);
            building.setEffect(new Bloom(0.7));
            skylineBox.getChildren().add(building);
        }
        
        logger.info("Skyline built: 5 buildings");
    }
    
    private void addPlayers() {
        String[][] players = {
            {"Agent Alpha", "3", "7", "2"},
            {"Agent Beta", "8", "4", "1"},
            {"Agent Gamma", "5", "9", "3"}
        };
        Color[] colors = {Color.LIGHTGREEN, Color.DODGERBLUE, Color.ORANGE};
        
        for (int i = 0; i < players.length; i++) {
            VBox playerBox = new VBox(8);
            
            HBox nameBox = new HBox(10);
            nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Circle indicator = new Circle(10, colors[i]);
            Label nameLabel = new Label(players[i][0]);
            nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");
            nameBox.getChildren().addAll(indicator, nameLabel);
            
            Label posLabel = new Label("Pos: (" + players[i][1] + ", " + players[i][2] + ", " + players[i][3] + ")");
            posLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
            
            Label statusLabel = new Label("Status: Active");
            statusLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 11px;");
            
            playerBox.getChildren().addAll(nameBox, posLabel, statusLabel);
            playerPanel.getChildren().add(playerBox);
        }
    }
    
    private void initFacilitiesTab() {
        logger.info("Initializing Facilities tab...");
        
        String[][] facilities = {
            {"🏗️ Brute Foundry", "Autonomous coding factory", "#ff6b6b"},
            {"🧬 A/B Testing Lab", "Model comparison", "#c77dff"},
            {"🌳 Knowledge Tree", "Database hierarchy", "#00d9ff"},
            {"🔬 Research Lab", "Web crawlers", "#ffaa00"},
            {"🔒 Secrets Locker", "Encrypted storage", "#999999"},
            {"🏥 Hospital", "Agent recovery", "#ff6b9d"},
            {"📡 GitHub Station", "AST crawler", "#6e5494"}
        };
        
        for (String[] facility : facilities) {
            try {
                VBox card = new VBox(10);
                card.setStyle("-fx-background-color: #16213e; -fx-padding: 15; -fx-background-radius: 10; -fx-min-width: 180;");
                
                Label name = new Label(facility[0]);
                name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + facility[2] + ";");
                
                Label desc = new Label(facility[1]);
                desc.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
                
                Button openBtn = new Button("Open");
                openBtn.setStyle("-fx-background-color: " + facility[2] + "; -fx-text-fill: #ffffff; -fx-font-size: 11px;");
                openBtn.setMaxWidth(Double.MAX_VALUE);
                
                card.getChildren().addAll(name, desc, openBtn);
                facilitiesPanel.getChildren().add(card);
                
                logger.debug("Added facility card: {}", facility[0]);
            } catch (Exception e) {
                logger.error("Failed to add facility card: {}", facility[0], e);
            }
        }
        
        logger.info("✅ Facilities tab initialized: {} cards", facilities.length);
    }
    
    @FXML
    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            addChatMessage("You", message);
            chatInput.clear();
            
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            delay.setOnFinished(e -> {
                String[] responses = {"Processing...", "Task queued", "Analyzing...", "Optimizing..."};
                addChatMessage("phi", responses[(int)(Math.random() * responses.length)]);
            });
            delay.play();
        }
    }
    
    private void simulateModelChat() {
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            private long lastChat = 0;
            private int idx = 0;
            
            @Override
            public void handle(long now) {
                if (now - lastChat > 4_000_000_000L) {
                    String[] chats = {
                        "qwen2.5: Processing task queue...",
                        "tinyllama: Grid sync complete ✅",
                        "phi: Agent Alpha moving to (4, 7, 2)",
                        "qwen2.5: LoRA adapter switched to CODE",
                        "phi3: Analysis complete"
                    };
                    
                    String[] parts = chats[idx % chats.length].split(": ");
                    addChatMessage(parts[0], parts[1]);
                    idx++;
                    lastChat = now;
                }
            }
        };
        timer.start();
    }
    
    private void addChatMessage(String sender, String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        Label msgLabel = new Label("[" + timestamp + "] " + sender + ": " + message);
        msgLabel.setStyle(getSenderStyle(sender));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(280);
        
        chatBox.getChildren().add(msgLabel);
        chatScroll.setVvalue(1.0);
        
        logger.debug("Chat: {} - {}", sender, message);
    }
    
    private String getSenderStyle(String sender) {
        return switch (sender) {
            case "System" -> "-fx-text-fill: #a0a0a0; -fx-font-size: 11px;";
            case "You" -> "-fx-text-fill: #00ff88; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "qwen2.5" -> "-fx-text-fill: #00d9ff; -fx-font-size: 11px;";
            case "tinyllama" -> "-fx-text-fill: #ffaa00; -fx-font-size: 11px;";
            case "phi" -> "-fx-text-fill: #c77dff; -fx-font-size: 11px;";
            default -> "-fx-text-fill: #ffffff; -fx-font-size: 11px;";
        };
    }
}
