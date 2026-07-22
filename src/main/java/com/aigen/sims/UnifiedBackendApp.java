package com.aigen.sims;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified Backend App - Combines GodHand + Player Grid 3D
 */
public class UnifiedBackendApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedBackendApp.class);
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Loading Unified Backend...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UnifiedBackend.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("SIMS1337 BACKEND - Unified Control Center");
        primaryStage.setScene(new Scene(root, 1800, 1100));
        primaryStage.show();
        
        logger.info("Unified Backend displayed - GodHand + Player Grid 3D integrated");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
