package com.aigen.sims.desktop;

import javafx.scene.shape.Circle;

/** Small colored circle for priority indication. */
public class ColorIndicator extends Circle {
    public ColorIndicator(Priority p) { super(5, p.getColor()); }
}
