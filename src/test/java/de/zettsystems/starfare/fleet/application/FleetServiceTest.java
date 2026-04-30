package de.zettsystems.starfare.fleet.application;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FleetServiceTest {

    private FleetService service;
    private GameState state;

    @BeforeEach
    void setUp() {
        service = new DefaultFleetService();
        state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
        state.systems().add(new StarSystem(2, "S2", 100, 0, null, 3, 2, true));
    }

    // --- sendFleet (immediate, used by AI) ---

    @Test
    void sendFleetConsumesShipsAndCreatesFleet() {
        boolean ok = service.sendFleet(state, 1, 1, 2, 4);

        assertThat(ok).isTrue();
        assertThat(state.getSystem(1).garrison()).isEqualTo(6);
        assertThat(state.fleets()).hasSize(1);
        assertThat(state.fleets().getFirst().ships()).isEqualTo(4);
    }

    // --- queueSend ---

    @Test
    void queueSendDoesNotMutateGarrison() {
        boolean ok = service.queueSend(state, 1, 1, 2, 4);

        assertThat(ok).isTrue();
        assertThat(state.getSystem(1).garrison()).isEqualTo(10); // unchanged
        assertThat(state.fleets()).isEmpty();            // no fleet yet
        assertThat(state.pendingOrders().get(1)).hasSize(1);
        assertThat(state.pendingOrders().get(1).getFirst()).isInstanceOf(FleetOrder.Send.class);
    }

    @Test
    void queueSendRejectsNonOwner() {
        assertThat(service.queueSend(state, 99, 1, 2, 3)).isFalse();
        assertThat(state.pendingOrders().get(99)).isNull();
    }

    @Test
    void queueSendRejectsOvercommit() {
        assertThat(service.queueSend(state, 1, 1, 2, 6)).isTrue();
        // second order would exceed garrison of 10
        assertThat(service.queueSend(state, 1, 1, 2, 5)).isFalse();
        assertThat(state.pendingOrders().get(1)).hasSize(1);
    }

    @Test
    void queueSendCumulativeValidation() {
        // two orders from same system must together not exceed garrison
        assertThat(service.queueSend(state, 1, 1, 2, 6)).isTrue();
        assertThat(service.queueSend(state, 1, 1, 2, 4)).isTrue(); // exactly uses remaining 4
        assertThat(service.queueSend(state, 1, 1, 2, 1)).isFalse();  // over
        assertThat(state.pendingOrders().get(1)).hasSize(2);
    }

    // --- queueWait ---

    @Test
    void queueWaitIdempotent() {
        // first create a fleet via sendFleet (immediate)
        service.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();

        assertThat(service.queueWait(state, 1, fleetId)).isTrue();
        assertThat(service.queueWait(state, 1, fleetId)).isTrue(); // second call must not duplicate
        assertThat(state.pendingOrders().get(1)).hasSize(1);
    }

    @Test
    void queueWaitRejectsUnknownFleet() {
        assertThat(service.queueWait(state, 1, 999)).isFalse();
    }

    // --- queueDisband ---

    @Test
    void queueDisbandRequiresLaunchedThisTurn() {
        service.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();

        assertThat(service.queueDisband(state, 1, fleetId)).isTrue(); // launched this turn (turn=1)
    }

    @Test
    void queueDisbandRejectsOldFleet() {
        service.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();
        // Simulate the fleet having launched in a prior turn
        var f = state.fleets().getFirst();
        state.fleets().set(0, new de.zettsystems.starfare.game.values.Fleet(
                f.globalId(), f.ownerId(), f.localNo(), f.fromSystemId(), f.toSystemId(),
                f.ships(), f.launchTurn() - 1, f.arrivalTurn()));

        assertThat(service.queueDisband(state, 1, fleetId)).isFalse();
    }

    // --- standing orders (production-routed) ---

    @Test
    void addStandingOrderRequiresOwnedSource() {
        assertThat(service.addStandingOrder(state, 1, 1, 2)).isGreaterThan(0);
        assertThat(state.standingOrders().get(1)).hasSize(1);
        // foreign source is rejected
        assertThat(service.addStandingOrder(state, 1, 2, 1)).isEqualTo(-1);
        assertThat(state.standingOrders().get(1)).hasSize(1);
    }

    @Test
    void addStandingOrderReplacesExistingForSameSource() {
        state.systems().add(new StarSystem(3, "S3", 200, 0, null, 0, 1, true));
        int first = service.addStandingOrder(state, 1, 1, 2);
        int second = service.addStandingOrder(state, 1, 1, 3);

        assertThat(second).isEqualTo(first); // same id, target updated
        assertThat(state.standingOrders().get(1)).hasSize(1);
        assertThat(state.standingOrders().get(1).getFirst().toSystemId()).isEqualTo(3);
    }

    @Test
    void applyStandingOrdersCreatesFleetEqualToProduction() {
        service.addStandingOrder(state, 1, 1, 2);

        var routed = service.applyStandingOrdersForProduction(state);

        assertThat(routed).contains(1);
        assertThat(state.fleets()).hasSize(1);
        var fleet = state.fleets().getFirst();
        assertThat(fleet.ships()).isEqualTo(2); // system 1 has productionPerTurn = 2
        assertThat(fleet.fromSystemId()).isOne();
        assertThat(fleet.toSystemId()).isEqualTo(2);
        // garrison remains untouched at this phase (production step is skipped separately)
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
    }

    @Test
    void applyStandingOrdersSkipsWhenProductionZero() {
        state.updateSystem(1, s -> new StarSystem(s.id(), s.name(), s.x(), s.y(),
                s.ownerId(), s.garrison(), 0, s.neutral()));
        service.addStandingOrder(state, 1, 1, 2);

        var routed = service.applyStandingOrdersForProduction(state);

        // still in routed set (so production step is skipped) but no fleet created
        assertThat(routed).contains(1);
        assertThat(state.fleets()).isEmpty();
        assertThat(state.standingOrders().get(1)).hasSize(1); // kept
    }

    @Test
    void applyStandingOrdersRemovesOrderWhenSourceLost() {
        service.addStandingOrder(state, 1, 1, 2);
        state.updateSystem(1, s -> s.captureBy(99, s.garrison()));

        var routed = service.applyStandingOrdersForProduction(state);

        assertThat(routed).doesNotContain(1);
        assertThat(state.standingOrders().get(1)).isEmpty();
        assertThat(state.fleets()).isEmpty();
    }

    @Test
    void removeStandingOrderDeletesById() {
        int id = service.addStandingOrder(state, 1, 1, 2);

        assertThat(service.removeStandingOrder(state, 1, id)).isTrue();
        assertThat(state.standingOrders().get(1)).isEmpty();
        assertThat(service.removeStandingOrder(state, 1, id)).isFalse();
    }

    @Test
    void removeStandingOrderFromDeletesByFromSystem() {
        service.addStandingOrder(state, 1, 1, 2);

        assertThat(service.removeStandingOrderFrom(state, 1, 1)).isTrue();
        assertThat(state.standingOrders().get(1)).isEmpty();
    }
}
