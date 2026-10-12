package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.domain.OrderPlanning;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.testsupport.MetalStates;
import de.zettsystems.starfare.testsupport.RecyclingStates;
import de.zettsystems.starfare.turn.application.RecyclingSpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class RecyclingTest {
    private final DefaultEconomyService economy = new DefaultEconomyService();
    private final DefaultFleetService fleets = new DefaultFleetService();
    private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();

    @Test void planningIsEditableAndDoesNotCreditOrDestroyShips() {
        var state = RecyclingStates.running();
        assertThat(RecyclingOrders.plan(state, 1, 1, 5)).isTrue();
        assertThat(state.getSystem(1).garrison()).isEqualTo(12);
        assertThat(state.metals().orElseThrow().stocks()).containsEntry(1, 0L);
        assertThat(state.metalPreview().ships().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(3);
        assertThat(RecyclingOrders.plan(state, 1, 1, 2)).isTrue();
        assertThat(state.recyclingShips(1)).isEqualTo(2);
        assertThat(RecyclingOrders.plan(state, 1, 1, 0)).isTrue();
        assertThat(state.recyclingShips(1)).isZero();
    }
    @Test void recyclingSharesReservationsWithDispatchBatchEditingUndoAndReserves() {
        var state = RecyclingStates.running();
        assertThat(fleets.queueSend(state, 1, 1, 2, 4)).isTrue();
        assertThat(RecyclingOrders.plan(state, 1, 1, 9)).isFalse();
        assertThat(RecyclingOrders.plan(state, 1, 1, 8)).isTrue();
        assertThat(fleets.queueSend(state, 1, 1, 2, 1)).isFalse();
        assertThat(OrderPlanning.dispatch(state, 1, 2, List.of(new FleetDispatch(1, 1)))).isFalse();
        assertThat(OrderPlanning.reserves(state, 1, List.of(1), 1, false)).isFalse();
        assertThat(fleets.sendFleet(state, 1, 1, 2, 5)).isFalse();
        assertThat(GameState.toSnapshot(state).metals()).isEqualTo(state.metals().orElseThrow());
    }
    @Test void tenSeparateShipsRecoverTheSameMetalAsOneBatchWithoutRoundingLoss() {
        var batch = RecyclingStates.running(); var split = RecyclingStates.running();
        assertThat(RecyclingOrders.plan(batch, 1, 1, 10)).isTrue(); batch.recycleShips();
        for (int count = 0; count < 10; count++) {
            assertThat(RecyclingOrders.plan(split, 1, 1, 1)).isTrue(); split.recycleShips();
        }
        assertThat(split.metals()).isEqualTo(batch.metals());
        assertThat(split.metals().orElseThrow().stocks()).containsEntry(1, 7L);
        assertThat(split.metals().orElseThrow().recycling().recyclingLossTenths()).isEqualTo(30);
        assertThat(RecyclingOrders.balanceTenths(split)).isEqualTo(split.metals().orElseThrow().recycling().initialTenths());
    }
    @Test void oneShipPreservesSevenTenthsAcrossCopiesAndNeverCreditsTwice() {
        var state = RecyclingStates.running();
        assertThat(RecyclingOrders.plan(state, 1, 1, 1)).isTrue();
        var events = state.recycleShips();
        assertThat(events).containsExactly(new TurnEvent.Recycling(1, 1, "Alpha", 1, 7));
        var copy = GameState.fromSnapshot(GameState.toSnapshot(GameState.copyOf(state)));
        assertThat(copy.metals().orElseThrow().recycling().fractions()).containsEntry(1, 7);
        assertThat(copy.recycleShips()).isEmpty();
        assertThat(RecyclingOrders.balanceTenths(copy)).isEqualTo(copy.metals().orElseThrow().recycling().initialTenths());
    }
    @Test void dispatchPrecedesRecyclingWhichPrecedesProductionAndRelocations() {
        var state = RecyclingStates.running();
        assertThat(fleets.queueSend(state, 1, 1, 2, 2)).isTrue();
        assertThat(RecyclingOrders.plan(state, 1, 1, 10)).isTrue();
        assertThat(state.metalPreview().ships()).containsEntry(1, 4).containsEntry(2, 3);
        advance(state, false);
        assertThat(state.getSystem(1).garrison()).isEqualTo(4);
        assertThat(state.getSystem(2).garrison()).isEqualTo(6);
        assertThat(state.fleets()).hasSize(1);
        assertThat(state.reports().get(1).events().getFirst()).isInstanceOf(TurnEvent.Recycling.class);
        assertThat(state.recyclingShips(1)).isZero();
        assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(state.metals().orElseThrow().recycling().initialTenths());
        long loss = state.metals().orElseThrow().recycling().recyclingLossTenths();
        advance(state, false);
        assertThat(state.metals().orElseThrow().recycling().recyclingLossTenths()).isEqualTo(loss);
    }
    @Test void oldRulesForeignSystemsNeutralSystemsAndInvalidAmountsRejectRecycling() {
        var state = RecyclingStates.running(); var before = GameState.toSnapshot(state);
        assertThat(RecyclingOrders.plan(state, 1, 3, 1)).isFalse();
        assertThat(RecyclingOrders.plan(state, 1, 4, 1)).isFalse();
        assertThat(RecyclingOrders.plan(state, 1, 99, 1)).isFalse();
        assertThat(RecyclingOrders.plan(state, 1, 1, -1)).isFalse();
        assertThat(RecyclingOrders.plan(state, 1, 1, Integer.MAX_VALUE)).isFalse();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(RecyclingOrders.plan(MetalStates.running(0, 0), 1, 1, 1)).isFalse();
        var target1 = RulesetCatalog.builtIn();
        assertThatThrownBy(() -> target1.requireCreatable(RulesetRef.SPACEWARD_RECYCLING)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void combatLossIncludesNeutralShipsAndConservesTheWholeGalaxy() {
        var state = RecyclingStates.running();
        state.players().forEach(player -> state.intel().put(player.id(), new java.util.HashMap<>()));
        assertThat(fleets.queueSend(state, 2, 3, 4, 20)).isTrue();
        for (int count = 0; count < 20 && state.metals().orElseThrow().recycling().combatLossTenths() == 0; count++) {
            advance(state, false);
            assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(state.metals().orElseThrow().recycling().initialTenths());
        }
        assertThat(state.metals().orElseThrow().recycling().combatLossTenths()).isPositive();
    }
    @Test void forecastIsReadOnlyAndEnemyViewsDoNotExposeRecycling() {
        var state = RecyclingStates.running();
        assertThat(RecyclingOrders.plan(state, 1, 1, 2)).isTrue();
        var before = GameState.toSnapshot(state);
        var owned = views.forPlayer(state, 1).systems().stream().filter(system -> system.id() == 1).findFirst().orElseThrow();
        assertThat(owned.availableShips()).isEqualTo(10);
        assertThat(owned.industry().metal().recycling().ships()).isEqualTo(2);
        var enemy = views.forPlayer(state, 2).systems().stream().filter(system -> system.id() == 1).findFirst().orElseThrow();
        assertThat(enemy.industry()).isNull();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
    }
    @Test void relocationUsesSurvivorsAfterRecyclingAndActualProduction() {
        var state = RecyclingStates.running();
        assertThat(RecyclingOrders.plan(state, 1, 1, 10)).isTrue();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 4)).isPositive();
        advance(state, false);
        assertThat(state.getSystem(1).garrison()).isEqualTo(2);
        assertThat(state.fleets()).singleElement().extracting(Fleet::ships).isEqualTo(4);
        assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(state.metals().orElseThrow().recycling().initialTenths());
    }
    @Test void productionCannotConsumeAnUnsettledForecastAndArrivalsWaitForTheNextRound() {
        var state = RecyclingStates.running();
        assertThat(RecyclingOrders.plan(state, 1, 1, 1)).isTrue();
        Map<Integer, Integer> input2 = Map.of();
        assertThatThrownBy(() -> economy.produce(state, input2)).isInstanceOf(IllegalStateException.class);
        assertThat(fleets.queueSend(state, 1, 1, 2, 9)).isTrue();
        assertThat(RecyclingOrders.plan(state, 1, 2, 4)).isFalse();
        assertThat(state.getSystem(2).garrison()).isEqualTo(3);
    }
    @ParameterizedTest @EnumSource(AiStrategy.class)
    void everyAiProfilePreservesShipsNearKnownEnemies(AiStrategy strategy) {
        var state = RecyclingStates.running();
        state.updateSystem(3, system -> system.relocateTo(220, 100));
        state.updatePlayer(1, player -> new Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), strategy));
        new DefaultSpacewardPlanning(economy, views).plan(state, fleets);
        assertThat(state.recyclingShips(1)).isZero();
        assertThat(state.metals().orElseThrow().recycling().recyclingLossTenths()).isZero();
    }
    @ParameterizedTest @EnumSource(AiStrategy.class)
    void everyAiProfileRecyclesLegallyAndSurvivesRestoration(AiStrategy strategy) {
        var state = RecyclingStates.running();
        state.players().replaceAll(player -> new Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), strategy));
        advance(state, true);
        var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        assertThat(state.metals().orElseThrow().recycling().recyclingLossTenths()).isPositive();
        advance(restored, true);
        assertThat(restored.turn()).isEqualTo(3);
        assertThat(RecyclingOrders.balanceTenths(restored)).isEqualTo(restored.metals().orElseThrow().recycling().initialTenths());
    }
    private void advance(GameState state, boolean ai) {
        var reports = new DefaultReportService();
        var engine = new RecyclingSpacewardTurnEngine(new RoundPipeline(new DefaultCombatService(reports), reports, fleets),
                economy, ai ? new DefaultSpacewardPlanning(economy, views) : (_, _) -> { }, fleets, reports);
        engine.advanceTurn(state);
    }
}
