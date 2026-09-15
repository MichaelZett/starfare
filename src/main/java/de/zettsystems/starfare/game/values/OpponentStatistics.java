package de.zettsystems.starfare.game.values;

/** Aggregated results against one opponent. */
public record OpponentStatistics(String opponentId, int games, int wins, int losses) {
}
