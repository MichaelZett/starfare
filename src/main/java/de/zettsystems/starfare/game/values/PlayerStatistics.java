package de.zettsystems.starfare.game.values;

import java.util.List;

/** Persisted-game statistics visible to one player. */
public record PlayerStatistics(int games, int wins, int losses, List<OpponentStatistics> opponents,
                               List<CompletedGameStatistics> completedGames) {
    public PlayerStatistics {
        opponents = List.copyOf(opponents);
        completedGames = List.copyOf(completedGames);
    }

    public PlayerStatistics(int games, int wins, int losses, List<OpponentStatistics> opponents) {
        this(games, wins, losses, opponents, List.of());
    }

    public static PlayerStatistics empty() {
        return new PlayerStatistics(0, 0, 0, List.of(), List.of());
    }
}
