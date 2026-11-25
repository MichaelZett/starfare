package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.domain.OrderPlanning;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnEvent.JointDeparture.Outcome;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The tricky ways ships leave a system: every ship is counted once, in the garrison or in a fleet. */
class FleetMovementConservationTest {
    private static final int FAR = 1;
    private static final int NEAR = 2;
    private static final int TARGET = 3;
    private static final int ENEMY = 4;

    private final DefaultFleetService fleets = new DefaultFleetService();
    private final DefaultReportService reports = new DefaultReportService();
    /** Every die favours the defender, so battles have fixed results. */
    private final TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reports, () -> 0.5),
            new DefaultAiService(), reports, fleets);
    private final GameState state = new GameState();

    FleetMovementConservationTest() {
        state.players().addAll(List.of(new Player(1, "P1", false, "#ffffff"), new Player(2, "P2", false, "#000000")));
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
        // Four rounds from Far to Target, two from Near; Enemy lies next to Near.
        state.systems().addAll(List.of(new StarSystem(FAR, "Far", 0, 0, 1, 20, 1, false),
                new StarSystem(NEAR, "Near", 480, 0, 1, 10, 1, false),
                new StarSystem(TARGET, "Target", 640, 0, 2, 1, 1, false),
                new StarSystem(ENEMY, "Enemy", 480, 160, 2, 40, 1, false),
                new StarSystem(5, "Rim", 3000, 1800, 2, 9, 1, false),
                new StarSystem(6, "Edge", 2900, 1800, 2, 9, 1, false)));
        state.start();
    }

    private List<FleetDispatch> attack() {
        return List.of(new FleetDispatch(FAR, 10), new FleetDispatch(NEAR, 10));
    }

    @Test
    void theSameJointAttackTwiceCannotReserveTheSameShipsTwice() {
        assertThat(OrderPlanning.jointAttack(state, 1, TARGET, attack())).isTrue();

        assertThat(OrderPlanning.jointAttack(state, 1, TARGET, attack()))
                .as("Near holds 10 ships, all of them already reserved").isFalse();
        assertThat(state.reservedShips(1, NEAR)).isEqualTo(10);
        assertThat(state.pendingOrders().get(1)).hasSize(1);
    }

    @Test
    void aReservationLimitsEveryOtherSendFromTheSameSystem() {
        assertThat(OrderPlanning.jointAttack(state, 1, TARGET, List.of(new FleetDispatch(FAR, 10), new FleetDispatch(NEAR, 6))))
                .isTrue();

        assertThat(fleets.queueSend(state, 1, NEAR, ENEMY, 5)).isFalse();
        assertThat(fleets.queueSend(state, 1, NEAR, ENEMY, 4)).isTrue();
        assertThat(OrderPlanning.replace(state, 1, List.of(new FleetOrder.Send(1, FAR, TARGET, 10),
                new FleetOrder.Send(1, NEAR, ENEMY, 5)))).as("a split may not take reserved ships").isFalse();
        assertThat(OrderPlanning.jointAttack(state, 1, ENEMY, List.of(new FleetDispatch(NEAR, 1), new FleetDispatch(FAR, 1))))
                .as("nothing is left at Near").isFalse();
        long before = ShipLedger.total(state);

        engine.advanceTurn(state);

        assertThat(state.getSystem(NEAR).garrison()).as("6 reserved stay, 4 left, 1 built").isEqualTo(7);
        assertThat(ShipLedger.total(state)).as("production only").isEqualTo(before + 6);
    }

    @Test
    void aStartAfterAnAttackOnItsSourceSendsOnlyTheSurvivors() {
        assertThat(OrderPlanning.jointAttack(state, 1, TARGET, attack())).isTrue();
        // The enemy strikes Near before its start: 8 attackers against 10 defenders leave 2.
        state.fleets().add(new Fleet(90, 2, 1, ENEMY, NEAR, 8, state.turn(), state.turn() + 1));
        engine.advanceTurn(state);
        int survivors = state.getSystem(NEAR).garrison();
        assertThat(survivors).isBetween(1, 9);
        long before = ShipLedger.total(state);

        assertThat(fleets.queueSend(state, 1, NEAR, FAR, 1)).as("the reservation still blocks Near").isFalse();
        engine.advanceTurn(state);
        engine.advanceTurn(state);

        assertThat(state.fleets()).filteredOn(fleet -> fleet.fromSystemId() == NEAR).singleElement()
                .extracting(Fleet::ships).isEqualTo(survivors + 1);
        assertThat(state.getSystem(NEAR).garrison()).isEqualTo(1);
        assertThat(state.reports().get(1).events()).contains(new TurnEvent.JointDeparture(1, NEAR, "Near", TARGET,
                "Target", 10, survivors + 1, Outcome.REDUCED));
        assertThat(ShipLedger.total(state)).as("two rounds of production, nothing else").isEqualTo(before + 2L * 6);
    }

    @Test
    void cancellingAStartFreesItsShipsAndUndoNeverBooksThemTwice() {
        assertThat(OrderPlanning.jointAttack(state, 1, TARGET, attack())).isTrue();
        var departure = state.departuresOf(1).getFirst();

        assertThat(OrderPlanning.cancelDeparture(state, 1, departure)).isTrue();
        assertThat(state.reservedShips(1, NEAR)).isZero();
        assertThat(fleets.queueSend(state, 1, NEAR, ENEMY, 10)).as("the cancelled reservation is free").isTrue();
        assertThat(state.undoOrders(1)).as("undoing the send is fine").isTrue();
        assertThat(state.undoOrders(1)).as("one step only").isFalse();
        assertThat(state.reservedShips(1, NEAR)).isZero();
        assertThat(fleets.queueSend(state, 1, NEAR, ENEMY, 10)).isTrue();
        assertThat(OrderPlanning.cancelDeparture(state, 1, departure)).as("already gone").isFalse();
    }

    @Test
    void undoOfACancelledStartReservesItsShipsAgain() {
        assertThat(OrderPlanning.jointAttack(state, 1, TARGET, attack())).isTrue();
        assertThat(OrderPlanning.cancelDeparture(state, 1, state.departuresOf(1).getFirst())).isTrue();

        assertThat(state.undoOrders(1)).isTrue();

        assertThat(state.reservedShips(1, NEAR)).isEqualTo(10);
        assertThat(fleets.queueSend(state, 1, NEAR, ENEMY, 1)).isFalse();
    }

    @Test
    void relocationsAndSendsNeverTakeMoreThanGarrisonAndProduction() {
        state.updateSystem(NEAR, system -> system.afterDefense(5));
        assertThat(fleets.addStandingOrder(state, 1, NEAR, FAR, 1)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, FAR, NEAR, 3)).isNegative();
        assertThat(fleets.queueSend(state, 1, NEAR, ENEMY, 5)).isTrue();
        long before = ShipLedger.total(state);

        engine.advanceTurn(state);

        assertThat(state.getSystem(NEAR).garrison()).isZero();
        assertThat(state.fleets()).filteredOn(fleet -> fleet.fromSystemId() == NEAR)
                .extracting(Fleet::ships).containsExactlyInAnyOrder(5, 1);
        assertThat(ShipLedger.total(state)).as("production only").isEqualTo(before + 6);
    }

    @Test
    void severalRelocationsFromOneSystemShareWhatIsThere() {
        state.updateSystem(NEAR, system -> system.afterDefense(0));
        // A relocation into Near raises its limit to two, but the incoming ship is still under way.
        assertThat(fleets.addStandingOrder(state, 1, FAR, NEAR, 1)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, NEAR, FAR, 1)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, NEAR, ENEMY, 1)).isPositive();
        long before = ShipLedger.total(state);

        engine.advanceTurn(state);

        assertThat(state.getSystem(NEAR).garrison()).isZero();
        assertThat(state.fleets()).filteredOn(fleet -> fleet.fromSystemId() == NEAR)
                .extracting(Fleet::ships).containsExactly(1);
        assertThat(ShipLedger.total(state)).isEqualTo(before + 6);
    }
}
