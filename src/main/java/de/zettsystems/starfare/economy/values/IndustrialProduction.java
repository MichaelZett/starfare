package de.zettsystems.starfare.economy.values;

/**
 * Ships actually produced this turn, before next turn's expanded capacity takes effect, with the
 * metal actually mined and booked for them and the capacity that construction finished.
 */
public record IndustrialProduction(int ownerId, int systemId, String systemName, int ships,
                                   long metalMined, long metalConsumed, int capacityGained) {
    public IndustrialProduction(int ownerId, int systemId, String systemName, int ships) {
        this(ownerId, systemId, systemName, ships, 0, 0, 0);
    }
}
