package de.zettsystems.starfare.navigation.domain;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.domain.OrderPlanning;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.navigation.values.*;
import de.zettsystems.starfare.testsupport.NavigationStates;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class RoutesTest {
    @Test void boundaryIsInclusiveAndUnknownOrForeignSourceIsRejected() {
        GameState state = NavigationStates.running();
        assertThat(Routes.plan(state, 1, 1, 2).orElseThrow().systems()).containsExactly(1, 2);
        assertThat(Routes.withinRange(state, 1, 2)).isTrue();
        state.updateSystem(2, system -> system.relocateTo(200.001, 100));
        assertThat(Routes.withinRange(state, 1, 2)).isFalse();
        assertThat(Routes.plan(state, 1, 1, 8)).isEmpty();
        assertThat(Routes.plan(state, 1, 4, 1)).isEmpty();
        assertThat(Routes.plan(state, 1, 1, 99)).isEmpty();
        assertThat(Routes.plan(state, 1, 1, 1)).isEmpty();
    }
    @Test void routeUsesOwnedStationsAndIncludesFullRoundAtEveryStop() {
        GameState state = NavigationStates.running();
        var route = Routes.plan(state, 1, 1, 4).orElseThrow();
        assertThat(route.systems()).containsExactly(1, 2, 3, 4);
        assertThat(route.rounds()).isEqualTo(8);
        assertThat(route.legRounds()).containsExactly(2, 2, 2);
        assertThat(route.description()).isEqualTo("Alpha → Beta → Gamma → Delta");
        state.updateSystem(2, system -> system.captureBy(2, 1));
        assertThat(Routes.plan(state, 1, 1, 4)).isEmpty();
        assertThat(Routes.plan(state, 1, 1, 2)).isPresent();
        state.updateSystem(2, system -> system.captureBy(1, 1));
        assertThat(Routes.plan(state, 1, 1, 4)).isPresent();
    }
    @Test void directClassicFlightsStayUnlimitedAndKeepTheOldTravelTime() {
        GameState state = new GameState(); state.players().add(new Player(1, "Human", false, "#111111"));
        state.systems().addAll(List.of(new StarSystem(1, "A", 0, 0, 1, 10, 1, false), new StarSystem(2, "B", 3000, 1800, null, 1, 1, true)));
        state.start(); var route = Routes.plan(state, 1, 1, 2).orElseThrow();
        assertThat(route.systems()).containsExactly(1, 2); assertThat(route.rounds()).isEqualTo(state.travelRounds(1, 2));
        assertThat(state.navigationSettings()).isEmpty();
        assertThat(new DefaultFleetService().sendFleet(state, 1, 1, 2, 3)).isTrue();
        assertThat(state.fleets().getFirst().journey()).isNull();
    }
    @Test void invalidRoutesCannotMutateLaunchesTransfersBatchEditsOrUndo() {
        GameState state = NavigationStates.running(); var fleets = new DefaultFleetService();
        var before = GameState.toSnapshot(state);
        assertThat(fleets.sendFleet(state, 1, 1, 5, 10)).isFalse();
        assertThat(fleets.queueSend(state, 1, 1, 5, 10)).isFalse();
        assertThat(fleets.addStandingOrder(state, 1, 1, 5, 2)).isNegative();
        assertThat(OrderPlanning.dispatch(state, 1, 5, List.of(new FleetDispatch(1, 10)))).isFalse();
        assertThatThrownBy(() -> state.addFleet(1, 1, 5, 10)).isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(fleets.queueSend(state, 1, 1, 4, 10)).isTrue();
        var expected = new PlannedOrder(0, "map.orderType.send", 1, 4, "Alpha", "Delta", 10, false, null, 9);
        var queued = GameState.toSnapshot(state);
        assertThat(OrderPlanning.edit(state, 1, expected, 5, 10)).isFalse();
        assertThat(GameState.toSnapshot(state)).isEqualTo(queued);
        state.rememberOrders(1); state.pendingOrders().get(1).clear();
        state.updateSystem(2, system -> system.captureBy(2, 1));
        assertThat(state.undoOrders(1)).isFalse();
        assertThat(state.pendingOrders().get(1)).isEmpty();
    }
    @Test void shorterAlternativeWinsAndTiesUseSystemIds() {
        GameState state = NavigationStates.running();
        state.systems().add(new StarSystem(9, "Shortcut", 200, 100, 1, 1, 4, false));
        assertThat(Routes.plan(state, 1, 1, 4).orElseThrow().systems()).containsExactly(1, 2, 3, 4);
        state.updateSystem(2, system -> system.captureBy(2, 1));
        assertThat(Routes.plan(state, 1, 1, 4).orElseThrow().systems()).containsExactly(1, 9, 3, 4);
    }
    @Test void searchPrefersFewerRoundsOverLowerSystemIdsAndLongerLegs() {
        // 160 map pixels per round; legs of 320 take 2 rounds, legs of about 480 take 3.
        // Beyond precedes Hop in the list, so equally short returns must be ordered by system id.
        GameState state = network(560, new StarSystem(1, "Start", 0, 0, 1, 10, 1, false),
                new StarSystem(2, "Detour", 320, 352, 1, 1, 1, false), new StarSystem(3, "Goal", 640, 0, 1, 1, 1, false),
                new StarSystem(5, "Beyond", 960, 0, 1, 1, 1, false), new StarSystem(4, "Hop", 320, 0, 1, 1, 1, false));
        var route = Routes.plan(state, 1, 1, 3).orElseThrow();
        assertThat(route.systems()).containsExactly(1, 4, 3);
        assertThat(route.rounds()).isEqualTo(5);
        assertThat(Routes.plan(state, 1, 1, 5).orElseThrow().rounds()).isEqualTo(8);
        assertThat(Routes.reachableRounds(state, 1, 1)).containsExactlyInAnyOrderEntriesOf(Map.of(2, 3, 3, 5, 4, 2, 5, 8));
        var home = Routes.returnHome(state, 1, 3).orElseThrow();
        assertThat(home.systems()).containsExactly(3, 4);
        assertThat(home.rounds()).isEqualTo(2);
    }
    @Test void everyIntermediateStopCostsAFullRound() {
        GameState state = network(560, new StarSystem(1, "Start", 0, 0, 1, 10, 1, false),
                new StarSystem(2, "Middle", 480, 0, 1, 1, 1, false), new StarSystem(3, "Near", 320, 0, 1, 1, 1, false),
                new StarSystem(4, "Goal", 960, 0, 1, 1, 1, false), new StarSystem(5, "Far", 640, 0, 1, 1, 1, false));
        var route = Routes.plan(state, 1, 1, 4).orElseThrow();
        assertThat(route.systems()).containsExactly(1, 2, 4);
        assertThat(route.rounds()).isEqualTo(7);
    }
    @Test void unknownSystemsAreNeverWithinRangeEvenWithoutALimit() {
        GameState state = new GameState(); state.players().add(new Player(1, "Human", false, "#111111"));
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 10, 1, false));
        assertThat(Routes.withinRange(state, 1, 99)).isFalse();
        assertThat(Routes.withinRange(state, 99, 1)).isFalse();
    }
    private static GameState network(double range, StarSystem... stations) {
        GameState state = new GameState(); state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.EXTENDED));
        state.players().add(new Player(1, "Navigator", false, "#56b4e9"));
        state.systems().addAll(List.of(stations));
        // Neutral corner far from every station.
        state.systems().add(new StarSystem(99, "Corner", 2560, 1920, null, 1, 1, true));
        state.configureNavigation(new NavigationSettings(range));
        return state;
    }
    @Test void invalidSettingsAndImpossibleJourneyPhasesAreRejected() {
        var single = List.of(1);
        assertThatThrownBy(() -> new FlightJourney(single, 1, FlightJourney.Phase.FLYING, 0, 3)).isInstanceOf(IllegalArgumentException.class);
        var pair = List.of(1, 2);
        assertThatThrownBy(() -> new FlightJourney(pair, 2, FlightJourney.Phase.FLYING, 0, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FlightJourney(pair, 1, FlightJourney.Phase.FLYING, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new FlightJourney(pair, 1, FlightJourney.Phase.FLYING, 0, 1).destination()).isEqualTo(2);
        var three = List.of(1, 2, 3);
        assertThatThrownBy(() -> new FlightJourney(three, 1, FlightJourney.Phase.STATION, 0, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new FlightJourney(three, 1, FlightJourney.Phase.STATION, 1, 3).readyTurn()).isOne();
        assertThatThrownBy(() -> new NavigationSettings(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NavigationSettings(0)).isInstanceOf(IllegalArgumentException.class);
        var input1 = List.of(1, 2);
        assertThatThrownBy(() -> new FlightJourney(input1, 1, FlightJourney.Phase.STATION, 3, 3)).isInstanceOf(IllegalArgumentException.class);
        var input2 = List.of(1, 2, 1);
        assertThatThrownBy(() -> new FlightJourney(input2, 1, FlightJourney.Phase.FLYING, 0, 3)).isInstanceOf(IllegalArgumentException.class);
    }
}
