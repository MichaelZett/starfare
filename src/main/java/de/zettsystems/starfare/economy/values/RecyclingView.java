package de.zettsystems.starfare.economy.values;

/** Exact personal recycling plan and the saved fractional empire stock. */
public record RecyclingView(int ships, int available, int fraction) { }
