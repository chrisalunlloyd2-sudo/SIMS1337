package com.aigen.sims.tasks;

/**
 * Task complexity levels for model routing
 */
public enum Complexity {
    VERY_LOW(0.1),
    LOW(0.3),
    MEDIUM(0.6),
    HIGH(0.8),
    VERY_HIGH(0.95),
    CRITICAL(1.0);

    private final double multiplier;

    Complexity(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
