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
    @Test void invalidSettingsAndImpossibleJourneyPhasesAreRejected() {
        assertThatThrownBy(() -> new NavigationSettings(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NavigationSettings(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FlightJourney(List.of(1, 2), 1, FlightJourney.Phase.STATION, 3, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FlightJourney(List.of(1, 2, 1), 1, FlightJourney.Phase.FLYING, 0, 3)).isInstanceOf(IllegalArgumentException.class);
    }
}
