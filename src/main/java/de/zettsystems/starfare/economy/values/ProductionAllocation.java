package de.zettsystems.starfare.economy.values;

/** Client command bound to the round visible when its editor was opened. */
public record ProductionAllocation(int systemId, int turn, int expansionPoints) { }
