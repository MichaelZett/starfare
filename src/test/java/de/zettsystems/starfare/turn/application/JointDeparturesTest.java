package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.domain.OrderPlanning;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.domain.Routes;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnEvent.JointDeparture.Outcome;
import de.zettsystems.starfare.testsupport.NavigationStates;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Planned later starts of a joint attack during round resolution. */
class JointDeparturesTest {
    private static final int FAR = 1;
    private static final int NEAR = 2;
    private static final int TARGET = 3;

    private final DefaultFleetService fleets = new DefaultFleetService();
    private final DefaultReportService reports = new DefaultReportService();
    private final TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reports), new DefaultAiService(),
            reports, fleets);
    private final GameState state = new GameState();
    private final int start;

    JointDeparturesTest() {
        state.players().addAll(List.of(new Player(1, "P1", false, "#ffffff"), new Player(2, "P2", false, "#000000")));
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
        // Four rounds from Far to Target, two from Near: Near starts two rounds after Far.
        state.systems().addAll(List.of(new StarSystem(FAR, "Far", 0, 0, 1, 10, 1, false),
                new StarSystem(NEAR, "Near", 480, 0, 1, 10, 1, false),
                new StarSystem(TARGET, "Target", 640, 0, 2, 1, 1, false)));
        state.start();
        start = state.turn();
        assertThat(OrderPlanning.jointAttack(state, 1, TARGET, List.of(new FleetDispatch(FAR, 10), new FleetDispatch(NEAR, 10))))
                .isTrue();
    }

    @Test
    void theFasterSourceDefendsUntilItsStartAndThenArrivesTogether() {
        assertThat(fleets.queueSend(state, 1, NEAR, TARGET, 1)).as("reserved ships cannot be sent twice").isFalse();

        engine.advanceTurn(state);
        assertThat(state.fleets()).extracting(Fleet::fromSystemId).containsExactly(FAR);
        assertThat(state.getSystem(NEAR).garrison()).as("still in the garrison, plus production").isEqualTo(11);
        assertThat(fleets.queueSend(state, 1, NEAR, TARGET, 2)).isFalse();
        assertThat(fleets.queueSend(state, 1, NEAR, TARGET, 1)).as("production beyond the reservation is free").isTrue();
        state.pendingOrders().clear();

        engine.advanceTurn(state);
        assertThat(state.scheduledDepartures()).hasSize(1);
        engine.advanceTurn(state);

        assertThat(state.scheduledDepartures()).isEmpty();
        assertThat(state.fleets()).extracting(Fleet::fromSystemId, Fleet::ships, Fleet::arrivalTurn)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(FAR, 10, start + 4),
                        org.assertj.core.groups.Tuple.tuple(NEAR, 10, start + 4));
        assertThat(state.getSystem(NEAR).garrison()).isEqualTo(3);
    }

    @Test
    void aLostSourceCancelsItsStartRightAfterTheBattlesOfThatRound() {
        state.updateSystem(NEAR, system -> system.captureBy(2, 1));

        engine.advanceTurn(state);

        assertThat(state.scheduledDepartures()).isEmpty();
        assertThat(state.reports().get(1).events()).contains(
                new TurnEvent.JointDeparture(1, NEAR, "Near", TARGET, "Target", 10, 0, Outcome.SOURCE_LOST));
    }

    @Test
    void fewerShipsLaunchWhatIsLeft() {
        engine.advanceTurn(state);
        state.updateSystem(NEAR, system -> system.launchFleet(7));
        engine.advanceTurn(state);
        engine.advanceTurn(state);

        assertThat(state.fleets()).filteredOn(fleet -> fleet.fromSystemId() == NEAR).singleElement()
                .extracting(Fleet::ships).isEqualTo(5);
        assertThat(state.reports().get(1).events()).contains(
                new TurnEvent.JointDeparture(1, NEAR, "Near", TARGET, "Target", 10, 5, Outcome.REDUCED));
    }

    @Test
    void anEmptyGarrisonCancelsTheStart() {
        engine.advanceTurn(state);
        engine.advanceTurn(state);
        state.updateSystem(NEAR, system -> system.launchFleet(system.garrison()));
        engine.advanceTurn(state);

        assertThat(state.fleets()).noneMatch(fleet -> fleet.fromSystemId() == NEAR);
        assertThat(state.scheduledDepartures()).isEmpty();
        assertThat(state.reports().get(1).events()).contains(
                new TurnEvent.JointDeparture(1, NEAR, "Near", TARGET, "Target", 10, 0, Outcome.NO_SHIPS));
    }

    @Test
    void aTargetThatBecameOwnIsStillFlownToAsReinforcement() {
        state.updateSystem(TARGET, system -> system.captureBy(1, 5));
        // Enough enemy systems elsewhere that taking the target does not end the game.
        state.systems().addAll(List.of(new StarSystem(4, "Rim", 3000, 1800, 2, 9, 1, false),
                new StarSystem(5, "Edge", 2900, 1800, 2, 9, 1, false)));

        engine.advanceTurn(state);
        engine.advanceTurn(state);
        engine.advanceTurn(state);

        assertThat(state.fleets()).filteredOn(fleet -> fleet.fromSystemId() == NEAR).singleElement()
                .extracting(Fleet::toSystemId, Fleet::ships).containsExactly(TARGET, 10);
    }

    @Test
    void anUnusedScheduleLeavesTheRoundUntouched() {
        GameState plain = new GameState();
        new JointDepartures(fleets, reports).launchDue(plain);
        new JointDepartures(fleets, reports).cancelLost(plain);
        assertThat(plain.reports()).isEmpty();
        assertThat(plain.fleets()).isEmpty();
    }

    @Test
    void anExtendedStartWithoutARouteIsCancelled() {
        GameState extended = NavigationStates.running();
        int beta = 2;
        int delta = 4;
        int alphaRounds = Routes.plan(extended, 1, 1, delta).orElseThrow().rounds();
        int betaRounds = Routes.plan(extended, 1, beta, delta).orElseThrow().rounds();
        assertThat(alphaRounds).isGreaterThan(betaRounds);
        assertThat(OrderPlanning.jointAttack(extended, 1, delta, List.of(new FleetDispatch(1, 10), new FleetDispatch(beta, 5))))
                .isTrue();
        assertThat(extended.scheduledDepartures()).singleElement()
                .matches(departure -> departure.departureTurn() == extended.turn() + alphaRounds - betaRounds);
        // The only station between Beta and Delta changes hands.
        extended.updateSystem(3, system -> system.captureBy(2, 1));
        for (int round = 0; round < alphaRounds - betaRounds; round++) { extended.nextTurn(); }

        new JointDepartures(fleets, reports).launchDue(extended);

        assertThat(extended.scheduledDepartures()).isEmpty();
        assertThat(extended.fleets()).isEmpty();
        assertThat(extended.reports().get(1).events()).containsExactly(
                new TurnEvent.JointDeparture(1, beta, "Beta", delta, "Delta", 5, 0, Outcome.NO_ROUTE));
    }
}
