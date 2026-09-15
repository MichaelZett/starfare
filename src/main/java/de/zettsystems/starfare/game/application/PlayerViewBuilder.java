package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.ReplayFrame;

/**
 * Builds immutable {@link PlayerViewState} snapshots from a {@link GameState} for a
 * specific player (fog-of-war filtered) or for an all-seeing observer.
 */
interface PlayerViewBuilder {

    PlayerViewState forPlayer(GameState state, int playerId);

    PlayerViewState forReview(GameState state, int playerId, boolean fogOfWar);

    PlayerViewState forObserver(GameState state);

    PlayerViewState forReplay(GameState state, ReplayFrame frame, int playerId);
}
