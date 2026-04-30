package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlayerViewState;

/**
 * Builds immutable {@link PlayerViewState} snapshots from a {@link GameState} for a
 * specific player (fog-of-war filtered) or for an all-seeing observer.
 */
interface PlayerViewBuilder {

    PlayerViewState forPlayer(GameState state, int playerId);

    PlayerViewState forObserver(GameState state);
}
