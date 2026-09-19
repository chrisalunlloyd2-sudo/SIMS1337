package com.aigen.sims.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/** Desktop-paradigm agent file icon (.agent). Double-click to interact. */
public class AgentIcon extends VBox {
    private final Agent agent;
    private final Label nameLabel, statusLabel;
    private final ProgressBar progressBar;

    public AgentIcon(Agent agent) {
        this.agent = agent;
        nameLabel = new Label(agent.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");
        statusLabel = new Label(agent.statusProperty().get());
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(90);
        getChildren().addAll(nameLabel, statusLabel, progressBar);
        setPadding(new Insets(6));
        setStyle("-fx-border-color: #555; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #222;");
        bindToAgentState();
        setupDoubleClickHandler();
        setupContextMenu();
    }

    private void bindToAgentState() {
        agent.statusProperty().addListener((obs, o, n) ->
            Platform.runLater(() -> statusLabel.setText(n)));
        agent.progressProperty().addListener((obs, o, n) ->
            Platform.runLater(() -> progressBar.setProgress(n.doubleValue())));
    }

    private void setupDoubleClickHandler() {
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TextInputDialog dlg = new TextInputDialog();
                dlg.setTitle("Agent: " + agent.getName());
                dlg.setHeaderText("Model: " + agent.modelProperty().get());
                dlg.setContentText("Prompt:");
                dlg.showAndWait().ifPresent(prompt -> {
                    agent.busy("⏳ Working");
                    new Thread(() -> {
                        try {
                            String out = agent.getSlm().generate(prompt, 200);
                            Platform.runLater(() -> {
                                Alert a = new Alert(Alert.AlertType.INFORMATION);
                                a.setTitle(agent.getName());
                                a.setHeaderText("Response");
                                a.setContentText(out);
                                a.show();
                                agent.idle();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> { agent.idle(); statusLabel.setText("❌ " + ex.getMessage()); });
                        }
                    }).start();
                });
            }
        });
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem chat = new MenuItem("💬 Chat");
        chat.setOnAction(e -> statusLabel.setText("(double-click to chat)"));
        menu.getItems().add(chat);
        setOnContextMenuRequested(e -> menu.show(this, e.getScreenX(), e.getScreenY()));
    }
}
