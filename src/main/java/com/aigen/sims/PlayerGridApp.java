package com.aigen.sims;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Player Grid Application - 4D City Skyline Grid
 * Old-school Sims-style 3D/4D graphics
 */
public class PlayerGridApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(PlayerGridApp.class);
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Loading Player Grid interface...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlayerGrid.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("Player Grid - 4D City Skyline");
        primaryStage.setScene(new Scene(root, 1400, 900));
        primaryStage.show();
        
        logger.info("Player Grid displayed");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
