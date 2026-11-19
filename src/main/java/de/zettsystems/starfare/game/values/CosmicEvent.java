package de.zettsystems.starfare.game.values;

/**
 * Random events of the SectorForces variant, after Galactic Empire (1983). Each has its own chance per
 * round; {@link EventFrequency} scales all of them.
 */
public enum CosmicEvent {
    /** Every fleet in hyperspace arrives one round later. */
    COSMIC_STORM(0.09),
    /** Every fleet in hyperspace has its remaining travel time halved. */
    SPACETIME_QUAKE(0.05),
    /** One fleet in hyperspace loses up to half of its ships. */
    DISAPPEARANCE(0.05),
    /** One world loses part of its garrison. */
    SPACEPORT_QUAKE(0.04),
    /** A sun turns unstable and explodes one round later, destroying its world. */
    SUPERNOVA(0.03),
    /** The neutral garrison of an exploding world flees as pirates. Needs {@link #SUPERNOVA}. */
    PIRATES(0.0);

    private final double baseChance;

    CosmicEvent(double baseChance) {
        this.baseChance = baseChance;
    }

    public double baseChance() {
        return baseChance;
    }
}
