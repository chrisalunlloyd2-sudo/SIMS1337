package com.aigen.sims.desktop;

import com.aigen.sims.agents.SLMAgent;
import javafx.beans.property.*;

/** Desktop-paradigm Agent model: wraps an SLMAgent with live UI state. */
public class Agent {
    private final String name;
    private final SLMAgent slm;
    private final StringProperty status = new SimpleStringProperty("🟢 Active");
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty model = new SimpleStringProperty("");

    public Agent(String name, SLMAgent slm) {
        this.name = name;
        this.slm = slm;
        this.model.set(slm.getModelName());
    }

    public String getName() { return name; }
    public SLMAgent getSlm() { return slm; }
    public StringProperty statusProperty() { return status; }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty modelProperty() { return model; }

    public void busy(String emoji) { status.set(emoji); }
    public void setProgress(double p) { progress.set(p); }
    public void idle() { status.set("🟢 Active"); }

    @Override public String toString() { return name + " [" + model.get() + "]"; }
}
