package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.values.MiningAutomation;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.testsupport.ColonyStates;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MiningPlannerTest {
    public static GameState ready(long stock, long remaining) {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_EARLY_INDUSTRY, original -> {
            var stocks = new HashMap<>(original.stocks()); stocks.put(1, stock);
            var deposits = new HashMap<>(original.deposits());
            deposits.put(1, new MetalDeposit(remaining, 3)); deposits.put(2, new MetalDeposit(remaining, 1));
            return new MetalEconomy(deposits, stocks, original.mining());
        });
        state.start(); state.seatByUser().put("owner", 1); return state;
    }
    private static MiningAutomation policy(int rounds, Set<Integer> locked) {
        return MiningAutomation.off("owner").configure(true, rounds, Set.of(1, 2), locked);
    }

    @Test
    void oneGlobalReserveUsesEfficientWorldAndDoesNotChangeThePreviewSource() {
        var state = ready(0, 100); var before = GameState.toSnapshot(state);
        var plan = MiningPlanner.plan(state, 1, policy(1, Set.of()));
        assertThat(plan.allocations()).containsEntry(1, 6).containsEntry(2, 0);
        assertThat(plan.demand()).isEqualTo(8);
        assertThat(plan.target()).isEqualTo(8);
        assertThat(plan.stock()).isEqualTo(10);
        assertThat(plan.shortfall()).isZero();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        Collections.reverse(state.systems());
        assertThat(MiningPlanner.plan(state, 1, policy(1, Set.of()))).isEqualTo(plan);
    }

    @Test
    void zeroReserveStillCoversUnrationedDemandInsteadOfAcceptingStarvedZeroConsumption() {
        var plan = MiningPlanner.plan(ready(0, 100), 1, policy(0, Set.of()));
        assertThat(plan.allocations()).containsEntry(1, 4).containsEntry(2, 0);
        assertThat(plan.demand()).isEqualTo(10);
        assertThat(plan.shortfall()).isZero();
    }

    @Test
    void locksAndFiniteDepositsProduceAnHonestUnmetReserve() {
        var locked = MiningPlanner.plan(ready(0, 100), 1, policy(1, Set.of(1)));
        assertThat(locked.allocations()).containsEntry(1, 0);
        assertThat(locked.shortfall()).isPositive();
        var finite = MiningPlanner.plan(ready(0, 1), 1, policy(1, Set.of()));
        assertThat(finite.allocations().values()).allMatch(points -> points <= 1);
        assertThat(finite.shortfall()).isPositive();
        var empty = MiningPlanner.plan(ready(0, 0), 1, policy(1, Set.of()));
        assertThat(empty.allocations().values()).containsOnly(0);
        assertThat(empty.shortfall()).isPositive();
    }

    @Test
    void existingRoutesAndTheirProductionArePreserved() {
        var state = ready(10, 100);
        var route = new StandingOrder(1, 1, 1, 2, 1);
        state.standingOrders().put(1, List.of(route));
        int production = state.expectedShipProduction(1);
        state.miningAutomation().put(1, policy(1, Set.of()));
        MiningPlanner.applyFor(state, 1);
        assertThat(state.metals().orElseThrow().mining().get(1)).isZero();
        assertThat(state.standingOrders().get(1)).containsExactly(route);
        assertThat(state.expectedShipProduction(1)).isGreaterThanOrEqualTo(production);
    }

    @Test
    void settingsCopyAndSnapshotSurviveAndNewRoundPlansEvenAfterPreviousSubmission() {
        var state = ready(0, 100); state.miningAutomation().put(1, policy(1, Set.of()));
        var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        var copy = GameState.copyOf(restored); copy.miningAutomation().clear();
        assertThat(restored.miningAutomation()).containsKey(1);
        restored.submittedThisTurn().add(1); restored.nextTurn();
        assertThat(restored.miningAutomation().get(1).plannedTurn()).isEqualTo(2);
        assertThat(restored.metals().orElseThrow().mining().get(1)).isEqualTo(6);
    }

    @Test
    void disabledAndReassignedSeatsDoNotContinueOldOrders() {
        var state = ready(0, 100); state.miningAutomation().put(1, policy(1, Set.of()));
        MiningPlanner.applyFor(state, 1);
        var current = state.miningAutomation().get(1);
        state.miningAutomation().put(1, current.configure(false, 1, current.systems(), current.protectedSystems()));
        var mining = state.metals().orElseThrow().mining(); state.nextTurn();
        assertThat(state.metals().orElseThrow().mining()).isEqualTo(mining);
        state.miningAutomation().put(1, policy(1, Set.of()));
        state.seatByUser().remove("owner"); state.seatByUser().put("replacement", 1); state.nextTurn();
        assertThat(state.miningAutomation()).doesNotContainKey(1);
    }

    @Test
    void lostWorldsAreRemovedAndNeverReenrolledOnRecapture() {
        var state = ready(0, 100); state.miningAutomation().put(1, policy(1, Set.of()));
        state.updateSystem(1, system -> system.captureBy(2, system.garrison())); state.nextTurn();
        assertThat(state.miningAutomation().get(1).systems()).containsExactly(2);
        state.updateSystem(1, system -> system.captureBy(1, system.garrison())); state.nextTurn();
        assertThat(state.miningAutomation().get(1).systems()).containsExactly(2);
    }

    @Test
    void researchExpansionRecyclingAndRealProductionUseTheSameForecast() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH); state.start(); state.seatByUser().put("owner", 1);
        state.planShipResearch(1, 25);
        var economy = new DefaultEconomyService();
        assertThat(economy.allocateProduction(state, 1, 1, 2, 0)).isTrue();
        state.planRecycling(1, 1);
        state.miningAutomation().put(1, policy(1, Set.of()));
        MiningPlanner.applyFor(state, 1);
        assertThat(state.industries().get(1).expansionAllocation()).isEqualTo(2);
        assertThat(state.metals().orElseThrow().research().empires().get(1).percent()).isEqualTo(25);
        var preview = state.metalPreview();
        long stock = preview.economy().stocks().get(1);
        state.recycleShips(); economy.produce(state, Map.of());
        assertThat(state.metals().orElseThrow().stocks().get(1)).isEqualTo(stock);
        assertThatCode(() -> GameState.toSnapshot(state)).doesNotThrowAnyException();
    }
}
