package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.economy.values.ProductionAllocation;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.MetalStates;
import de.zettsystems.starfare.testsupport.RecyclingStates;
import de.zettsystems.starfare.testsupport.SpacewardStates;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionPreviewTest extends AbstractIntegrationTest {
    @Test void optimisticProductionCommitRejectsAnExternalEditAndStaleRound() {
        save(MetalStates.running(2, 5));
        var saved = de.zettsystems.starfare.economy.values.ProductionPreset.saved(games.viewFor(id, 1).systems().getFirst().industry());
        var command = new ProductionAllocation(1, 1, 1, 2, saved);
        assertThat(games.commitProduction(id, "stranger", command).accepted()).isFalse();
        assertThat(games.allocateProduction(id, 1, 1, 1, 2, 1)).isTrue();
        var before = registry.readState(id, GameState::toSnapshot);
        assertThat(games.commitProduction(id, "preview-owner", command).accepted()).isFalse();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        var fresh = de.zettsystems.starfare.economy.values.ProductionPreset.saved(games.viewFor(id, 1).systems().getFirst().industry());
        assertThat(games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 0, 1, 2, fresh)).accepted()).isFalse();
        assertThat(games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 1, 1, 2, fresh)).accepted()).isTrue();
    }
    @Autowired GameService games;
    @Autowired GameRegistry registry;
    @Autowired GameSessionStore store;
    private GameId id;

    private void save(GameState state) {
        state.seatByUser().put("preview-owner", 1);
        id = GameId.newId();
        store.save(new GameSession(id, "Production preview", "preview-owner", Instant.now(), state));
    }

    @AfterEach void cleanup() { if (id != null) { store.delete(id); } }

    @Test void previewsTheEmpireWithoutChangingMemoryOrStorage() {
        save(MetalStates.running(2, 5));
        var before = registry.readState(id, GameState::toSnapshot);
        var plan = new ProductionAllocation(1, 1, 1, 2);
        var result = games.previewProduction(id, "preview-owner", plan).orElseThrow();
        assertThat(result.industry().metal().extraction()).isEqualTo(5);
        assertThat(result.industry().metal().actualShips()).isEqualTo(3);
        assertThat(result.empire().metal().extraction()).isGreaterThanOrEqualTo(5);
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        assertThat(store.load(id).orElseThrow().readState(GameState::toSnapshot)).isEqualTo(before);
        assertThat(games.allocateProduction(id, 1, 1, 1, 1, 2)).isTrue();
        assertThat(games.viewFor(id, 1).systems().getFirst().industry()).isEqualTo(result.industry());
    }

    @Test void rejectsForeignSystemsUnknownAccountsInvalidBudgetsAndStaleRounds() {
        save(MetalStates.running(2, 5));
        assertThat(games.previewProduction(id, "stranger", new ProductionAllocation(1, 1, 0, 0))).isEmpty();
        assertThat(games.previewProduction(id, "preview-owner", new ProductionAllocation(3, 1, 0, 0))).isEmpty();
        assertThat(games.previewProduction(id, "preview-owner", new ProductionAllocation(1, 0, 0, 0))).isEmpty();
        assertThat(games.previewProduction(id, "preview-owner", new ProductionAllocation(1, 1, 50, 50))).isEmpty();
        registry.writeState(id, state -> { state.submittedThisTurn().add(1); return null; });
        assertThat(games.previewProduction(id, "preview-owner", new ProductionAllocation(1, 1, 0, 0))).isEmpty();
    }

    @Test void olderIndustrialRulesUseTheSamePreviewWithoutMetal() {
        save(SpacewardStates.running());
        var result = games.previewProduction(id, "preview-owner", new ProductionAllocation(1, 1, 4)).orElseThrow();
        assertThat(result.industry().shipbuilding()).isEqualTo(6);
        assertThat(result.industry().metal()).isNull();
        assertThat(result.empire().metal()).isNull();
    }

    private static GameState directEconomy() {
        var state = de.zettsystems.starfare.testsupport.ColonyStates.ready(
                de.zettsystems.starfare.game.values.RulesetRef.SPACEWARD_DIRECT_ECONOMY, original -> {
                    var stocks = new java.util.HashMap<>(original.stocks());
                    stocks.replaceAll((_, _) -> 100L);
                    return new de.zettsystems.starfare.economy.domain.MetalEconomy(original.deposits(), stocks,
                            original.mining(), original.recycling());
                });
        state.start();
        var fleets = new de.zettsystems.starfare.fleet.application.DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 4)).isPositive();
        return state;
    }

    private java.util.Map<Integer, Integer> routes() {
        return registry.readState(id, state -> state.standingOrders().get(1).stream().collect(
                java.util.stream.Collectors.toMap(order -> order.toSystemId(), order -> order.ships())));
    }

    private de.zettsystems.starfare.economy.values.AllocationSplit saved() {
        return de.zettsystems.starfare.economy.values.ProductionPreset.saved(
                games.viewFor(id, 1).systems().getFirst().industry());
    }

    @Test void lessShipbuildingShortensOutgoingRelocationsInTheSameSave() {
        save(directEconomy());
        var preview = games.previewProduction(id, "preview-owner", new ProductionAllocation(1, 1, 4, 0)).orElseThrow();
        assertThat(preview.relocations()).extracting(adjustment -> adjustment.toSystemId() + ":" + adjustment.before() + "→" + adjustment.after())
                .containsExactlyInAnyOrder("2:6→4", "3:4→2");
        assertThat(routes()).containsEntry(2, 6).containsEntry(3, 4);
        var result = games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 1, 4, 0, saved()));
        assertThat(result.accepted()).isTrue();
        assertThat(result.relocations()).isEqualTo(preview.relocations());
        assertThat(routes()).containsEntry(2, 4).containsEntry(3, 2);
        assertThat(games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 1, 0, 0, saved())).relocations()).isEmpty();
        assertThat(routes()).containsEntry(2, 4).containsEntry(3, 2);
    }

    @Test void rejectedPlansAndOlderRulesNeverTouchRelocations() {
        save(directEconomy());
        var before = registry.readState(id, GameState::toSnapshot);
        var stale = new ProductionAllocation(1, 1, 4, 0, new de.zettsystems.starfare.economy.values.AllocationSplit(1, 1, 8));
        assertThat(games.commitProduction(id, "preview-owner", stale).accepted()).isFalse();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        cleanup();
        var limited = de.zettsystems.starfare.testsupport.MetalStates.running(100, 500);
        new de.zettsystems.starfare.fleet.application.DefaultFleetService().addStandingOrder(limited, 1, 1, 2, 6);
        save(limited);
        var result = games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 1, 4, 0, saved()));
        assertThat(result.accepted()).isTrue();
        assertThat(result.relocations()).isEmpty();
        assertThat(routes()).containsEntry(2, 6);
    }

    @Test void incomingRoutesAndGarrisonSurplusFollowTheRule() {
        var state = directEconomy();
        var fleets = new de.zettsystems.starfare.fleet.application.DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 2, 1, 2)).isPositive();
        assertThat(state.getSystem(1).availableShips()).isPositive();
        save(state);
        var result = games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 1, 6, 0, saved()));
        assertThat(result.relocations()).extracting(adjustment -> adjustment.after()).containsExactlyInAnyOrder(4, 2);
        assertThat(routes()).containsEntry(2, 4).containsEntry(3, 2).containsEntry(1, 2);
    }

    @Test void includesReservedRecyclingInTheSharedStockForecast() {
        var state = RecyclingStates.ready();
        state.start();
        save(state);
        assertThat(games.planRecycling(id, 1, 1, 1, 9)).isTrue();
        var result = games.previewProduction(id, "preview-owner", new ProductionAllocation(1, 1, 0, 0)).orElseThrow();
        assertThat(result.empire().metal().recycling()).isEqualTo(6);
        assertThat(result.industry().metal().recycling().ships()).isEqualTo(9);
        assertThat(result.industry().metal().actualShips()).isPositive();
    }
}
