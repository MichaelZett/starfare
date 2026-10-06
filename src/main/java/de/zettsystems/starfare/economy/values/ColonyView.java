package de.zettsystems.starfare.economy.values;

/** Visible only with full system information; population uses hundredths of a labor unit. */
public record ColonyView(SystemQuality quality, int population, int nextPopulation) {
    public int labor() { return population / 100; }
    public int populationLimit() { return quality.suitability().laborLimit() * 100; }
}
