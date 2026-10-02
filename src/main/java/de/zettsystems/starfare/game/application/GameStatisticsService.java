package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.PlayerStatistics;

/** Stores game outcomes independently from removable detailed sessions. */
public interface GameStatisticsService {
    void recordFinishedGame(GameId gameId);

    PlayerStatistics statisticsFor(String account);

    default PlayerStatistics statisticsFor(String account, @org.jspecify.annotations.Nullable String variant) {
        if (variant != null) { throw new UnsupportedOperationException("Variant filtering is not implemented"); }
        return statisticsFor(account);
    }
}
