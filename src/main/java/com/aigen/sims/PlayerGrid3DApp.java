package com.aigen.sims;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Player Grid 3D Application - XYZ City Skyline with Model Chat
 */
public class PlayerGrid3DApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(PlayerGrid3DApp.class);
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Loading Player Grid 3D...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlayerGrid3D.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("Player Grid 3D - XYZ City Skyline + Model Chat");
        primaryStage.setScene(new Scene(root, 1600, 1000));
        primaryStage.show();
        
        logger.info("Player Grid 3D displayed");
        logger.info("Features: XYZ coordinates, 3D skyline, live model chat");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
