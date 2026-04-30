package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.game.domain.GameState;

public interface TurnEngine {
    void advanceTurn(GameState state);
}
