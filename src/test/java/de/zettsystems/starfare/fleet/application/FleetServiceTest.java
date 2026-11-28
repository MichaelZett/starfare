package de.zettsystems.starfare.fleet.application;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.fleet.values.ScheduledDeparture;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.testsupport.NavigationStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FleetServiceTest {

    @Test
    void capacityReadsDoNotCreateStandingOrderEntries() {
        assertThat(service.routingCapacity(state, 1, 1)).isEqualTo(2);
        assertThat(service.routingHeadroom(state, 1, 1, 2)).isEqualTo(2);
        assertThat(service.routedFrom(state, 1, 1)).isZero();
        assertThat(service.routingHeadroom(state, 99, 1, 2)).isZero();
        assertThat(state.standingOrders()).isEmpty();
    }

    @Test
    void rejectedRelocationDoesNotCreateEntriesAndProductionMayExceedGarrison() {
        state.systems().clear();
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 20, false));
        state.systems().add(new StarSystem(2, "S2", 100, 0, 1, 3, 2, false));

        assertThat(service.addStandingOrder(state, 1, 1, 2, 21)).isEqualTo(-1);
        assertThat(state.standingOrders()).isEmpty();
        assertThat(service.addStandingOrder(state, 1, 1, 2, 20)).isPositive();
        assertThat(state.standingOrders().get(1)).singleElement().satisfies(order ->
                assertThat(order.ships()).isEqualTo(20));
    }

    @Test
    void relocationsLeaveShipsReservedForALaterJointStartAlone() {
        state.systems().clear();
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        state.systems().add(new StarSystem(2, "S2", 100, 0, 1, 3, 2, false));
        assertThat(service.addStandingOrder(state, 1, 1, 2, 2)).isPositive();
        state.standingOrders().get(1).replaceAll(order -> new StandingOrder(order.id(), order.ownerId(),
                order.fromSystemId(), order.toSystemId(), 20));
        state.replaceDepartures(1, List.of(new ScheduledDeparture(1, 1, 2, 4, state.turn() + 2, state.turn() + 3, null)));

        assertThat(service.applyStandingOrdersForProduction(state)).containsEntry(1, 3);
        assertThat(state.fleets()).singleElement().satisfies(fleet -> assertThat(fleet.ships()).isEqualTo(3));
    }

    @Test
    void relocationsWithoutReservationsStillDrawOnTheWholeGarrison() {
        state.systems().clear();
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        state.systems().add(new StarSystem(2, "S2", 100, 0, 1, 3, 2, false));
        assertThat(service.addStandingOrder(state, 1, 1, 2, 2)).isPositive();
        state.standingOrders().get(1).replaceAll(order -> new StandingOrder(order.id(), order.ownerId(),
                order.fromSystemId(), order.toSystemId(), 20));

        assertThat(service.applyStandingOrdersForProduction(state)).containsEntry(1, 7);
    }

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
        assertThat(state.pendingOrders().get(1)).first().isInstanceOf(FleetOrder.Send.class);
    }

    @Test
    void ordersWithAnUnknownSystemAreRejectedInsteadOfThrowing() {
        int unknown = 99;

        assertThat(service.queueSend(state, 1, unknown, 2, 1)).isFalse();
        assertThat(service.queueSend(state, 1, 1, unknown, 1)).isFalse();
        assertThat(service.sendFleet(state, 1, unknown, 2, 1)).isFalse();
        assertThat(service.sendFleet(state, 1, 1, unknown, 1)).isFalse();
        assertThat(service.addStandingOrder(state, 1, unknown, 2, 1)).isEqualTo(-1);
        assertThat(service.addStandingOrder(state, 1, 1, unknown, 1)).isEqualTo(-1);
        assertThat(service.routingCapacity(state, 1, unknown)).isZero();
        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.fleets()).isEmpty();
        assertThat(state.standingOrders()).isEmpty();
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
    }

    @Test
    void standingOrdersFromAnUnknownSystemAreDroppedAtProduction() {
        state.standingOrders().put(1, new ArrayList<>(List.of(new StandingOrder(1, 1, 99, 2, 1))));

        assertThat(service.applyStandingOrdersForProduction(state)).isEmpty();
        assertThat(state.standingOrders().get(1)).isEmpty();
        assertThat(state.fleets()).isEmpty();
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

    @Test
    void zeroShipOrdersAreRejectedEverywhere() {
        assertThat(service.queueSend(state, 1, 1, 2, 0)).isFalse();
        assertThat(service.sendFleet(state, 1, 1, 2, 0)).isFalse();
        assertThat(service.addStandingOrder(state, 1, 1, 2, 0)).isEqualTo(-1);

        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.fleets()).isEmpty();
        assertThat(state.standingOrders()).isEmpty();
    }

    @Test
    void sendFleetMayLaunchTheWholeAvailableGarrison() {
        assertThat(service.sendFleet(state, 1, 1, 2, 10)).isTrue();

        assertThat(state.getSystem(1).garrison()).isZero();
        assertThat(state.fleets()).singleElement().satisfies(fleet -> assertThat(fleet.ships()).isEqualTo(10));
    }

    @Test
    void queuedShipsFromAnotherSystemDoNotReduceThisSystemsBudget() {
        state.systems().add(new StarSystem(3, "S3", 50, 0, 1, 5, 1, false));
        assertThat(service.queueSend(state, 1, 3, 2, 5)).isTrue();
        assertThat(service.queueSend(state, 1, 1, 2, 10)).isTrue();

        assertThat(state.pendingOrders().get(1)).containsExactly(
                new FleetOrder.Send(1, 3, 2, 5), new FleetOrder.Send(1, 1, 2, 10));
    }

    @Test
    void waitAndDisbandOrdersAreOnlyAcceptedForTheOwnersFleet() {
        state.players().add(new Player(2, "P2", false, "#000"));
        state.systems().add(new StarSystem(3, "S3", 50, 0, 2, 5, 1, false));
        service.sendFleet(state, 2, 3, 2, 3);
        service.sendFleet(state, 1, 1, 2, 3);
        int foreign = state.fleets().getFirst().globalId();

        assertThat(service.queueWait(state, 1, foreign)).isFalse();
        assertThat(service.queueDisband(state, 1, foreign)).isFalse();
        assertThat(service.setFleetWait(state, 1, foreign)).isFalse();
        assertThat(state.pendingOrders().get(1)).isNull();
        assertThat(state.waitThisTurn()).isEmpty();
    }

    @Test
    void setFleetWaitMarksTheFleetForThisTurn() {
        service.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();

        assertThat(service.setFleetWait(state, 1, fleetId)).isTrue();

        assertThat(state.waitThisTurn()).containsExactly(fleetId);
    }

    @Test
    void waitAndDisbandAreQueuedNextToSendsAndCanBeUndone() {
        service.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();
        assertThat(service.queueSend(state, 1, 1, 2, 2)).isTrue();

        assertThat(service.queueWait(state, 1, fleetId)).isTrue();
        assertThat(state.pendingOrders().get(1)).containsExactly(
                new FleetOrder.Send(1, 1, 2, 2), new FleetOrder.Wait(1, fleetId));
        assertThat(state.undoOrders(1)).isTrue();
        assertThat(state.pendingOrders().get(1)).containsExactly(new FleetOrder.Send(1, 1, 2, 2));

        assertThat(service.queueDisband(state, 1, fleetId)).isTrue();
        assertThat(service.queueDisband(state, 1, fleetId)).isTrue();
        assertThat(state.pendingOrders().get(1)).containsExactly(
                new FleetOrder.Send(1, 1, 2, 2), new FleetOrder.Disband(1, fleetId));
        assertThat(state.undoOrders(1)).isTrue();
        assertThat(state.pendingOrders().get(1)).containsExactly(new FleetOrder.Send(1, 1, 2, 2));
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
        state.fleets().set(0, new Fleet(
                f.globalId(), f.ownerId(), f.localNo(), f.fromSystemId(), f.toSystemId(),
                f.ships(), f.launchTurn() - 1, f.arrivalTurn()));

        assertThat(service.queueDisband(state, 1, fleetId)).isFalse();
    }

    // --- standing orders (production-routed) ---

    @Test
    void addStandingOrderRequiresOwnedSource() {
        assertThat(service.addStandingOrder(state, 1, 1, 2, 2)).isPositive();
        assertThat(state.standingOrders().get(1)).hasSize(1);
        // foreign source is rejected
        assertThat(service.addStandingOrder(state, 1, 2, 1, 1)).isEqualTo(-1);
        assertThat(state.standingOrders().get(1)).hasSize(1);
    }

    @Test
    void routingIsCappedByProductionPlusIncoming() {
        // System 1 produziert 2 — mehr darf nicht abfliessen.
        assertThat(service.routingHeadroom(state, 1, 1, 2)).isEqualTo(2);
        assertThat(service.addStandingOrder(state, 1, 1, 2, 3)).isEqualTo(-1);
        assertThat(service.addStandingOrder(state, 1, 1, 2, 2)).isPositive();
        assertThat(service.routingHeadroom(state, 1, 1, 2)).as("Ersetzen gibt den Platz frei").isEqualTo(2);
    }

    @Test
    void secondTargetSharesTheSameBudget() {
        state.systems().add(new StarSystem(3, "S3", 200, 0, null, 0, 1, true));
        assertThat(service.addStandingOrder(state, 1, 1, 2, 1)).isPositive();

        assertThat(service.addStandingOrder(state, 1, 1, 3, 2)).as("1+2 > Produktion 2").isEqualTo(-1);
        assertThat(service.addStandingOrder(state, 1, 1, 3, 1)).isPositive();
        assertThat(state.standingOrders().get(1)).hasSize(2);
        assertThat(service.routedFrom(state, 1, 1)).isEqualTo(2);
    }

    @Test
    void incomingRoutingRaisesTheOutgoingLimit() {
        state.systems().add(new StarSystem(3, "S3", 200, 0, 1, 5, 1, false));
        // S3 produziert 1, darf also allein nur 1 weiterleiten.
        assertThat(service.routingHeadroom(state, 1, 3, 2)).isOne();

        service.addStandingOrder(state, 1, 1, 3, 2);

        assertThat(service.routingHeadroom(state, 1, 3, 2)).as("1 eigene + 2 eingehende").isEqualTo(3);
    }

    @Test
    void addStandingOrderReplacesExistingForSameRoute() {
        int first = service.addStandingOrder(state, 1, 1, 2, 1);
        int second = service.addStandingOrder(state, 1, 1, 2, 2);

        assertThat(second).isEqualTo(first); // same id, size updated
        assertThat(state.standingOrders().get(1)).hasSize(1);
        assertThat(state.standingOrders().get(1).getFirst().ships()).isEqualTo(2);
    }

    @Test
    void applyStandingOrdersCreatesFleetOfTheRoutedSize() {
        service.addStandingOrder(state, 1, 1, 2, 2);

        var routed = service.applyStandingOrdersForProduction(state);

        assertThat(routed).containsEntry(1, 2);
        assertThat(state.fleets()).hasSize(1);
        var fleet = state.fleets().getFirst();
        assertThat(fleet.ships()).isEqualTo(2);
        assertThat(fleet.fromSystemId()).isOne();
        assertThat(fleet.toSystemId()).isEqualTo(2);
        // garrison remains untouched at this phase (production step is applied separately)
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
    }

    @Test
    void routingSmallerThanProductionLeavesTheRest() {
        service.addStandingOrder(state, 1, 1, 2, 1);

        var routed = service.applyStandingOrdersForProduction(state);
        state.updateSystem(1, s -> s.produceAndRoute(routed.getOrDefault(1, 0)));

        assertThat(state.fleets().getFirst().ships()).isOne();
        // 10 Garnison + 2 Produktion - 1 verlegt
        assertThat(state.getSystem(1).garrison()).isEqualTo(11);
    }

    @Test
    void routingFallsBackToTheGarrisonWhenProductionIsGone() {
        service.addStandingOrder(state, 1, 1, 2, 2);
        // Produktion faellt weg, die Garnison traegt die Verlegung weiter.
        state.updateSystem(1, s -> new StarSystem(s.id(), s.name(), s.x(), s.y(),
                s.ownerId(), s.garrison(), 0, s.neutral()));

        var routed = service.applyStandingOrdersForProduction(state);

        assertThat(routed).containsEntry(1, 2);
        assertThat(state.fleets()).hasSize(1);
        assertThat(state.standingOrders().get(1)).hasSize(1); // kept
    }

    @Test
    void routingNeverShipsMoreThanTheSystemHas() {
        state.updateSystem(1, s -> new StarSystem(s.id(), s.name(), s.x(), s.y(),
                s.ownerId(), 0, 2, s.neutral()));
        service.addStandingOrder(state, 1, 1, 2, 2);
        state.updateSystem(1, s -> new StarSystem(s.id(), s.name(), s.x(), s.y(),
                s.ownerId(), 0, 0, s.neutral()));

        var routed = service.applyStandingOrdersForProduction(state);

        assertThat(routed).isEmpty();
        assertThat(state.fleets()).isEmpty();
    }

    @Test
    void applyStandingOrdersRemovesOrderWhenSourceLost() {
        service.addStandingOrder(state, 1, 1, 2, 2);
        state.updateSystem(1, s -> s.captureBy(99, s.garrison()));

        var routed = service.applyStandingOrdersForProduction(state);

        assertThat(routed).isEmpty();
        assertThat(state.standingOrders().get(1)).isEmpty();
        assertThat(state.fleets()).isEmpty();
    }

    @Test
    void removeStandingOrderDeletesById() {
        int id = service.addStandingOrder(state, 1, 1, 2, 2);

        assertThat(service.removeStandingOrder(state, 1, id)).isTrue();
        assertThat(state.standingOrders().get(1)).isEmpty();
        assertThat(service.removeStandingOrder(state, 1, id)).isFalse();
    }

    @Test
    void removingStandingOrdersKeepsTheOtherRoutes() {
        state.systems().add(new StarSystem(3, "S3", 50, 0, 1, 5, 2, false));
        int first = service.addStandingOrder(state, 1, 1, 2, 1);
        int second = service.addStandingOrder(state, 1, 3, 2, 2);

        assertThat(service.removeStandingOrder(state, 1, first)).isTrue();
        assertThat(service.removeStandingOrderFrom(state, 1, 1)).isFalse();
        assertThat(state.standingOrders().get(1)).singleElement().satisfies(order ->
                assertThat(order.id()).isEqualTo(second));
    }

    // --- rejected inputs ---

    @Test
    void ordersWithinOneSystemOrToADestroyedSystemAreRejected() {
        state.updateCosmic(cosmic -> cosmic.explode(2));
        int[][] routes = {{1, 1}, {1, 2}};

        for (int[] route : routes) {
            assertThat(service.queueSend(state, 1, route[0], route[1], 1)).as("queue %s", route).isFalse();
            assertThat(service.sendFleet(state, 1, route[0], route[1], 1)).as("send %s", route).isFalse();
            assertThat(service.addStandingOrder(state, 1, route[0], route[1], 1)).as("relocate %s", route).isEqualTo(-1);
        }

        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.fleets()).isEmpty();
        assertThat(state.standingOrders()).isEmpty();
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
    }

    @Test
    void ordersBeyondTheFlightRangeAreRejected() {
        GameState extended = NavigationStates.running();
        int fleets = extended.fleets().size();

        assertThat(service.queueSend(extended, 1, 1, 5, 1)).isFalse();
        assertThat(service.sendFleet(extended, 1, 1, 5, 1)).isFalse();
        assertThat(service.addStandingOrder(extended, 1, 1, 5, 1)).isEqualTo(-1);

        assertThat(extended.pendingOrders().getOrDefault(1, List.of())).isEmpty();
        assertThat(extended.fleets()).hasSize(fleets);
        assertThat(extended.standingOrders().getOrDefault(1, List.of())).isEmpty();
    }

    @Test
    void aRelocationWithoutRouteSendsNothingButIsKept() {
        GameState extended = NavigationStates.running();
        int fleets = extended.fleets().size();
        extended.standingOrders().put(1, new ArrayList<>(List.of(new StandingOrder(1, 1, 1, 5, 2))));

        var routed = service.applyStandingOrdersForProduction(extended);

        assertThat(routed).isEmpty();
        assertThat(extended.fleets()).hasSize(fleets);
        assertThat(extended.standingOrders().get(1)).hasSize(1);
    }

    @Test
    void relocationsFromANeutralSourceAreDropped() {
        state.systems().add(new StarSystem(3, "S3", 50, 0, 1, 5, 1, true));
        state.standingOrders().put(1, new ArrayList<>(List.of(new StandingOrder(1, 1, 3, 1, 1))));

        var routed = service.applyStandingOrdersForProduction(state);

        assertThat(routed).isEmpty();
        assertThat(state.standingOrders().get(1)).isEmpty();
        assertThat(state.fleets()).isEmpty();
    }

    @Test
    void withoutRelocationsNothingCanBeRemovedOrTrimmed() {
        assertThat(service.removeStandingOrder(state, 1, 1)).isFalse();
        assertThat(service.removeStandingOrderFrom(state, 1, 1)).isFalse();
        assertThat(service.trimRelocations(state, 1, 1)).isEmpty();
        assertThat(state.standingOrders()).isEmpty();
    }

    @Test
    void disbandingIsLimitedToOwnFleetsJustLaunchedFromAnOwnedSystem() {
        state.players().add(new Player(2, "P2", false, "#000"));
        service.sendFleet(state, 1, 1, 2, 3);
        Fleet launched = state.fleets().getFirst();

        assertThat(service.disbandFleet(state, 2, launched.globalId())).as("foreign fleet").isFalse();
        assertThat(service.disbandFleet(state, 1, 999)).as("unknown fleet").isFalse();
        state.fleets().set(0, new Fleet(launched.globalId(), 1, launched.localNo(), 1, 2, 3,
                launched.launchTurn() - 1, launched.arrivalTurn()));
        assertThat(service.disbandFleet(state, 1, launched.globalId())).as("launched earlier").isFalse();
        state.fleets().set(0, launched);
        state.updateSystem(1, system -> system.captureBy(2, system.garrison()));
        assertThat(service.disbandFleet(state, 1, launched.globalId())).as("source lost").isFalse();
        assertThat(service.queueDisband(state, 1, launched.globalId())).as("source lost").isFalse();

        assertThat(state.fleets()).hasSize(1);
    }

    @Test
    void disbandingAJustLaunchedFleetReturnsItsShips() {
        service.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();
        assertThat(service.setFleetWait(state, 1, fleetId)).isTrue();

        assertThat(service.disbandFleet(state, 1, fleetId)).isTrue();

        assertThat(state.fleets()).isEmpty();
        assertThat(state.waitThisTurn()).isEmpty();
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
    }

    @Test
    void waitOrdersDoNotCountAgainstTheShipsLeftToSend() {
        service.sendFleet(state, 1, 1, 2, 3);
        assertThat(service.queueWait(state, 1, state.fleets().getFirst().globalId())).isTrue();

        assertThat(service.queueSend(state, 1, 1, 2, 7)).isTrue();
        assertThat(service.queueSend(state, 1, 1, 2, 1)).isFalse();
    }

    @Test
    void removeStandingOrderFromDeletesByFromSystem() {
        service.addStandingOrder(state, 1, 1, 2, 2);

        assertThat(service.removeStandingOrderFrom(state, 1, 1)).isTrue();
        assertThat(state.standingOrders().get(1)).isEmpty();
    }
}
