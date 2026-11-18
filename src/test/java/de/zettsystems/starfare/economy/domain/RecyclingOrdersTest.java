package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.testsupport.MetalStates;
import de.zettsystems.starfare.testsupport.RecyclingStates;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RecyclingOrdersTest {
    @Test void dispatchFromOtherSystemsDoesNotReduceWhatCanBeRecycled() {
        var state = RecyclingStates.running();
        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Send(1, 2, 3, 3))));
        assertThat(RecyclingOrders.plan(state, 1, 1, 12)).isTrue();
    }

    @Test void storedReservationsMayUseExactlyTheAvailableShipsOfTheirSystem() {
        var state = RecyclingStates.running();
        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Send(1, 1, 2, 4), new FleetOrder.Send(1, 2, 3, 1))));
        state.planRecycling(1, 8);
        var metal = state.metals().orElseThrow();
        assertThatCode(() -> RecyclingOrders.validate(state, metal)).doesNotThrowAnyException();
        state.planRecycling(1, 9);
        var overbooked = state.metals().orElseThrow();
        assertThatThrownBy(() -> RecyclingOrders.validate(state, overbooked))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid stored recycling reservation");
    }

    @Test void recyclingRulesRequireAStoredRecyclingEconomy() {
        var state = RecyclingStates.running();
        var withoutRecycling = MetalStates.running(10, 100).metals().orElseThrow();
        assertThatThrownBy(() -> RecyclingOrders.validate(state, withoutRecycling))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Recycling rules and state must match");
    }

    @Test void fractionsStayBelowOneShipAndCountersCannotBeNegative() {
        assertThatCode(() -> new RecyclingEconomy(Map.of(1, 0), Map.of(1, 0, 2, 9), 0, 0, 0)).doesNotThrowAnyException();
        Map<Integer, Integer> orders = Map.of(1, 0);
        Map<Integer, Integer> whole = Map.of(1, 10);
        Map<Integer, Integer> negative = Map.of(1, -1);
        Map<Integer, Integer> none = Map.of();
        assertThatThrownBy(() -> new RecyclingEconomy(orders, whole, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecyclingEconomy(orders, negative, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecyclingEconomy(negative, none, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecyclingEconomy(orders, none, -1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecyclingEconomy(orders, none, 0, -1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecyclingEconomy(orders, none, 0, 0, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
