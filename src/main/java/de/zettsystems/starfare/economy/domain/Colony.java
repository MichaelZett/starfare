package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.SystemQuality;

/** Population is measured in hundredths of a labor unit; growth never rounds to zero. */
public record Colony(int population) {
    public static final int POPULATION_PER_LABOR = 100;
    public Colony {
        if (population < POPULATION_PER_LABOR || population > 50 * POPULATION_PER_LABOR) {
            throw new IllegalArgumentException("Invalid colony population");
        }
    }
    public int labor() { return population / POPULATION_PER_LABOR; }
    public Colony grow(SystemQuality quality) {
        int limit = quality.suitability().laborLimit() * POPULATION_PER_LABOR;
        if (population >= limit) { return this; }
        int increase = Math.ceilDiv(population * quality.suitability().growthPercent(), 100);
        return new Colony(Math.min(limit, population + increase));
    }
}
