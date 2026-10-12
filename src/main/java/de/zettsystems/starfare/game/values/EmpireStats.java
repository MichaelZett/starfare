package de.zettsystems.starfare.game.values;

/**
 * Aggregated numbers of one player's empire for the header bar: owned systems, their summed
 * production per turn and all ships (garrisons plus fleets in transit).
 */
public record EmpireStats(int systems, int production, int ships,
                          de.zettsystems.starfare.economy.values.@org.jspecify.annotations.Nullable MetalBalance metal) {
    public EmpireStats(int systems, int production, int ships) { this(systems, production, ships, null); }

    public static final EmpireStats NONE = new EmpireStats(0, 0, 0);
}
