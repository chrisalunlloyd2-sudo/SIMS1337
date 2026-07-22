package com.aigen.sims;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Player Grid 3D Controller - XYZ City Skyline with Model Chat
 */
public class PlayerGrid3DController {
    private static final Logger logger = LoggerFactory.getLogger(PlayerGrid3DController.class);
    
    @FXML
    private VBox chatBox;
    
    @FXML
    private TextField chatInput;
    
    // Player positions (XYZ coordinates)
    private final PlayerPosition[] players = {
        new PlayerPosition("Agent Alpha", 3, 7, 2, Color.LIGHTGREEN),
        new PlayerPosition("Agent Beta", 8, 4, 1, Color.DODGERBLUE),
        new PlayerPosition("Agent Gamma", 5, 9, 3, Color.ORANGE)
    };
    
    @FXML
    public void initialize() {
        logger.info("Player Grid 3D initialized");
        addChatMessage("System", "Player Grid 3D initialized - XYZ coordinates active");
        addChatMessage("System", "3 agents online: Alpha, Beta, Gamma");
        
        // Simulate model chat
        simulateModelChat();
    }
    
    @FXML
    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            addChatMessage("You", message);
            chatInput.clear();
            
            // Simulate model response
            simulateModelResponse(message);
        }
    }
    
    @FXML
    private void buildFactory() {
        addChatMessage("System", "🏗️ Brute Foundry initialized at (6, 6, 0)");
        logger.info("Factory built at XYZ(6,6,0)");
    }
    
    @FXML
    private void openABLab() {
        addChatMessage("System", "🧬 A/B Testing Lab opened at (9, 3, 1)");
        logger.info("A/B Lab opened");
    }
    
    @FXML
    private void openTree() {
        addChatMessage("System", "🌳 Tree of Knowledge rooted at (2, 8, 2)");
        logger.info("Knowledge Tree opened");
    }
    
    @FXML
    private void openResearch() {
        addChatMessage("System", "🔬 Research Lab activated at (10, 5, 1)");
        logger.info("Research Lab opened");
    }
    
    @FXML
    private void refreshGrid() {
        addChatMessage("System", "🔄 Grid refreshed - XYZ coordinates updated");
        logger.info("Grid refreshed");
    }
    
    private void addChatMessage(String sender, String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        Label msgLabel = new Label("[" + timestamp + "] " + sender + ": " + message);
        msgLabel.setStyle(getSenderStyle(sender));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(330);
        
        chatBox.getChildren().add(msgLabel);
        
        // Auto-scroll to bottom
        if (chatBox.getParent() instanceof ScrollPane) {
            ScrollPane scroll = (ScrollPane) chatBox.getParent();
            scroll.setVvalue(1.0);
        }
        
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
    
    private void simulateModelChat() {
        // Simulate ongoing model conversations
        javafx.animation.AnimationTimer chatTimer = new javafx.animation.AnimationTimer() {
            private long lastChat = 0;
            private int messageIndex = 0;
            
            @Override
            public void handle(long now) {
                if (now - lastChat > 3_000_000_000L) { // Every 3 seconds
                    String[] chats = {
                        "qwen2.5: Processing task queue...",
                        "tinyllama: Grid coordinates updated",
                        "phi: Agent Alpha moving to (4, 7, 2)",
                        "qwen2.5: LoRA adapter switched to CODE",
                        "tinyllama: Factory output: +15%",
                        "phi: Research complete: new optimization found"
                    };
                    
                    String[] parts = chats[messageIndex % chats.length].split(": ");
                    addChatMessage(parts[0], parts[1]);
                    messageIndex++;
                    lastChat = now;
                }
            }
        };
        chatTimer.start();
    }
    
    private void simulateModelResponse(String userMessage) {
        // Simulate model responses after 1 second using PauseTransition
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> {
            String[] responses = {
                "Processing your request...",
                "Task queued for execution",
                "Analyzing grid coordinates...",
                "Optimizing agent paths..."
            };
            String response = responses[(int)(Math.random() * responses.length)];
            addChatMessage("phi", response);
        });
        delay.play();
    }
    
    // Inner class for player positions
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
