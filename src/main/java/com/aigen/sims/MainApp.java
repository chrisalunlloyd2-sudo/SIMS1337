package com.aigen.sims;

import com.aigen.sims.routing.ModelPool;
import com.aigen.sims.routing.ModelRouter;
import com.aigen.sims.routing.TaskQueue;
import com.aigen.sims.routing.LoRASwitcher;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX Application - GodHand Interface
 */
public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    
    // Core routing components (shared across app)
    public static ModelPool modelPool;
    public static ModelRouter modelRouter;
    public static TaskQueue taskQueue;
    public static LoRASwitcher loraSwitcher;
    
    @Override
    public void init() {
        logger.info("Initializing GodHand GUI...");
        
        // Initialize core routing system
        modelPool = new ModelPool();
        modelRouter = new ModelRouter(modelPool);
        taskQueue = new TaskQueue(100);
        loraSwitcher = new LoRASwitcher();
        
        logger.info("Core routing system initialized");
        logger.info("  - Models: {}", modelPool.getModelCount());
        logger.info("  - Queue capacity: {}", 100);
        logger.info("  - LoRA adapters: {}", loraSwitcher.getAvailableAdapters().length);
        
        // Warm up models asynchronously (don't block GUI startup)
        new Thread(() -> {
            try {
                logger.info("Warming up models in background...");
                modelPool.warmUpAll();
            } catch (Exception e) {
                logger.warn("Model warm-up had issues (GUI will still work): {}", e.getMessage());
            }
        }).start();
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Loading GodHand interface...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GodHand.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("GodHand - Agent Orchestration Dashboard");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
        
        logger.info("GodHand GUI displayed");
        
        // Keep app running even if models fail
        primaryStage.setOnCloseRequest(e -> {
            logger.info("GodHand closing...");
        });
    }
    
    @Override
    public void stop() {
        logger.info("Shutting down GodHand...");
        logger.info("Saving state...");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
