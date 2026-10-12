package de.zettsystems.starfare.navigation.values;

import de.zettsystems.starfare.game.values.Fleet;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RouteStateTest {
    private static Fleet fleet(FlightJourney.Phase phase) {
        return new Fleet(1, 1, 1, 1, 2, 10, 1, 4,
                new FlightJourney(List.of(1, 2, 3, 4), 1, phase, phase == FlightJourney.Phase.FLYING ? 0 : 5, 10));
    }
    @Test void flyingUsesLegArrivalAndAddsWaitExactlyOnce() {
        var state = RouteState.forFleet(fleet(FlightJourney.Phase.FLYING), true);
        assertThat(state.nextTurn()).isEqualTo(5);
        assertThat(state.arrivalTurn()).isEqualTo(11);
        assertThat(state.atStation()).isFalse();
    }
    @Test void stationUsesReadyTimeWhileBlockedHasNoArrivalPromise() {
        var station = RouteState.forFleet(fleet(FlightJourney.Phase.STATION), true);
        assertThat(station.nextTurn()).isEqualTo(6); assertThat(station.atStation()).isTrue();
        var blocked = RouteState.forFleet(fleet(FlightJourney.Phase.BLOCKED), true);
        assertThat(blocked.nextTurn()).isNull(); assertThat(blocked.arrivalTurn()).isNull();
        assertThat(blocked.blocked()).isTrue();
    }
    @Test void partnerAndEvacuationDoNotPromiseAnOldFinalArrival() {
        var partner = new Fleet(1, 1, 1, 1, 2, 10, 1, 4,
                new FlightJourney(List.of(1, 2), 1, FlightJourney.Phase.PARTNER, 5, 4));
        assertThat(RouteState.forFleet(partner, false).arrivalTurn()).isNull();
        assertThat(RouteState.forFleet(partner.requireReturn(), false).arrivalTurn()).isNull();
    }
    @Test void classicTimingStaysDirect() {
        var classic = new Fleet(1, 1, 1, 1, 2, 10, 1, 4);
        assertThat(RouteState.forFleet(classic, true).arrivalTurn()).isEqualTo(5);
        assertThat(RouteState.forFleet(classic, false).systems()).containsExactly(1, 2);
    }
    @Test void longRoutesAlwaysKeepCurrentNextAndDestinationVisible() {
        var state = new RouteState(List.of(1, 2, 3, 4, 5, 6, 7), 4, true, true, null, null);
        assertThat(state.compactIndices()).containsExactly(0, 4, 5, 6);
        var preview = new RouteState(state.systems(), 0, true, false, null, null);
        assertThat(preview.compactIndices()).containsExactly(0, 1, 6);
    }
}
