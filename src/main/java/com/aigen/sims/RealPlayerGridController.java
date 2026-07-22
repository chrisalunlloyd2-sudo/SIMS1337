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
 * REAL Player Grid Controller - 10x10 XYZ 3D Grid with City Skyline
 */
public class RealPlayerGridController {
    private static final Logger logger = LoggerFactory.getLogger(RealPlayerGridController.class);
    
    @FXML private Label statusLabel;
    @FXML private GridPane mainGrid;
    @FXML private HBox skylineBox;
    @FXML private VBox playerPanel, chatPanel, facilityPanel;
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatBox;
    @FXML private TextField chatInput;
    
    // 10x10 grid with XYZ coordinates
    private final int GRID_SIZE = 10;
    private final Rectangle[][][] gridCells = new Rectangle[GRID_SIZE][GRID_SIZE][5];
    
    // Player positions
    private final PlayerPosition[] players = {
        new PlayerPosition("Agent Alpha", 3, 7, 2, Color.LIGHTGREEN),
        new PlayerPosition("Agent Beta", 8, 4, 1, Color.DODGERBLUE),
        new PlayerPosition("Agent Gamma", 5, 9, 3, Color.ORANGE)
    };
    
    @FXML
    public void initialize() {
        logger.info("Real Player Grid 3D initialized - 10x10x5 XYZ grid");
        
        // Build the actual 10x10 grid
        buildGrid();
        
        // Build city skyline
        buildSkyline();
        
        // Add players
        addPlayers();
        
        // Add facilities
        addFacilities();
        
        // Start chat
        initChat();
        
        addChatMessage("System", "🎮 Real Player Grid 3D initialized");
        addChatMessage("System", "✅ 10x10x5 XYZ grid rendered");
        addChatMessage("System", "✅ 3 agents online: Alpha, Beta, Gamma");
        
        // Simulate model chat
        simulateModelChat();
    }
    
