package de.zettsystems.starfare.game.values;

/**
 * Size of one galaxy map in map pixels. The area grows with the number of systems, so every galaxy has the
 * same density and a jump to a neighbour takes about as long on a small map as on a huge one.
 *
 * @param width  horizontal extent; systems lie between {@link GameConfig#SYSTEM_MARGIN} and the width minus it
 * @param height vertical extent, with the same margin
 */
public record GalaxyExtent(int width, int height) {
    /** Maps of games created before the size followed the system count. */
    public static final GalaxyExtent LEGACY = new GalaxyExtent(3200, 2000);
    /** Map area inside the margin per system; 24 systems give about the size every map used to have. */
    static final double AREA_PER_SYSTEM = 240_000;
    /** Width to height, as on the earlier fixed map. */
    static final double ASPECT_RATIO = 1.6;
    private static final int ROUNDING = 10;

    /** The map for this many systems at the fixed density. */
    public static GalaxyExtent forSystems(int systemCount) {
        double inner = Math.max(1, systemCount) * AREA_PER_SYSTEM;
        return new GalaxyExtent(rounded(Math.sqrt(inner * ASPECT_RATIO)), rounded(Math.sqrt(inner / ASPECT_RATIO)));
    }

    private static int rounded(double innerLength) {
        return (int) Math.round((innerLength + 2 * GameConfig.SYSTEM_MARGIN) / ROUNDING) * ROUNDING;
    }

    /** Horizontal margin of the systems; small maps keep at least half of their width free for systems. */
    public double marginX() {
        return Math.min(GameConfig.SYSTEM_MARGIN, width / 4.0);
    }

    public double marginY() {
        return Math.min(GameConfig.SYSTEM_MARGIN, height / 4.0);
    }
}
