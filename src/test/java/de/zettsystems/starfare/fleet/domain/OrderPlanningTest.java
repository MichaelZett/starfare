package de.zettsystems.starfare.fleet.domain;

import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class OrderPlanningTest {
    private final GameState state = new GameState();

    OrderPlanningTest() {
        state.systems().addAll(List.of(new StarSystem(1, "A", 0, 0, 1, 20, 3, false, 5),
                new StarSystem(2, "B", 100, 0, 1, 10, 4, false),
                new StarSystem(3, "C", 200, 0, 2, 10, 2, false)));
    }

    @Test
    void batchIsAtomicAndRespectsOwnershipCapacityAndReserve() {
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of(new FleetDispatch(1, 5), new FleetDispatch(2, 11)))).isFalse();
        assertThat(state.pendingOrders()).isEmpty();
        assertThat(OrderPlanning.dispatch(state, 1, 2, List.of(new FleetDispatch(3, 1)))).isFalse();
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of(new FleetDispatch(1, 16)))).isFalse();
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of(new FleetDispatch(1, 15), new FleetDispatch(2, 10)))).isTrue();
        assertThat(state.pendingOrders().get(1)).hasSize(2);
        assertThat(state.getSystem(1).garrison()).isEqualTo(20);
    }

    @Test
    void duplicateSourceSelfTargetAndMissingTargetLeaveOrdersUnchanged() {
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of(new FleetDispatch(1, 2), new FleetDispatch(1, 2)))).isFalse();
        assertThat(OrderPlanning.dispatch(state, 1, 1, List.of(new FleetDispatch(1, 2)))).isFalse();
        assertThat(OrderPlanning.dispatch(state, 1, 99, List.of(new FleetDispatch(1, 2)))).isFalse();
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of())).isFalse();
        assertThat(state.pendingOrders()).isEmpty();
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
    void undoIsPersonalSurvivesSnapshotAndExpiresAtTurnBoundary() {
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of(new FleetDispatch(1, 5)))).isTrue();
        assertThat(OrderPlanning.dispatch(state, 2, 1, List.of(new FleetDispatch(3, 5)))).isTrue();
        GameState restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        assertThat(restored.undoOrders(1)).isTrue();
        assertThat(restored.pendingOrders().get(1)).isEmpty();
        assertThat(restored.pendingOrders().get(2)).hasSize(1);
        restored.nextTurn();
        assertThat(restored.undoOrders(2)).isFalse();
    }

    @Test
    void reserveBatchRejectsForeignSystemAndConflictingQueuedShips() {
        assertThat(OrderPlanning.reserves(state, 1, List.of(1, 3), 7, false)).isFalse();
        assertThat(state.getSystem(1).garrisonReserve()).isEqualTo(5);
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of(new FleetDispatch(1, 15)))).isTrue();
        assertThat(OrderPlanning.reserves(state, 1, List.of(1, 2), 8, false)).isFalse();
        assertThat(state.getSystem(2).garrisonReserve()).isZero();
        assertThat(OrderPlanning.reserves(state, 1, List.of(1, 2), 0, true)).isTrue();
        assertThat(state.getSystem(1).garrisonReserve()).isEqualTo(3);
        assertThat(state.getSystem(2).garrisonReserve()).isEqualTo(4);
    }

    @Test
    void undoDoesNotOverruleANewerReserve() {
        assertThat(OrderPlanning.dispatch(state, 1, 3, List.of(new FleetDispatch(1, 15)))).isTrue();
        assertThat(OrderPlanning.replace(state, 1, List.of())).isTrue();
        assertThat(OrderPlanning.reserves(state, 1, List.of(1), 20, false)).isTrue();
        assertThat(state.undoOrders(1)).isFalse();
        assertThat(state.pendingOrders().get(1)).isEmpty();
    }
}
