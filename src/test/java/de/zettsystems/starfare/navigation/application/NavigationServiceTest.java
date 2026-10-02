package de.zettsystems.starfare.navigation.application;

import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.testsupport.NavigationStates;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import de.zettsystems.starfare.navigation.values.FlightJourney.Phase;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class NavigationServiceTest {
    private final DefaultFleetService fleets = new DefaultFleetService();
    private final DefaultNavigationService navigation = new DefaultNavigationService();
    private final DefaultReportService reports = new DefaultReportService();
    private final RoundPipeline pipeline = new RoundPipeline(new DefaultCombatService(reports), reports, fleets, navigation);
    private final DefaultEconomyService economy = new DefaultEconomyService();
    private void advance(GameState state) { pipeline.advanceTurn(state, () -> { }, routed -> economy.produce(state, routed)); }
    private void through(GameState state, int turn) { while (state.turn() < turn) { advance(state); } }
    @Test void stationDwellNeverMergesWithGarrisonAndFinalEtaMatchesTheResolvedRound() {
        GameState state = NavigationStates.running(); assertThat(fleets.sendFleet(state, 1, 1, 4, 30)).isTrue();
        assertThat(state.fleets().getFirst().journey().finalArrivalTurn()).isEqualTo(9);
        through(state, 3); Fleet docked = state.fleets().getFirst();
        assertThat(docked.journey().phase()).isEqualTo(Phase.STATION);
        assertThat(docked.journey().readyTurn()).isEqualTo(4);
        assertThat(state.getSystem(2).garrison()).isEqualTo(13);
        through(state, 4); assertThat(state.fleets().getFirst().inFlight()).isFalse();
        through(state, 5); assertThat(state.fleets().getFirst().fromSystemId()).isEqualTo(2);
        assertThat(state.fleets().getFirst().toSystemId()).isEqualTo(3);
        through(state, 6); assertThat(state.fleets().getFirst().journey().phase()).isEqualTo(Phase.STATION);
        through(state, 9); assertThat(state.fleets()).isEmpty();
        assertThat(state.reports().get(1).events()).isNotEmpty();
    }
    @Test void waitingAtAStationDelaysDepartureAndFinalArrivalOnce() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 10); through(state, 4);
        int id = state.fleets().getFirst().globalId(); assertThat(fleets.queueWait(state, 1, id)).isTrue();
        advance(state); Fleet waiting = state.fleets().getFirst();
        assertThat(waiting.inFlight()).isFalse(); assertThat(waiting.journey().readyTurn()).isEqualTo(5);
        assertThat(waiting.journey().finalArrivalTurn()).isEqualTo(10);
        advance(state); assertThat(state.fleets().getFirst().inFlight()).isTrue();
        assertThat(state.fleets().getFirst().journey().finalArrivalTurn()).isEqualTo(10);
    }
    @Test void losingADockedStationBlocksDepartureAndRecaptureRestartsRefuelling() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 10); through(state, 4);
        state.updateSystem(2, system -> system.captureBy(2, 1)); advance(state);
        Fleet blocked = state.fleets().getFirst(); assertThat(blocked.journey().phase()).isEqualTo(Phase.BLOCKED);
        assertThat(fleets.queueDisband(state, 1, blocked.globalId())).isFalse();
        through(state, 7); assertThat(state.fleets().getFirst().journey().phase()).isEqualTo(Phase.BLOCKED);
        state.updateSystem(2, system -> system.captureBy(1, 1)); navigation.refreshStationAccess(state);
        Fleet resumed = state.fleets().getFirst(); assertThat(resumed.journey().readyTurn()).isEqualTo(8);
        assertThat(resumed.journey().finalArrivalTurn()).isEqualTo(13);
        advance(state); assertThat(state.fleets().getFirst().inFlight()).isFalse();
        advance(state); assertThat(state.fleets().getFirst().inFlight()).isTrue();
    }
    @Test void recaptureAtTheEndOfARoundStillRequiresOneFullRefuellingRound() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 10); through(state, 4);
        state.updateSystem(2, system -> system.captureBy(2, 1)); advance(state);
        assertThat(state.fleets().getFirst().journey().phase()).isEqualTo(Phase.BLOCKED);
        assertThat(fleets.sendFleet(state, 1, 1, 2, 40)).isTrue(); through(state, 7);
        assertThat(state.getSystem(2).ownerId()).isEqualTo(1);
        assertThat(state.fleets().getFirst().journey().readyTurn()).isEqualTo(8);
        advance(state); assertThat(state.fleets().getFirst().inFlight()).isFalse();
        advance(state); assertThat(state.fleets().getFirst().inFlight()).isTrue();
    }
    @Test void aLostNextStationCannotStartAnUnauthorizedTransitLeg() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 10); through(state, 4);
        state.updateSystem(3, system -> system.captureBy(2, 1)); advance(state);
        Fleet stopped = state.fleets().getFirst();
        assertThat(stopped.journey().phase()).isEqualTo(Phase.BLOCKED);
        assertThat(stopped.toSystemId()).isEqualTo(2); assertThat(stopped.inFlight()).isFalse();
        state.updateSystem(3, system -> system.captureBy(1, 1)); navigation.refreshStationAccess(state);
        assertThat(state.fleets().getFirst().journey().readyTurn()).isEqualTo(6);
        advance(state); assertThat(state.fleets().getFirst().inFlight()).isFalse();
        advance(state); assertThat(state.fleets().getFirst().toSystemId()).isEqualTo(3);
        assertThat(state.fleets().getFirst().inFlight()).isTrue();
    }
    @Test void stationLostInFlightUsesNormalCombatAndEndsTheItineraryThere() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 40);
        state.updateSystem(2, system -> system.captureBy(2, 1)); through(state, 3);
        assertThat(state.fleets()).isEmpty(); assertThat(state.getSystem(2).ownerId()).isEqualTo(1);
        assertThat(state.getSystem(4).ownerId()).isEqualTo(2);
    }
    @Test void pausedTransferKeepsItsOrderAndShipsAndResumesThroughTheSameStations() {
        GameState state = NavigationStates.running(); assertThat(fleets.addStandingOrder(state, 1, 1, 4, 3)).isPositive();
        state.updateSystem(2, system -> system.captureBy(2, 1));
        assertThat(fleets.applyStandingOrdersForProduction(state)).isEmpty();
        economy.produce(state, Map.of()); assertThat(state.getSystem(1).garrison()).isEqualTo(54);
        assertThat(state.standingOrders().get(1)).hasSize(1);
        state.updateSystem(2, system -> system.captureBy(1, 1));
        assertThat(fleets.applyStandingOrdersForProduction(state)).containsEntry(1, 3);
        assertThat(state.fleets().getFirst().journey().stations()).containsExactly(1, 2, 3, 4);
    }
    @Test void disbandingDockedShipsAddsThemToTheCurrentStationOnly() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 10); through(state, 3);
        int before = state.getSystem(2).garrison();
        assertThat(fleets.disbandFleet(state, 1, state.fleets().getFirst().globalId())).isTrue();
        assertThat(state.getSystem(2).garrison()).isEqualTo(before + 10); assertThat(state.fleets()).isEmpty();
    }
}
