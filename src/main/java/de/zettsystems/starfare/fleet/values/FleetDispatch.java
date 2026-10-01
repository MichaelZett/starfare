package de.zettsystems.starfare.fleet.values;

/** One source and its explicitly previewed amount in a batch dispatch. */
public record FleetDispatch(int sourceId, int ships) { }
