package de.zettsystems.starfare.navigation.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.navigation.domain.Routes;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultNavigationService implements NavigationService {
    @Override public void departReadyFleets(GameState state) {
        for (Fleet fleet : List.copyOf(state.fleets())) { departFleet(state, fleet); }
    }
    private static void departFleet(GameState state, Fleet fleet) {
        var journey = fleet.journey();
        if (journey == null || journey.flying() || state.waitThisTurn().contains(fleet.globalId()) || journey.readyTurn() > state.turn()) { return; }
        if (journey.evacuating()) {
            if (java.util.Objects.equals(state.getSystem(fleet.toSystemId()).ownerId(), fleet.ownerId())) {
                state.fleets().remove(fleet);
                state.updateSystem(fleet.toSystemId(), system -> system.reinforce(fleet.ships())); return;
            }
            launchReturn(state, fleet); return;
        }
        if (!journey.hasNextLeg()) { return; }
        int from = journey.stations().get(journey.legIndex());
        int to = journey.stations().get(journey.legIndex() + 1);
        if (!departureAllowed(state, fleet)) { state.replaceFleet(fleet.holdAtBlockedStation()); return; }
        int arrival = state.turn() + state.travelRounds(from, to);
        int finalArrival = arrival + Routes.remainingRounds(state, journey.stations(), journey.legIndex() + 1)
                + (journey.legIndex() + 1 < journey.stations().size() - 1 ? 1 : 0);
        state.replaceFleet(fleet.continueFlight(from, to, state.turn(), arrival, finalArrival));
    }
    private static void launchReturn(GameState state, Fleet fleet) {
        var home = Routes.returnHome(state, fleet.ownerId(), fleet.toSystemId());
        if (home.isPresent()) {
            var route = home.orElseThrow();
            state.replaceFleet(fleet.prepareReturn(route, state.turn(), state.turn() + state.travelRounds(route.systems().getFirst(), route.systems().get(1))));
        }
    }
    @Override public void refreshStationAccess(GameState state, int accessTurn) {
        for (Fleet fleet : List.copyOf(state.fleets())) {
            var journey = fleet.journey();
            if (journey == null || journey.flying() || journey.evacuating()) { continue; }
            if (journey.stationed()) {
                if (!Routes.stationAllowed(state, fleet.ownerId(), fleet.toSystemId())) { state.replaceFleet(fleet.requireReturn()); }
                continue;
            }
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
        if (!journey.hasNextLeg()) { return false; }
        int current = journey.stations().get(journey.legIndex());
        int next = journey.stations().get(journey.legIndex() + 1);
        boolean finalLeg = journey.legIndex() + 1 == journey.stations().size() - 1;
        return Routes.stationAllowed(state, fleet.ownerId(), current) && Routes.withinRange(state, current, next)
                && (finalLeg || Routes.stationAllowed(state, fleet.ownerId(), next));
    }
    @Override public boolean interceptStationArrival(GameState state, Fleet fleet) {
        var journey = fleet.journey();
        if (fleet.evacuating()) {
            if (java.util.Objects.equals(state.getSystem(fleet.toSystemId()).ownerId(), fleet.ownerId()) && !java.util.Objects.requireNonNull(journey).hasNextLeg()) { return false; }
            state.replaceFleet(fleet.dockAtPartner().requireReturn()); return true;
        }
        if (state.allied(fleet.ownerId(), state.getSystem(fleet.toSystemId()).ownerId())) {
            state.replaceFleet(fleet.dockAtPartner()); return true;
        }
        if (journey == null || !journey.hasNextLeg() || !Routes.stationAllowed(state, fleet.ownerId(), fleet.toSystemId())) { return false; }
        state.replaceFleet(fleet.landForRefueling()); return true;
    }
}
