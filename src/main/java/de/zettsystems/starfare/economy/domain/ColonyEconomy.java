package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.SystemQuality;

/** Immutable colony and its saved environment; conquest changes neither. */
public record ColonyEconomy(SystemQuality quality, Colony colony) {
    public ColonyEconomy {
        if (colony.population() > quality.suitability().laborLimit() * Colony.POPULATION_PER_LABOR) {
            throw new IllegalArgumentException("Population exceeds its environment limit");
        }
    }
    public int usableCapacity(Industry industry) { return Math.min(industry.capacity(), colony.labor()); }
    public int shipbuilding(Industry industry) { return usableCapacity(industry) - industry.expansionAllocation(); }
    /** Without a labor limit the colony is atmosphere only and all installed industry is usable. */
    public int usableCapacity(Industry industry, boolean laborLimited) {
        return laborLimited ? usableCapacity(industry) : industry.capacity();
    }
    public int shipbuilding(Industry industry, boolean laborLimited) {
        return usableCapacity(industry, laborLimited) - industry.expansionAllocation();
    }
    public ColonyEconomy grow() { return new ColonyEconomy(quality, colony.grow(quality)); }
}
