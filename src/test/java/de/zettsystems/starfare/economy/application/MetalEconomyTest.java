package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.economy.domain.MetalDeposit;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.testsupport.MetalStates;
import de.zettsystems.starfare.turn.application.MetalSpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class MetalEconomyTest {
    private final DefaultEconomyService economy = new DefaultEconomyService();
    private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();

    @Test void miningAndConstructionConserveMetalAndLeaveNeutralEconomiesUntouched() {
        var state = MetalStates.running(2, 5);
        var neutral = state.metals().orElseThrow().deposits().get(4);
        var population = state.colonies().get(4);
        long original = material(state);
        assertThat(economy.allocateProduction(state, 1, 1, 1, 2)).isTrue();
        for (int round = 0; round < 5; round++) {
            economy.produce(state, Map.of());
            assertThat(material(state)).isEqualTo(original);
        }
        assertThat(state.metals().orElseThrow().deposits()).containsEntry(4, neutral);
        assertThat(state.colonies()).containsEntry(4, population);
    }

    @Test void allMiningLeavesNoShipDemandAndCannotUseTheSameCapacityForExpansion() {
        var state = MetalStates.running(2, 5);
        assertThat(economy.allocateProduction(state, 1, 1, 0, 6)).isTrue();
        assertThat(economy.allocateProduction(state, 1, 2, 0, 4)).isTrue();
        var forecast = state.metalPreview();
        assertThat(forecast.ships()).containsEntry(1, 0).containsEntry(2, 0);
        assertThat(forecast.economy().stocks()).containsEntry(1, 19L);
        assertThat(economy.allocateExpansion(state, 1, 1, 1)).isFalse();
        var input1 = Map.of(1, 1);
        assertThatThrownBy(() -> economy.produce(state, input1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static long material(GameState state) {
        var resources = state.metals().orElseThrow();
        return resources.deposits().values().stream().mapToLong(MetalDeposit::remaining).sum()
                + resources.stocks().values().stream().mapToLong(Long::longValue).sum()
                + state.systems().stream().mapToLong(StarSystem::garrison).sum()
                + state.fleets().stream().mapToLong(Fleet::ships).sum();
    }

    @Test void scarcityUsesLargestRemainderAndStableSystemIdsRegardlessOfIterationOrder() {
        var state = MetalStates.running(2, 5);
        var before = GameState.toSnapshot(state);
        assertThat(state.metalPreview().ships()).containsEntry(1, 1).containsEntry(2, 1);
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        Collections.reverse(state.systems());
        assertThat(state.metalPreview().ships()).containsEntry(1, 1).containsEntry(2, 1);
        assertThat(economy.allocateProduction(state, 1, 1, 2, 0)).isTrue();
        assertThat(economy.allocateProduction(state, 1, 2, 0, 0)).isTrue();
        var tie = MetalStates.running(1, 5);
        assertThat(economy.allocateProduction(tie, 1, 1, 2, 0)).isTrue();
        assertThat(tie.metalPreview().ships()).containsEntry(1, 1).containsEntry(2, 0);
    }
    @Test void extractionIsFiniteAndAvailableForShipbuildingInTheSameRound() {
        var state = MetalStates.running(2, 5);
        assertThat(economy.allocateProduction(state, 1, 1, 1, 2)).isTrue();
        var forecast = state.metalPreview();
        assertThat(forecast.extracted()).containsEntry(1, 5L);
        assertThat(forecast.ships()).containsEntry(1, 3).containsEntry(2, 4);
        assertThat(forecast.economy().stocks()).containsEntry(1, 0L);
        assertThat(economy.produce(state, Map.of()).stream().filter(p -> p.ownerId() == 1).mapToInt(p -> p.ships()).sum()).isEqualTo(7);
        assertThat(state.getSystem(1).garrison()).isEqualTo(15);
        assertThat(state.metals().orElseThrow().deposits().get(1).remaining()).isZero();
        assertThat(state.colonies().get(1).colony().population()).isEqualTo(711);
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(4);
        assertThat(state.metalPreview().ships()).containsEntry(1, 0).containsEntry(2, 0);
    }
    @Test void stockPersistsUnusedCapacityCreatesNoDebtAndExpansionDoesNotConsumeMetal() {
        var state = MetalStates.running(0, 0);
        assertThat(economy.allocateProduction(state, 1, 1, 3, 0)).isTrue();
        economy.produce(state, Map.of());
        assertThat(state.industries().get(1).expansionProgress()).isEqualTo(3);
        assertThat(state.getSystem(1).garrison()).isEqualTo(12);
        assertThat(state.metals().orElseThrow().stocks()).containsEntry(1, 0L);
        var abundant = MetalStates.running(100, 50);
        economy.produce(abundant, Map.of());
        assertThat(abundant.metals().orElseThrow().stocks()).containsEntry(1, 90L);
    }
    @Test void combinedAllocationsAreAtomicAndRejectWrongOwnerNeutralAndOverCapacity() {
        var state = MetalStates.running(2, 5);
        var before = GameState.toSnapshot(state);
        assertThat(economy.allocateProduction(state, 2, 1, 1, 2)).isFalse();
        assertThat(economy.allocateProduction(state, 1, 4, 0, 1)).isFalse();
        assertThat(economy.allocateProduction(state, 1, 999, 0, 1)).isFalse();
        assertThat(economy.allocateProduction(state, 1, 1, -1, 2)).isFalse();
        assertThat(economy.allocateProduction(state, 1, 1, 1, -2)).isFalse();
        assertThat(economy.allocateProduction(state, 1, 1, Integer.MAX_VALUE, 1)).isFalse();
        assertThat(economy.allocateProduction(state, 1, 1, 5, 2)).isFalse();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(economy.allocateProduction(state, 1, 1, 2, 4)).isTrue();
        before = GameState.toSnapshot(state);
        assertThat(economy.allocateExpansion(state, 1, 1, 3)).isFalse();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(economy.allocateProduction(state, 1, 1, 4, 2)).isTrue();
    }
    @Test void conquestKeepsGeologyAndEmpireStocksButClearsTheFormerOwnersMiningPlan() {
        var state = MetalStates.running(2, 5);
        economy.allocateProduction(state, 1, 1, 1, 2);
        var old = state.metals().orElseThrow();
        state.updateSystem(1, system -> system.captureBy(2, 5));
        assertThat(state.metals().orElseThrow().deposits()).isEqualTo(old.deposits());
        assertThat(state.metals().orElseThrow().stocks()).isEqualTo(old.stocks());
        assertThat(state.metals().orElseThrow().mining()).containsEntry(1, 0);
        assertThat(state.industries().get(1).expansionAllocation()).isZero();
    }
    @Test void realRoundRelocatesOnlyActuallyBuiltShipsAndPreservesReserve() {
        var state = MetalStates.running(10, 0);
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        var snapshot = GameState.toSnapshot(state);
        // A fixture with the same orders but depleted stock models a later shortage.
        var limited = MetalStates.running(0, 0);
        limited.standingOrders().putAll(snapshot.standingOrders());
        advance(limited, fleets);
        assertThat(limited.getSystem(1).garrison()).isEqualTo(10);
        assertThat(limited.getSystem(2).garrison()).isEqualTo(3);
        assertThat(limited.fleets()).hasSize(1);
        assertThat(limited.fleets().getFirst().ships()).isEqualTo(2);
        assertThat(limited.reports().get(1).events()).isNotEmpty();
        assertThat(limited.turn()).isEqualTo(2);
        assertThat(limited.metals().orElseThrow().stocks()).containsEntry(1, 0L);
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(limited)).metals()).isEqualTo(limited.metals());
    }
    @Test void regularDispatchReservesShipsBeforeProductionRelocations() {
        var state = MetalStates.running(10, 0);
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        assertThat(fleets.queueSend(state, 1, 1, 2, 2)).isTrue();
        assertThat(economy.allocateProduction(state, 1, 1, 3, 0)).isTrue();
        var preview = views.forPlayer(state, 1).systems().stream().filter(s -> s.id() == 1).findFirst().orElseThrow();
        assertThat(preview.industry().nextDelivery()).isEqualTo(3);
        advance(state, fleets);
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
        assertThat(state.fleets().stream().mapToInt(Fleet::ships).sum()).isEqualTo(5);
    }
    @Test void viewsUseTheSharedForecastAndHideExactResourcesBehindFog() {
        var state = MetalStates.running(2, 5);
        var view = views.forPlayer(state, 1);
        var alpha = view.systems().stream().filter(s -> s.id() == 1).findFirst().orElseThrow();
        assertThat(alpha.productionPerTurn()).isEqualTo(1);
        assertThat(alpha.industry().metal().stock()).isEqualTo(2);
        assertThat(alpha.industry().metal().actualShips()).isEqualTo(1);
        assertThat(view.systems().stream().filter(s -> s.id() == 3).findFirst().orElseThrow().industry()).isNull();
        var frame = state.replayFrames().get(1);
        economy.allocateProduction(state, 1, 1, 1, 2);
        economy.produce(state, Map.of());
        var historical = views.forReplay(state, frame, 1).systems().getFirst();
        assertThat(historical.industry().metal().remaining()).isEqualTo(5);
        assertThat(historical.industry().metal().stock()).isEqualTo(2);
        assertThat(historical.productionPerTurn()).isEqualTo(1);
        assertThat(views.forObserver(state).systems().getFirst().industry().metal().remaining()).isZero();
    }
    @Test void copiesAndSnapshotsPreserveResourcesWithoutMiningOnRead() {
        var state = MetalStates.running(2, 5);
        economy.allocateProduction(state, 1, 1, 1, 2);
        var before = GameState.toSnapshot(state);
        var copy = GameState.copyOf(state);
        assertThat(GameState.toSnapshot(copy)).isEqualTo(before);
        economy.produce(copy, Map.of());
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(copy.metals()).isNotEqualTo(state.metals());
        copy.resetForNewGame();
        assertThat(copy.metals()).isEmpty();
    }
    @Test void copyingALaterRoundPreservesDeadlinesSubmissionsAndUniqueFleetNumbers() {
        var state = MetalStates.running(2, 5);
        var fleets = new DefaultFleetService();
        assertThat(fleets.sendFleet(state, 1, 1, 2, 1)).isTrue();
        state.nextTurn();
        state.submittedThisTurn().add(1);
        state.updateStragglerClock(state.turnStartedAt().plusSeconds(5));
        state.waitThisTurn().add(state.fleets().getFirst().globalId());
        var copy = GameState.copyOf(state);
        assertThat(GameState.toSnapshot(copy)).isEqualTo(GameState.toSnapshot(state));
        assertThat(fleets.sendFleet(copy, 1, 1, 2, 1)).isTrue();
        assertThat(copy.fleets().stream().map(Fleet::globalId)).containsExactly(1, 2);
        assertThat(copy.fleets().stream().map(Fleet::localNo)).containsExactly(1, 2);
        assertThat(state.fleets()).hasSize(1);
    }
    @Test void deliveryPreviewDoesNotOverflowAtMaximumGarrison() {
        var state = MetalStates.running(10, 0);
        state.updateSystem(1, system -> system.reinforce(Integer.MAX_VALUE - system.garrison()));
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        var home = views.forPlayer(state, 1).systems().stream()
                .filter(system -> system.id() == 1).findFirst().orElseThrow();
        assertThat(home.industry().nextDelivery()).isEqualTo(6);
        assertThat(state.getSystem(1).garrison()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test void resourcesCannotBeCreatedPubliclyOrRetrofittedIntoW1() {
        assertThatCode(() -> RulesetCatalog.builtIn().requireSupported(RulesetRef.SPACEWARD_METALS)).doesNotThrowAnyException();
        var target2 = RulesetCatalog.builtIn();
        assertThatThrownBy(() -> target2.requireCreatable(RulesetRef.SPACEWARD_METALS)).isInstanceOf(IllegalArgumentException.class);
        var legacy = ColonyStates.ready();
        var input3 = new MetalEconomy(Map.of(), Map.of(), Map.of());
        assertThatThrownBy(() -> legacy.establishMetals(input3)).isInstanceOf(IllegalArgumentException.class);
        assertThat(economy.allocateProduction(legacy, 1, 1, 0, 0)).isFalse();
        assertThat(legacy.metals()).isEmpty();
        assertThatThrownBy(() -> new MetalDeposit(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MetalDeposit(1, 0)).isInstanceOf(IllegalArgumentException.class);
        var target4 = new MetalDeposit(1, 1);
        assertThatThrownBy(() -> target4.extract(2)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @EnumSource(AiStrategy.class)
    void everyAiProfileCanMineAndContinueAfterRestoration(AiStrategy strategy) {
        var state = MetalStates.running(0, 50);
        state.players().replaceAll(player -> new Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), strategy));
        advance(state, new DefaultFleetService());
        var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        advance(restored, new DefaultFleetService());
        assertThat(restored.turn()).isEqualTo(3);
        assertThat(restored.metals().orElseThrow().deposits().get(1).remaining()).isLessThan(50);
        assertThat(restored.metals().orElseThrow().stocks().values()).allMatch(stock -> stock >= 0);
    }
    private void advance(GameState state, DefaultFleetService fleets) {
        var reports = new DefaultReportService();
        var engine = new MetalSpacewardTurnEngine(new RoundPipeline(new DefaultCombatService(reports), reports, fleets),
                economy, new DefaultSpacewardPlanning(economy, views), fleets, reports);
        engine.advanceTurn(state);
    }
}
