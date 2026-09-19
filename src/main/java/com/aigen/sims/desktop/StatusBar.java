package com.aigen.sims.desktop;

import javafx.collections.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Desktop-paradigm bottom status bar: agent/task counts, funds, connection. */
public class StatusBar extends HBox {
    private final Label agentCountLabel = new Label("Agents: 0");
    private final Label taskCountLabel = new Label("Tasks: 0");
    private final Label fundsLabel = new Label("$0.00");
    private final Label connLabel = new Label("🟢 Connected");

    public StatusBar(ObservableList<Agent> agents, ObservableList<Task> tasks) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(agentCountLabel, spacer, taskCountLabel, fundsLabel, connLabel);
        setSpacing(18);
        setStyle("-fx-background-color: #111; -fx-padding: 4 10 4 10; -fx-border-color: #333; -fx-border-width: 1 0 0 0;");
        bind(agents, tasks);
    }

    private void bind(ObservableList<Agent> agents, ObservableList<Task> tasks) {
        agents.addListener((ListChangeListener<Agent>) c -> agentCountLabel.setText("Agents: " + agents.size()));
        tasks.addListener((ListChangeListener<Task>) c -> taskCountLabel.setText("Tasks: " + tasks.size()));
        agentCountLabel.setText("Agents: " + agents.size());
        taskCountLabel.setText("Tasks: " + tasks.size());
    }
}
