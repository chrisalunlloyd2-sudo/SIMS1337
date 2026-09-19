package com.aigen.sims.desktop;

import javafx.scene.paint.Color;

/** Task priority with a display color. */
public enum Priority {
    LOW(Color.GRAY), MEDIUM(Color.ORANGE), HIGH(Color.RED), CRITICAL(Color.PURPLE);

    private final Color color;
    Priority(Color c) { this.color = c; }
    public Color getColor() { return color; }
}
