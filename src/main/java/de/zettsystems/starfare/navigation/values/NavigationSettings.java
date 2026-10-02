package de.zettsystems.starfare.navigation.values;
/** Frozen maximum distance per flight leg, independent of speed and sensors. */
public record NavigationSettings(double range) {
    public NavigationSettings { if (!Double.isFinite(range) || range <= 0) { throw new IllegalArgumentException("Invalid flight range"); } }
}
