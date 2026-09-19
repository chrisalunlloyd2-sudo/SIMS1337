package com.aigen.sims.desktop;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Desktop-paradigm folder icon. Double-click to expand/collapse contents. */
public class FolderIcon extends VBox {
    private final Label folderLabel;
    private final ObservableList<TaskIcon> contents = FXCollections.observableArrayList();
    private final BooleanProperty expanded = new SimpleBooleanProperty(false);

    public FolderIcon(String name) {
        folderLabel = new Label("📁 " + name);
        folderLabel.setStyle("-fx-font-weight: bold;");
        getChildren().add(folderLabel);
        setPadding(new Insets(6));
        setStyle("-fx-border-color: #888; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #2a2a2a;");
        setupExpandCollapse();
    }

    private void setupExpandCollapse() {
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                expanded.set(!expanded.get());
                updateVisualState();
            }
        });
    }

    private void updateVisualState() {
        if (expanded.get()) {
            getChildren().setAll(folderLabel);
            getChildren().addAll(contents);
        } else {
            getChildren().setAll(folderLabel);
        }
    }

    public void addTask(TaskIcon icon) { contents.add(icon); if (expanded.get()) getChildren().add(icon); }
}
