package com.aigen.sims;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GodHandController {
    private static final Logger logger = LoggerFactory.getLogger(GodHandController.class);
    
    @FXML private Label statusLabel;
    @FXML private Button godHandButton;
    @FXML private Button playerGridButton;
    
    // TRACER: Track button state
    private int godHandClicks = 0;
    private int playerGridClicks = 0;
    
    @FXML
    public void initialize() {
        logger.info("========================================");
        logger.info("🔬 GodHandController.initialize() START");
        logger.info("========================================");
        
        // TRACER: Log field injection
        logger.info("Field Injection Check:");
        logger.info("  • statusLabel: {}", statusLabel == null ? "NULL ❌" : "OK ✅");
        logger.info("  • godHandButton: {}", godHandButton == null ? "NULL ❌" : "OK ✅");
        logger.info("  • playerGridButton: {}", playerGridButton == null ? "NULL ❌" : "OK ✅");
        
        // TRACER: Add button listeners to verify binding
        if (godHandButton != null) {
            godHandButton.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
                logger.info("🔍 GodHand BUTTON CLICK DETECTED (event filter)!");
                godHandClicks++;
                logger.info("  • Click count: {}", godHandClicks);
            });
            logger.info("✅ GodHand button event filter attached");
        }
        
        if (playerGridButton != null) {
            playerGridButton.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
                logger.info("🔍 PlayerGrid BUTTON CLICK DETECTED (event filter)!");
                playerGridClicks++;
                logger.info("  • Click count: {}", playerGridClicks);
            });
            logger.info("✅ PlayerGrid button event filter attached");
        }
        
        logger.info("========================================");
        logger.info("🔬 GodHandController.initialize() END");
        logger.info("========================================");
    }
    
    @FXML
    public void showGodHandView() {
        logger.info("========================================");
        logger.info("🧠 showGodHandView() CALLED");
        logger.info("  • Total clicks: {}", godHandClicks);
        logger.info("========================================");
        
        // TRACER: Verify method is executing
        try {
            logger.info("  • Attempting to update statusLabel...");
            if (statusLabel != null) {
                statusLabel.setText("🧠 GodHand View Active");
                logger.info("  ✅ statusLabel updated successfully");
            } else {
                logger.error("  ❌ statusLabel is NULL - cannot update");
            }
        } catch (Exception e) {
            logger.error("  ❌ Error in showGodHandView: {}", e.getMessage(), e);
        }
    }
    
    @FXML
    public void showPlayerGridView() {
        logger.info("========================================");
        logger.info("🎮 showPlayerGridView() CALLED");
        logger.info("  • Total clicks: {}", playerGridClicks);
        logger.info("========================================");
        
        // TRACER: Verify method is executing
        try {
            logger.info("  • Attempting to update statusLabel...");
            if (statusLabel != null) {
                statusLabel.setText("🎮 Player Grid View Active");
                logger.info("  ✅ statusLabel updated successfully");
            } else {
                logger.error("  ❌ statusLabel is NULL - cannot update");
            }
        } catch (Exception e) {
            logger.error("  ❌ Error in showPlayerGridView: {}", e.getMessage(), e);
        }
    }
}
