package de.zettsystems.starfare.economy.values;

/** Visible only with full system information; population uses hundredths of a labor unit. */
public record ColonyView(SystemQuality quality, int population, int nextPopulation, boolean laborLimited) {
    public ColonyView(SystemQuality quality, int population, int nextPopulation) { this(quality, population, nextPopulation, true); }
    public int labor() { return population / 100; }
    public int populationLimit() { return quality.suitability().laborLimit() * 100; }
}
