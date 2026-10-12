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
        assertThat(games.commitProduction(id, "stranger", command)).isFalse();
        assertThat(games.allocateProduction(id, 1, 1, 1, 2, 1)).isTrue();
        var before = registry.readState(id, GameState::toSnapshot);
        assertThat(games.commitProduction(id, "preview-owner", command)).isFalse();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        var fresh = de.zettsystems.starfare.economy.values.ProductionPreset.saved(games.viewFor(id, 1).systems().getFirst().industry());
        assertThat(games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 0, 1, 2, fresh))).isFalse();
        assertThat(games.commitProduction(id, "preview-owner", new ProductionAllocation(1, 1, 1, 2, fresh))).isTrue();
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
