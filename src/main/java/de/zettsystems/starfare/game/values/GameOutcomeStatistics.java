package de.zettsystems.starfare.game.values;

/** Personal totals shown when a game ends. */
public record GameOutcomeStatistics(int rounds, int systems, int shipsBuilt, int shipsDestroyed, int shipsLost) {}
