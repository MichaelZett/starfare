package de.zettsystems.starfare.fleet.domain;

import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.fleet.values.ScheduledDeparture;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class OrderPlanningTest {
    private final GameState state = new GameState();

    OrderPlanningTest() {
        state.systems().addAll(List.of(new StarSystem(1, "A", 0, 0, 1, 15, 3, false),
                new StarSystem(2, "B", 100, 0, 1, 10, 4, false),
                new StarSystem(3, "C", 200, 0, 2, 10, 2, false)));
    }

    @Test
    void jointAttackIsAtomicAndRespectsOwnershipAndCapacity() {
        state.updateSystem(1, system -> system.relocateTo(-400, 0)); // four rounds to C, B needs two
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 5), new FleetDispatch(2, 11)))).isFalse();
        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.scheduledDepartures()).isEmpty();
        assertThat(OrderPlanning.jointAttack(state, 1, 2, List.of(new FleetDispatch(3, 1)))).isFalse();
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 16)))).isFalse();
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 15), new FleetDispatch(2, 10)))).isTrue();
        assertThat(state.pendingOrders().get(1)).as("the slowest source leaves now")
                .containsExactly(new FleetOrder.Send(1, 1, 3, 15));
        assertThat(state.scheduledDepartures()).as("the faster one starts two rounds later and arrives together")
                .containsExactly(new ScheduledDeparture(1, 2, 3, 10, state.turn() + 2, state.turn() + 4, null));
        assertThat(state.getSystem(1).garrison()).isEqualTo(15);
        assertThat(state.getSystem(2).garrison()).isEqualTo(10);
    }

    @Test
    void duplicateSourceSelfTargetAndMissingTargetLeaveOrdersUnchanged() {
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 2), new FleetDispatch(1, 2)))).isFalse();
        assertThat(OrderPlanning.jointAttack(state, 1, 1, List.of(new FleetDispatch(1, 2)))).isFalse();
        assertThat(OrderPlanning.jointAttack(state, 1, 99, List.of(new FleetDispatch(1, 2)))).isFalse();
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of())).isFalse();
        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.scheduledDepartures()).isEmpty();
    }

    @Test
    void editCreditsExistingAmountAndRejectsStaleEditor() {
        var fleets = new DefaultFleetService();
        assertThat(fleets.queueSend(state, 1, 1, 3, 12)).isTrue();
        PlannedOrder old = new PlannedOrder(0, "map.orderType.send", 1, 3, "A", "C", 12, false, null);
        assertThat(OrderPlanning.edit(state, 1, old, 2, 15)).isTrue();
        assertThat(OrderPlanning.edit(state, 1, old, 3, 1)).isFalse();
        assertThat(state.pendingOrders().get(1)).containsExactly(new FleetOrder.Send(1, 1, 2, 15));
        assertThat(state.undoOrders(1)).isTrue();
        assertThat(state.pendingOrders().get(1)).containsExactly(new FleetOrder.Send(1, 1, 3, 12));
        assertThat(state.undoOrders(1)).isFalse();
    }

    @Test
    void editOfAnIndexBehindTheLastOrderIsRejectedWithoutFailing() {
        assertThat(new DefaultFleetService().queueSend(state, 1, 1, 3, 12)).isTrue();
        PlannedOrder behind = new PlannedOrder(1, "map.orderType.send", 1, 3, "A", "C", 12, false, null);

        assertThat(OrderPlanning.edit(state, 1, behind, 2, 1)).isFalse();
        assertThat(state.pendingOrders().get(1)).containsExactly(new FleetOrder.Send(1, 1, 3, 12));
    }

    @Test
    void replacementWithAnEmptySendIsRejected() {
        assertThat(OrderPlanning.replace(state, 1, List.of(new FleetOrder.Send(1, 2, 3, 0)))).isFalse();
        assertThat(OrderPlanning.replace(state, 1, List.of(new FleetOrder.Send(1, 2, 3, 1)))).isTrue();
        assertThat(state.pendingOrders().get(1)).containsExactly(new FleetOrder.Send(1, 2, 3, 1));
    }

    @Test
    void undoIsPersonalSurvivesSnapshotAndExpiresAtTurnBoundary() {
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 5)))).isTrue();
        assertThat(OrderPlanning.jointAttack(state, 2, 1, List.of(new FleetDispatch(3, 5)))).isTrue();
        GameState restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        assertThat(restored.undoOrders(1)).isTrue();
        assertThat(restored.pendingOrders().get(1)).isEmpty();
        assertThat(restored.pendingOrders().get(2)).hasSize(1);
        restored.nextTurn();
        assertThat(restored.undoOrders(2)).isFalse();
    }

    @Test
    void plannedStartsReserveShipsCanBeCancelledAndUndoRestoresThem() {
        state.updateSystem(1, system -> system.relocateTo(-400, 0));
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 5), new FleetDispatch(2, 8)))).isTrue();
        ScheduledDeparture planned = state.scheduledDepartures().getFirst();
        var fleets = new DefaultFleetService();
        assertThat(state.reservedShips(1, 2)).isEqualTo(8);
        assertThat(fleets.queueSend(state, 1, 2, 3, 3)).isFalse();
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(2, 3))))
                .as("a second joint attack cannot take the reserved ships").isFalse();

        assertThat(OrderPlanning.cancelDeparture(state, 2, planned)).as("only the planner cancels").isFalse();
        assertThat(OrderPlanning.cancelDeparture(state, 1, planned)).isTrue();
        assertThat(state.reservedShips(1, 2)).isZero();
        assertThat(OrderPlanning.cancelDeparture(state, 1, planned)).isFalse();
        assertThat(state.undoOrders(1)).isTrue();
        assertThat(state.scheduledDepartures()).containsExactly(planned);
        assertThat(state.undoOrders(1)).isFalse();
    }

    @Test
    void undoOfAJointAttackTakesBackItsSendsAndItsPlannedStarts() {
        state.updateSystem(1, system -> system.relocateTo(-400, 0));
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 5), new FleetDispatch(2, 8)))).isTrue();
        assertThat(state.undoOrders(1)).isTrue();
        assertThat(state.pendingOrders().get(1)).isEmpty();
        assertThat(state.scheduledDepartures()).isEmpty();
    }

    @Test
    void plannedStartsSurviveSnapshotAndCopyAndOldSnapshotsHaveNone() {
        state.updateSystem(1, system -> system.relocateTo(-400, 0));
        assertThat(OrderPlanning.jointAttack(state, 1, 3, List.of(new FleetDispatch(1, 5), new FleetDispatch(2, 8)))).isTrue();
        var snapshot = GameState.toSnapshot(state);
        GameState restored = GameState.fromSnapshot(snapshot);
        assertThat(restored.scheduledDepartures()).isEqualTo(state.scheduledDepartures());
        assertThat(GameState.copyOf(state).scheduledDepartures()).isEqualTo(state.scheduledDepartures());
        assertThat(restored.undoOrders(1)).as("the undo step keeps the planned starts as well").isTrue();
        assertThat(restored.scheduledDepartures()).isEmpty();

        var legacy = new de.zettsystems.starfare.game.domain.GameStateSnapshot(snapshot.turn(), snapshot.nextGlobalFleetId(),
                snapshot.nextLocalFleetNo(), snapshot.players(), snapshot.systems(), snapshot.fleets(), snapshot.reports(),
                snapshot.intel(), snapshot.waitThisTurn(), snapshot.submittedThisTurn(), snapshot.gameOver(),
                snapshot.winnerId(), snapshot.started(), snapshot.joinedHumanPlayerIds(), snapshot.originalHumanPlayerIds(),
                snapshot.observers(), snapshot.seatByUser(), snapshot.invitedSeats(), snapshot.pendingOrders(),
                snapshot.standingOrders(), snapshot.nextStandingOrderId(), snapshot.observersAllowed(),
                snapshot.reentryAllowed(), snapshot.turnStartedAt(), snapshot.visibility(), snapshot.finishedAt(),
                snapshot.ownershipHistory(), snapshot.replayFrames());
        assertThat(GameState.fromSnapshot(legacy).scheduledDepartures()).isEmpty();
    }
}
