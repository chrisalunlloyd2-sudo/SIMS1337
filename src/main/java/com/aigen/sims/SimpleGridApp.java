package com.aigen.sims;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SIMPLE GRID TEST - Just shows a 10x10 grid, nothing else
 */
public class SimpleGridApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(SimpleGridApp.class);
    
    @FXML
    private GridPane testGrid;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Loading Simple Grid Test...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SimpleGridTest.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        
        // Build the grid BEFORE showing
        buildGrid();
        
        primaryStage.setTitle("🎮 SIMPLE GRID TEST - 10x10");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
        
        logger.info("Grid test window displayed - {} cells created", 10 * 10);
    }
    
    private void buildGrid() {
        logger.info("Building 10x10 grid with VERY VISIBLE cells...");
        
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                // BIG bright red cells - impossible to miss
                Rectangle cell = new Rectangle(50, 50);
                cell.setFill(Color.RED);
                cell.setStroke(Color.YELLOW);
                cell.setStrokeWidth(3);
                
                // Add to grid
                testGrid.add(cell, col, row);
                
                logger.debug("Cell [{},{}] added", row, col);
            }
        }
        
        logger.info("✅ Grid built: 100 cells (10x10)");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
