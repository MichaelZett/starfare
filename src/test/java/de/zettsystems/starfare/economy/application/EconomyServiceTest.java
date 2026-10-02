package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.testsupport.SpacewardStates;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class EconomyServiceTest {
    private final EconomyService economy = new DefaultEconomyService();
    @Test void productionUsesCurrentCapacityAndGrowthAffectsTheFollowingRound() {
        GameState state = SpacewardStates.running();
        assertThat(economy.allocateExpansion(state, 1, 1, 4)).isTrue();
        for (int turn = 0; turn < 5; turn++) {
            assertThat(economy.produce(state, Map.of()).stream().filter(r -> r.systemId() == 1).findFirst().orElseThrow().ships()).isEqualTo(6);
        }
        assertThat(state.getSystem(1).garrison()).isEqualTo(42);
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(7);
        assertThat(state.industries().get(1)).isEqualTo(new Industry(11, 4, 0));
        economy.produce(state, Map.of());
        assertThat(state.getSystem(1).garrison()).isEqualTo(49);
    }
    @Test void allocationChangesKeepRoutesAndCompetingTransfersNeverSpendTheReserve() {
        GameState state = SpacewardStates.running();
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 7)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 4, 3)).isPositive();
        assertThat(economy.allocateExpansion(state, 1, 1, 4)).isTrue();
        assertThat(fleets.routingCapacity(state, 1, 1)).isEqualTo(6);
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 7)).isEqualTo(-1);
        Map<Integer, Integer> routed = fleets.applyStandingOrdersForProduction(state);
        assertThat(routed.get(1)).isEqualTo(8);
        assertThat(state.fleets()).extracting(f -> f.ships()).containsExactly(7, 1);
        economy.produce(state, routed);
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
        assertThat(state.standingOrders().get(1)).hasSize(2);
    }
    @Test void captureRetainsCapacityAndProgressButRestoresFullShipbuildingAndNoReserve() {
        GameState state = SpacewardStates.running();
        economy.allocateExpansion(state, 1, 1, 4);
        economy.produce(state, Map.of());
        economy.produce(state, Map.of());
        state.updateSystem(1, system -> system.captureBy(2, 5));
        assertThat(state.industries().get(1)).isEqualTo(new Industry(10, 0, 8));
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(10);
        assertThat(state.getSystem(1).garrisonReserve()).isZero();
        assertThat(state.getSystem(1).garrison()).isEqualTo(5);
    }
    @Test void allExpansionBuildsNoShipsAndNeutralSystemsDoNotGrow() {
        GameState state = SpacewardStates.running();
        StarSystem neutral = state.getSystem(4);
        assertThat(economy.allocateExpansion(state, 1, 1, 10)).isTrue();
        economy.produce(state, Map.of());
        assertThat(state.getSystem(1).garrison()).isEqualTo(12);
        assertThat(state.getSystem(1).productionPerTurn()).isZero();
        assertThat(state.industries().get(1).expansionProgress()).isEqualTo(10);
        assertThat(state.getSystem(4)).isEqualTo(neutral);
    }
    @Test void unauthorizedOrInvalidAllocationsAndClassicAreUnchanged() {
        GameState state = SpacewardStates.running();
        var before = GameState.toSnapshot(state);
        assertThat(economy.allocateExpansion(state, 2, 1, 4)).isFalse();
        assertThat(economy.allocateExpansion(state, 1, 4, 2)).isFalse();
        assertThat(economy.allocateExpansion(state, 1, 1, -1)).isFalse();
        assertThat(economy.allocateExpansion(state, 1, 1, 11)).isFalse();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        GameState classic = new GameState();
        assertThat(economy.allocateExpansion(classic, 1, 1, 1)).isFalse();
        assertThatThrownBy(() -> economy.produce(classic, Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThat(classic.industries()).isEmpty();
    }
}
