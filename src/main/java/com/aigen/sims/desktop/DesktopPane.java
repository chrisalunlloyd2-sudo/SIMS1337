package com.aigen.sims.desktop;

import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.*;

/**
 * Desktop-paradigm root pane. Holds AgentIcons, TaskIcons, FolderIcons.
 * Drag a TaskIcon onto an AgentIcon to assign the task to that agent.
 */
public class DesktopPane extends Pane {
    private final ObservableList<Agent> agents = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final Map<String, AgentIcon> agentIcons = new HashMap<>();
    private final Map<String, TaskIcon> taskIcons = new HashMap<>();
    private final Rectangle selection;

    public DesktopPane() {
        setStyle("-fx-background-color: #141414;");
        selection = new Rectangle(0, 0, 0, 0);
        selection.setFill(Color.rgb(80, 140, 255, 0.15));
        selection.setStroke(Color.rgb(80, 140, 255, 0.6));
        selection.setVisible(false);
        getChildren().add(selection);
        setupDropTarget();
    }

    public ObservableList<Agent> getAgents() { return agents; }
    public ObservableList<Task> getTasks() { return tasks; }

    public void addAgent(Agent a) {
        agents.add(a);
        AgentIcon icon = new AgentIcon(a);
        agentIcons.put(a.getName(), icon);
        icon.setLayoutX(20 + (agents.size() - 1) * 130);
        icon.setLayoutY(20);
        getChildren().add(icon);
        // agent accepts task drops
        icon.setOnDragOver(e -> {
            if (e.getGestureSource() != icon && e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.MOVE);
            e.consume();
        });
        icon.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                Task t = findTask(db.getString());
                if (t != null) {
                    t.setAssignedAgent(a.getName());
                    t.setStatus("In Progress");
                    a.busy("⏳ Working");
                    new Thread(() -> {
                        try { Thread.sleep(1500); javafx.application.Platform.runLater(a::idle); }
                        catch (InterruptedException ignored) {}
                    }).start();
                }
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    public void addTask(Task t) {
        tasks.add(t);
        TaskIcon icon = new TaskIcon(t);
        taskIcons.put(t.getId(), icon);
        icon.setLayoutX(20 + (tasks.size() - 1) * 130);
        icon.setLayoutY(110);
        getChildren().add(icon);
    }

    public void addFolder(String name, List<Task> folderTasks) {
        FolderIcon folder = new FolderIcon(name);
        folder.setLayoutX(20 + (Math.random() * 200));
        folder.setLayoutY(200 + (Math.random() * 150));
        getChildren().add(folder);
        for (Task t : folderTasks) {
            TaskIcon ti = taskIcons.get(t.getId());
            if (ti != null) folder.addTask(ti);
        }
    }

    private Task findTask(String id) {
        return tasks.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
    }

    private void setupDropTarget() {
        setOnDragOver(e -> { e.acceptTransferModes(TransferMode.MOVE); e.consume(); });
        setOnDragDropped(e -> { e.setDropCompleted(true); e.consume(); });
    }

    /** Simple refresh: re-stamp backgrounds so listeners fire. */
    public void refresh() {
        agents.forEach(a -> a.idle());
        tasks.forEach(t -> { if (!"Completed".equals(t.getStatus())) t.setStatus("Pending"); });
    }
}
