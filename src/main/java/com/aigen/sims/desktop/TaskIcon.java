package com.aigen.sims.desktop;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Desktop-paradigm task file icon (.task). Drag onto an AgentIcon to assign. */
public class TaskIcon extends VBox {
    private final Task task;
    private final Label taskLabel, deadlineLabel;
    private final Circle priorityIndicator;

    public TaskIcon(Task task) {
        this.task = task;
        taskLabel = new Label(task.getTitle());
        deadlineLabel = new Label(task.getDeadline());
        priorityIndicator = new Circle(5, task.getPriority().getColor());
        HBox top = new HBox(6, priorityIndicator, taskLabel);
        getChildren().addAll(top, deadlineLabel);
        setPadding(new Insets(6));
        setStyle("-fx-border-color: #666; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #1b1b1b;");
        setupDragHandler();
        setupContextMenu();
    }

    private void setupDragHandler() {
        setOnDragDetected(e -> {
            javafx.scene.input.Dragboard db = startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(task.getId());
            db.setContent(cc);
            e.consume();
        });
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem details = new MenuItem("📋 View Details");
        details.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Task " + task.getId());
            a.setHeaderText(task.getTitle());
            a.setContentText("Priority: " + task.getPriority() + "\nDeadline: " + task.getDeadline()
                    + "\nStatus: " + task.getStatus() + "\nAssigned: " + task.getAssignedAgent());
            a.show();
        });
        MenuItem done = new MenuItem("✅ Mark Done");
        done.setOnAction(e -> task.setStatus("Completed"));
        menu.getItems().addAll(details, done);
        setOnContextMenuRequested(e -> menu.show(this, e.getScreenX(), e.getScreenY()));
    }
}
