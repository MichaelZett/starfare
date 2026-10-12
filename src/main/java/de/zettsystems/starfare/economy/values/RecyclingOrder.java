package de.zettsystems.starfare.economy.values;

/** Editable command bound to the displayed round. Zero cancels the order. */
public record RecyclingOrder(int systemId, int turn, int ships) { }
