package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerViewBuilderTest {

    private final PlayerViewBuilder builder = new DefaultPlayerViewBuilder();
    private GameState state;

    @BeforeEach
    void setUp() {
        state = new GameState();
        state.players().add(new Player(1, "P1", false, "#aaaaaa"));
        state.players().add(new Player(2, "P2", false, "#bbbbbb"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));      // owned by P1
        state.systems().add(new StarSystem(2, "S2", 100, 0, 2, 7, 1, false));     // owned by P2
        state.systems().add(new StarSystem(3, "S3", 200, 0, null, 4, 1, true));   // neutral
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
    }

    private VisibleSystem visible(PlayerViewState view, int systemId) {
        return view.systems().stream().filter(s -> s.id() == systemId).findFirst().orElseThrow();
    }

    @Test
    void ownSystemShowsGarrisonOwnerColorAndCurrentTurn() {
        PlayerViewState view = builder.forPlayer(state, 1);

        VisibleSystem own = visible(view, 1);
        assertThat(own.fullyVisible()).isTrue();
        assertThat(own.ownerId()).isOne();
        assertThat(own.garrison()).isEqualTo(10);
        assertThat(own.productionPerTurn()).isEqualTo(2);
        assertThat(own.colorHex()).isEqualTo("#aaaaaa");
        assertThat(own.lastSeenTurn()).isEqualTo(state.turn());
    }

    @Test
    void enemyWithoutIntelHasNullVisibility() {
        PlayerViewState view = builder.forPlayer(state, 1);

        VisibleSystem enemy = visible(view, 2);
        assertThat(enemy.fullyVisible()).isFalse();
        assertThat(enemy.ownerId()).isNull();
        assertThat(enemy.garrison()).isNull();
        assertThat(enemy.productionPerTurn()).isNull();
        assertThat(enemy.colorHex()).isNull();
        assertThat(enemy.lastSeenTurn()).isNull();
    }

    @Test
    void enemyWithOwnerIntelExposesColorAndLastSeen() {
        state.intel().get(1).put(2, new GameState.Intel(2, 5));

        PlayerViewState view = builder.forPlayer(state, 1);

        VisibleSystem enemy = visible(view, 2);
        assertThat(enemy.colorHex()).isEqualTo("#bbbbbb");
        assertThat(enemy.lastSeenTurn()).isEqualTo(5);
        assertThat(enemy.garrison()).as("garrison stays hidden — intel only carries owner+turn").isNull();
    }

    @Test
    void intelWithNullOwnerLeavesColorEmpty() {
        // intel says "I once saw it, no owner at the time" (neutral)
        state.intel().get(1).put(3, new GameState.Intel(null, 4));

        PlayerViewState view = builder.forPlayer(state, 1);

        VisibleSystem neutral = visible(view, 3);
        assertThat(neutral.colorHex()).isNull();
        assertThat(neutral.lastSeenTurn()).as("lastSeen is only filled together with a known owner").isNull();
    }

    @Test
    void committedShipsAreSubtractedFromOwnGarrison() {
        state.pendingOrders().put(1, List.of(
                new FleetOrder.Send(1, 1, 2, 3),
                new FleetOrder.Send(1, 1, 3, 2)));

        PlayerViewState view = builder.forPlayer(state, 1);

        // 10 garrison - 3 - 2 already committed = 5 visible
        assertThat(visible(view, 1).garrison()).isEqualTo(5);
    }

    @Test
    void plannedOrdersIncludeSendWaitAndDisbandWithDistinctTypes() {
        state.pendingOrders().put(1, List.of(
                new FleetOrder.Send(1, 1, 2, 3),
                new FleetOrder.Wait(1, 99),
                new FleetOrder.Disband(1, 100)));

        PlayerViewState view = builder.forPlayer(state, 1);

        assertThat(view.plannedOrders()).hasSize(3);
        PlannedOrder send = view.plannedOrders().getFirst();
        assertThat(send.type()).isEqualTo("map.orderType.send");
        assertThat(send.fromSystem()).isEqualTo("S1");
        assertThat(send.toSystem()).isEqualTo("S2");
        assertThat(send.ships()).isEqualTo(3);
        assertThat(view.plannedOrders().get(1).type()).isEqualTo("map.orderType.wait");
        assertThat(view.plannedOrders().get(2).type()).isEqualTo("map.orderType.disband");
    }

    @Test
    void standingOrderViewsCarryNamesAndProduction() {
        state.standingOrders().put(1, List.of(new StandingOrder(7, 1, 1, 2)));

        PlayerViewState view = builder.forPlayer(state, 1);

        assertThat(view.standingOrders()).hasSize(1);
        var so = view.standingOrders().getFirst();
        assertThat(so.id()).isEqualTo(7);
        assertThat(so.fromSystem()).isEqualTo("S1");
        assertThat(so.toSystem()).isEqualTo("S2");
        assertThat(so.productionPerTurn()).isEqualTo(2);
    }

    @Test
    void ownFleetsArePlayerScoped() {
        state.addFleet(1, 1, 2, 3);
        state.addFleet(2, 2, 1, 4);

        PlayerViewState p1 = builder.forPlayer(state, 1);
        PlayerViewState p2 = builder.forPlayer(state, 2);

        assertThat(p1.ownFleets()).hasSize(1);
        assertThat(p1.ownFleets().getFirst().ownerId()).isOne();
        assertThat(p2.ownFleets()).hasSize(1);
        assertThat(p2.ownFleets().getFirst().ownerId()).isEqualTo(2);
    }

    @Test
    void observerSeesEverySystemWithOwnerColors() {
        PlayerViewState view = builder.forObserver(state);

        assertThat(view.systems()).hasSize(3);
        for (VisibleSystem s : view.systems()) {
            assertThat(s.fullyVisible()).isTrue();
            assertThat(s.lastSeenTurn()).isEqualTo(state.turn());
        }
        assertThat(visible(view, 1).colorHex()).isEqualTo("#aaaaaa");
        assertThat(visible(view, 2).colorHex()).isEqualTo("#bbbbbb");
        assertThat(visible(view, 3).colorHex()).as("neutral systems have no owner color").isNull();
    }
}
