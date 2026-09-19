package com.aigen.sims.desktop;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/** Desktop-paradigm top menu: File / Agents / Tasks / Help. */
public class DesktopMenuBar extends MenuBar {
    public DesktopMenuBar(Runnable onRefresh, Runnable onNewAgent, Runnable onNewTask) {
        Menu file = new Menu("File");
        MenuItem refresh = new MenuItem("🔄 Refresh");
        refresh.setOnAction(e -> onRefresh.run());
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> javafx.application.Platform.exit());
        file.getItems().addAll(refresh, new SeparatorMenuItem(), exit);

        Menu agents = new Menu("Agents");
        MenuItem newAgent = new MenuItem("➕ New Agent");
        newAgent.setOnAction(e -> onNewAgent.run());
        agents.getItems().add(newAgent);

        Menu tasks = new Menu("Tasks");
        MenuItem newTask = new MenuItem("➕ New Task");
        newTask.setOnAction(e -> onNewTask.run());
        tasks.getItems().add(newTask);

        Menu help = new Menu("Help");
        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            a.setTitle("About");
            a.setHeaderText("SIMS1337 — WORLD AS DESKTOP PARADIGM");
            a.setContentText("Agents are files. Tasks are files. Drag, drop, double-click.");
            a.show();
        });
        help.getItems().add(about);

        getMenus().addAll(file, agents, tasks, help);
    }
}
