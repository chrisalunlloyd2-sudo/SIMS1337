package com.aigen.sims;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Unified Backend Controller - Combines GodHand + Player Grid 3D + All Facilities
 */
public class UnifiedBackendController {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedBackendController.class);
    
    @FXML private Label statusLabel;
    @FXML private VBox modelPanel, adapterPanel, queuePanel;
    @FXML private VBox chatPanel, facilitiesPanel;
    
    @FXML
    public void initialize() {
        logger.info("Unified Backend initialized");
        
        // Initialize GodHand panels
        initModelPanel();
        initAdapterPanel();
        initQueuePanel();
        
        // Initialize Chat
        initChat();
        
        // Initialize Facilities
        initFacilities();
        
        addChatMessage("System", "Unified Backend v1.0 - All systems online");
        addChatMessage("System", "GodHand + Player Grid 3D integrated");
    }
    
    private void initModelPanel() {
        Label title = new Label("🧠 MODEL STATUS");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00ff88;");
        modelPanel.getChildren().add(title);
        
        String[] models = {"qwen2.5:0.5b 🟢", "tinyllama:1.1b 🟢", "phi:latest 🟢", "phi3:mini 🟡"};
        for (String model : models) {
            Label lbl = new Label(model);
            lbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px;");
            modelPanel.getChildren().add(lbl);
        }
    }
    
    private void initAdapterPanel() {
        Label title = new Label("🔄 LoRA ADAPTERS");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #00d9ff;");
        adapterPanel.getChildren().add(title);
        
        String[] adapters = {"CHAT", "CODE", "PATHFIND", "MOTIVES", "CAREER", "ANALYSIS"};
        for (String adapter : adapters) {
            Label lbl = new Label("⚪ " + adapter);
            lbl.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
            adapterPanel.getChildren().add(lbl);
        }
    }
    
    private void initQueuePanel() {
        Label title = new Label("📋 TASK QUEUE");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffaa00;");
        queuePanel.getChildren().add(title);
        
        Label queueInfo = new Label("Tasks: 0 | Utilization: 0%");
        queueInfo.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px;");
        queuePanel.getChildren().add(queueInfo);
    }
    
    private void initChat() {
        Label title = new Label("💬 MODEL CHAT");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c77dff;");
        chatPanel.getChildren().add(title);
        
        ScrollPane scroll = new ScrollPane();
        scroll.setPrefHeight(200);
        scroll.setStyle("-fx-background-color: #0a0a15; -fx-background-radius: 5;");
        
        VBox chatBox = new VBox(5);
        chatBox.setStyle("-fx-padding: 5;");
        scroll.setContent(chatBox);
        
        chatPanel.getChildren().add(scroll);
        
        // Simulate chat
        simulateChat(chatBox);
    }
    
    private void initFacilities() {
        Label title = new Label("🏗️ FACILITIES");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ff6b6b;");
        facilitiesPanel.getChildren().add(title);
        
        String[] facilities = {
            "🏗️ Brute Foundry - Idle",
            "🧬 A/B Lab - Standby",
            "🌳 Knowledge Tree - Rooted",
            "🔬 Research Lab - Active",
            "🔒 Secrets Locker - Locked",
            "🏥 Hospital - Ready",
            "📡 GitHub Station - Synced"
        };
        
        for (String facility : facilities) {
            Label lbl = new Label(facility);
            lbl.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
            facilitiesPanel.getChildren().add(lbl);
        }
    }
    
    private void simulateChat(VBox chatBox) {
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            private long lastMsg = 0;
            private int idx = 0;
            
            @Override
            public void handle(long now) {
                if (now - lastMsg > 4_000_000_000L) {
                    String[] msgs = {
                        "qwen2.5: Processing task #42",
                        "tinyllama: Grid sync complete",
                        "phi: Agent path optimized",
                        "qwen2.5: LoRA switch: CODE → CHAT",
                        "phi3: Analysis complete ✅"
                    };
                    
                    Label msg = new Label("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msgs[idx % msgs.length]);
                    msg.setStyle("-fx-text-fill: #00d9ff; -fx-font-size: 11px; -fx-wrap-text: true;");
                    chatBox.getChildren().add(msg);
                    
                    idx++;
                    lastMsg = now;
                }
            }
        };
        timer.start();
    }
    
    private void addChatMessage(String sender, String message) {
        logger.info("Chat: {} - {}", sender, message);
    }
    
    @FXML private void openBruteFoundry() { logger.info("🏗️ Brute Foundry opened"); }
    @FXML private void openABTesting() { logger.info("🧬 A/B Testing opened"); }
    @FXML private void openKnowledgeTree() { logger.info("🌳 Knowledge Tree opened"); }
    @FXML private void openResearchLab() { logger.info("🔬 Research Lab opened"); }
    @FXML private void openSecretsLocker() { logger.info("🔒 Secrets Locker opened"); }
    @FXML private void openHospital() { logger.info("🏥 Hospital opened"); }
    @FXML private void openGitHubStation() { logger.info("📡 GitHub Station opened"); }
}