    private void buildGrid() {
        logger.info("Building 10x10x5 grid...");
        
        for (int z = 0; z < 5; z++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int x = 0; x < GRID_SIZE; x++) {
                    // Create grid cell
                    Rectangle cell = new Rectangle(30, 30);
                    cell.setArcWidth(5);
                    cell.setArcHeight(5);
                    
                    // Color based on Z depth
                    Color baseColor = switch (z) {
                        case 0 -> Color.web("#1a3a5c"); // Bottom layer
                        case 1 -> Color.web("#1a4a6c");
                        case 2 -> Color.web("#1a5a7c");
                        case 3 -> Color.web("#1a6a8c");
                        case 4 -> Color.web("#1a7a9c"); // Top layer
                        default -> Color.GRAY;
                    };
                    
                    // Add glow effect to some cells
                    if ((x + y + z) % 3 == 0) {
                        Bloom bloom = new Bloom(0.5);
                        cell.setEffect(bloom);
                    }
                    
                    cell.setFill(baseColor);
                    cell.setStroke(Color.web("#00d9ff"));
                    cell.setStrokeWidth(1);
                    
                    // Store coordinates in userData
                    cell.setUserData("(" + x + ", " + y + ", " + z + ")");
                    
                    gridCells[x][y][z] = cell;
                    
                    // Only show top layer by default
                    if (z == 4) {
                        mainGrid.add(cell, x, y);
                    }
                }
            }
        }
        
        logger.info("Grid built: {} cells", GRID_SIZE * GRID_SIZE * 5);
    }
    
    private void buildSkyline() {
        logger.info("Building city skyline...");
        
        int[] buildingHeights = {200, 280, 350, 300, 220};
        Color[] buildingColors = {
            Color.web("#1a1a3e"),
            Color.web("#2a2a4e"),
            Color.web("#1a1a3e"),
            Color.web("#2a2a4e"),
            Color.web("#1a1a3e")
        };
        Color[] strokeColors = {
            Color.web("#00d9ff"),
            Color.web("#00ff88"),
            Color.web("#ffaa00"),
            Color.web("#c77dff"),
            Color.web("#ff6b6b")
        };
        
        for (int i = 0; i < 5; i++) {
            Rectangle building = new Rectangle(50, buildingHeights[i]);
            building.setFill(buildingColors[i]);
            building.setStroke(strokeColors[i]);
            building.setStrokeWidth(2);
            
            Bloom bloom = new Bloom(0.7);
            building.setEffect(bloom);
            
            building.setUserData("Building " + (i + 1) + " - " + buildingHeights[i] + "m");
            
            skylineBox.getChildren().add(building);
        }
        
        logger.info("Skyline built: 5 buildings");
    }
    
    private void addPlayers() {
        logger.info("Adding {} players", players.length);
        
        for (PlayerPosition player : players) {
            VBox playerBox = new VBox(8);
            
            HBox nameBox = new HBox(10);
            nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Circle indicator = new Circle(10, player.color);
            Label nameLabel = new Label(player.name);
            nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");
            
            nameBox.getChildren().addAll(indicator, nameLabel);
            
            Label posLabel = new Label("Pos: (" + player.x + ", " + player.y + ", " + player.z + ")");
            posLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
            
            Label statusLabel = new Label("Status: Active");
            statusLabel.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 11px;");
            
            playerBox.getChildren().addAll(nameBox, posLabel, statusLabel);
            playerPanel.getChildren().add(playerBox);
        }
    }
    
    private void addFacilities() {
        String[] facilities = {
            "🏗️ Brute Foundry",
            "🧬 A/B Testing Lab",
            "🌳 Knowledge Tree",
            "🔬 Research Lab",
            "🔒 Secrets Locker",
            "🏥 Hospital",
            "📡 GitHub Station"
        };
        
        Color[] colors = {
            Color.web("#ff6b6b"),
            Color.web("#c77dff"),
            Color.web("#00d9ff"),
            Color.web("#ffaa00"),
            Color.web("#999999"),
            Color.web("#ff6b9d"),
            Color.web("#6e5494")
        };
        
        for (int i = 0; i < facilities.length; i++) {
            HBox facilityBox = new HBox(10);
            facilityBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Rectangle indicator = new Rectangle(14, 14, colors[i]);
            indicator.setArcWidth(3);
            indicator.setArcHeight(3);
            
            Label nameLabel = new Label(facilities[i]);
            nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px;");
            
            facilityBox.getChildren().addAll(indicator, nameLabel);
            facilityPanel.getChildren().add(facilityBox);
        }
    }
    
    private void initChat() {
        addChatMessage("System", "Chat system ready");
    }
    
    @FXML
    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            addChatMessage("You", message);
            chatInput.clear();
            
            // Simulate response
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
            private int msgIdx = 0;
            
            @Override
            public void handle(long now) {
                if (now - lastChat > 4_000_000_000L) {
                    String[] chats = {
                        "qwen2.5: Processing task queue...",
                        "tinyllama: Grid sync complete ✅",
                        "phi: Agent Alpha moving to (4, 7, 2)",
                        "qwen2.5: LoRA adapter switched to CODE",
                        "tinyllama: Factory output +15%",
                        "phi: Research complete - new optimization"
                    };
                    
                    String[] parts = chats[msgIdx % chats.length].split(": ");
                    addChatMessage(parts[0], parts[1]);
                    msgIdx++;
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
        
        // Auto-scroll
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
    
    // Facility button handlers
    @FXML private void buildFactory() { addChatMessage("System", "🏗️ Brute Foundry built at (6, 6, 0)"); }
    @FXML private void openABLab() { addChatMessage("System", "🧬 A/B Lab opened at (8, 3, 1)"); }
    @FXML private void openTree() { addChatMessage("System", "🌳 Knowledge Tree rooted at (2, 8, 2)"); }
    @FXML private void openResearch() { addChatMessage("System", "🔬 Research Lab activated at (9, 5, 1)"); }
    @FXML private void openSecrets() { addChatMessage("System", "🔒 Secrets Locker secured"); }
    @FXML private void openHospital() { addChatMessage("System", "🏥 Hospital ready for agents"); }
    @FXML private void openGitHub() { addChatMessage("System", "📡 GitHub Station synced"); }
    
    private static class PlayerPosition {
        String name;
        int x, y, z;
        Color color;
        
        PlayerPosition(String name, int x, int y, int z, Color color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }
    }
}
