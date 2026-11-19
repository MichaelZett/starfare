package de.zettsystems.starfare.game.values;

/** How often random cosmic events strike a SectorForces game. */
public enum EventFrequency {
    OFF(0.0),
    RARE(0.5),
    NORMAL(1.0);

    private final double factor;

    EventFrequency(double factor) {
        this.factor = factor;
    }

    /** Scales the base chance of an event per round. */
    public double scale(double baseChance) {
        return baseChance * factor;
    }
}
