package de.zettsystems.starfare.navigation.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.navigation.domain.Routes;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultNavigationService implements NavigationService {
    @Override public void departReadyFleets(GameState state) {
        for (Fleet fleet : List.copyOf(state.fleets())) {
            var journey = fleet.journey();
            if (journey == null || journey.flying() || state.waitThisTurn().contains(fleet.globalId()) || journey.readyTurn() > state.turn()) { continue; }
            int from = journey.stations().get(journey.legIndex());
            int to = journey.stations().get(journey.legIndex() + 1);
            if (!departureAllowed(state, fleet)) {
                state.replaceFleet(fleet.holdAtBlockedStation()); continue;
            }
            int arrival = state.turn() + state.travelRounds(from, to);
            int finalArrival = arrival + Routes.remainingRounds(state, journey.stations(), journey.legIndex() + 1)
                    + (journey.legIndex() + 1 < journey.stations().size() - 1 ? 1 : 0);
            state.replaceFleet(fleet.continueFlight(from, to, state.turn(), arrival, finalArrival));
        }
    }
    @Override public void refreshStationAccess(GameState state, int accessTurn) {
        for (Fleet fleet : List.copyOf(state.fleets())) {
            var journey = fleet.journey();
            if (journey == null || journey.flying()) { continue; }
            if (!departureAllowed(state, fleet)) {
                state.replaceFleet(fleet.holdAtBlockedStation());
            } else if (journey.blocked()) {
                int ready = Math.max(journey.readyTurn(), accessTurn + 1);
                state.replaceFleet(fleet.resumeAtStation(ready, ready + Routes.remainingRounds(state, journey.stations(), journey.legIndex())));
            }
        }
    }
    private static boolean departureAllowed(GameState state, Fleet fleet) {
        var journey = java.util.Objects.requireNonNull(fleet.journey());
        int current = journey.stations().get(journey.legIndex());
        int next = journey.stations().get(journey.legIndex() + 1);
        boolean finalLeg = journey.legIndex() + 1 == journey.stations().size() - 1;
        return Routes.stationAllowed(state, fleet.ownerId(), current) && Routes.withinRange(state, current, next)
                && (finalLeg || Routes.stationAllowed(state, fleet.ownerId(), next));
    }
    @Override public boolean interceptStationArrival(GameState state, Fleet fleet) {
        var journey = fleet.journey();
        if (journey == null || !journey.hasNextLeg() || !Routes.stationAllowed(state, fleet.ownerId(), fleet.toSystemId())) { return false; }
        state.replaceFleet(fleet.landForRefueling()); return true;
    }
}
