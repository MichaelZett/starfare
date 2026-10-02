package de.zettsystems.starfare.economy.values;

/** Ships actually produced this turn, before next turn's expanded capacity takes effect. */
public record IndustrialProduction(int ownerId, int systemId, String systemName, int ships) { }
