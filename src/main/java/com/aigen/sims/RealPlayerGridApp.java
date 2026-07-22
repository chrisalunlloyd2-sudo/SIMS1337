package com.aigen.sims;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Real Player Grid 3D App - 10x10 XYZ Grid with City Skyline
 */
public class RealPlayerGridApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(RealPlayerGridApp.class);
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Loading Real Player Grid 3D...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RealPlayerGrid.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("🎮 Player Grid 3D - 10x10 XYZ City Skyline");
        primaryStage.setScene(new Scene(root, 1400, 900));
        primaryStage.show();
        
        logger.info("Real Player Grid 3D displayed");
        logger.info("Features: 10x10x5 XYZ grid, 5 buildings, 3 agents, live chat");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
