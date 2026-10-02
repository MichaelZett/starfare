package de.zettsystems.starfare.navigation.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;

public interface NavigationService {
    void departReadyFleets(GameState state);
    void refreshStationAccess(GameState state, int accessTurn);
    default void refreshStationAccess(GameState state) { refreshStationAccess(state, state.turn()); }
    boolean interceptStationArrival(GameState state, Fleet fleet);
}
