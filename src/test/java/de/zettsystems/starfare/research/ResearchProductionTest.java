package de.zettsystems.starfare.research;

import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.testsupport.ColonyStates;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResearchProductionTest {
    private final DefaultEconomyService economy = new DefaultEconomyService();

    @Test void empireBudgetIsReservedOnceAndPreviewsMatchProductionWithoutSpendingMetalOnResearch() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        state.start();
        state.planShipResearch(1, 50);
        assertThat(state.researchReservations()).containsEntry(1, 5).containsEntry(2, 2).containsEntry(4, 0);
        assertThat(state.usableIndustrialCapacity(1)).isEqualTo(5);
        assertThat(economy.allocateProduction(state, 1, 1, 4, 2)).isFalse();
        var views = new DefaultPlayerViewBuilder();
        var industry = views.forPlayer(state, 1).systems().stream().filter(s -> s.id() == 1).findFirst().orElseThrow().industry();
        assertThat(industry.usableCapacity()).isEqualTo(5);
        assertThat(industry.metal().plannedShips()).isEqualTo(5);
        var forecast = state.metalPreview();
        var snapshot = GameState.toSnapshot(state);
        assertThat(GameState.copyOf(state).metalPreview()).isEqualTo(forecast);
        assertThat(GameState.toSnapshot(state)).isEqualTo(snapshot);
        long balance = RecyclingOrders.balanceTenths(state);
        economy.produce(state, Map.of());
        assertThat(state.getSystem(1).garrison()).isEqualTo(17);
        assertThat(state.getSystem(2).garrison()).isEqualTo(5);
        assertThat(state.metals().orElseThrow().research().empires().get(1).progress()).isEqualTo(7);
        assertThat(state.metals().orElseThrow().stocks()).containsEntry(1, 3L);
        assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(balance);
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(state)).metalPreview()).isEqualTo(state.metalPreview());
    }

    @Test void completionRoundBuildsOldTechnologyAndOnlyFollowingRoundsBuildTheNewGeneration() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        state.start(); state.planShipResearch(1, 100);
        for (int i = 0; i < 7; i++) { economy.produce(state, Map.of()); state.nextTurn(); }
        assertThat(state.shipTechnology(1)).isZero();
        assertThat(state.metals().orElseThrow().research().empires().get(1).progress()).isEqualTo(98);
        state.planShipResearch(1, 50);
        economy.produce(state, Map.of());
        assertThat(state.shipTechnology(1)).isEqualTo(1);
        assertThat(state.getSystem(1).composition().levels()).containsOnlyKeys(0);
        assertThat(state.getSystem(1).composition().count()).isEqualTo(17);
        state.captureReplayFrame();
        var frame = state.replayFrames().get(state.turn());
        state.nextTurn(); state.planShipResearch(1, 0);
        economy.produce(state, Map.of());
        assertThat(state.getSystem(1).composition().levels()).containsEntry(0, 17).containsEntry(1, 2);
        var historical = new DefaultPlayerViewBuilder().forReplay(state, frame, 1);
        var industry = historical.systems().stream().filter(s -> s.id() == 1).findFirst().orElseThrow().industry();
        assertThat(industry.researchAllocation()).isEqualTo(5);
        assertThat(industry.composition().levels()).containsOnlyKeys(0);
    }

    @Test void raisingBudgetClampsExpansionBeforeMiningAndRoundingCannotLoseTheEmpiresSmallContributions() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        assertThat(economy.allocateProduction(state, 1, 1, 6, 4)).isTrue();
        state.planShipResearch(1, 50);
        assertThat(state.industries().get(1).expansionAllocation()).isEqualTo(1);
        assertThat(state.metals().orElseThrow().mining()).containsEntry(1, 4);
        state.planShipResearch(1, 90);
        assertThat(state.industries().get(1).expansionAllocation()).isZero();
        assertThat(state.metals().orElseThrow().mining()).containsEntry(1, 1);
        state.planShipResearch(1, 10);
        assertThat(state.researchReservations().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(1);
        assertThatCode(() -> GameState.fromSnapshot(GameState.toSnapshot(state))).doesNotThrowAnyException();
    }
}
