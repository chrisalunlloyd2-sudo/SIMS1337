package com.aigen.sims;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tabbed GodHand App - All GUIs in one window
 */
public class TabbedGodHandApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(TabbedGodHandApp.class);
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Loading Tabbed GodHand...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TabbedGodHand.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("⚙️ SIMS1337 - Unified Control Center (GodHand + Player Grid + Facilities)");
        primaryStage.setScene(new Scene(root, 1600, 1000));
        primaryStage.show();
        
        logger.info("Tabbed GodHand displayed - 3 tabs: GodHand, Player Grid 3D, Facilities");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
