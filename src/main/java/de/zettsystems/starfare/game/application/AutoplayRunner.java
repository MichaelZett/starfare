package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;

public interface AutoplayRunner {
    void autoplayToEnd(GameId gameId);
}
