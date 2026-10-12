package de.zettsystems.starfare.economy.values;

/** Current stock and the empire-wide next-round forecast, in whole metal units. */
public record MetalBalance(long current, long extraction, long recycling, long consumption, long next) { }
