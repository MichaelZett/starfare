package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;

/** Records the first end of a game and announces it to the platform. */
public interface GameEndRecorder {
    void recordFinishedGame(GameId gameId);
}
