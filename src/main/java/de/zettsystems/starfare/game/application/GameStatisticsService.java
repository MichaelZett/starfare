package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.PlayerStatistics;

/** Stores game outcomes independently from removable detailed sessions. */
public interface GameStatisticsService {
    void recordFinishedGame(GameId gameId);

    PlayerStatistics statisticsFor(String account);
}
