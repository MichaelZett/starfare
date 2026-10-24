package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.economy.values.MiningAutomationView;
import de.zettsystems.starfare.economy.values.MiningRequest;
import de.zettsystems.starfare.economy.values.ProductionAllocation;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.MetalStates;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class MiningAutomationServiceTest extends AbstractIntegrationTest {
    @Autowired GameService games;
    @Autowired GameSessionStore store;
    @Autowired GameSessionRepository repository;
    @Autowired GameArchiveStore archives;
    @Autowired ObjectMapper mapper;

    @Test
    void previewIsReadOnlySaveMatchesItAndStaleOrUnauthorizedWritesAreRejected() {
        var id = save();
        try {
            var view = games.miningAutomationFor(id, "owner").orElseThrow();
            var request = reviewed(id, request(view, true));
            var before = registry.readState(id, GameState::toSnapshot);
            var preview = games.previewMining(id, "owner", request).orElseThrow();
            assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
            assertThat(games.miningAutomationFor(id, "stranger")).isEmpty();
            assertThat(games.configureMining(id, "stranger", request)).isFalse();
            assertThat(games.configureMining(id, "owner", request)).isTrue();
            var saved = games.miningAutomationFor(id, "owner").orElseThrow().policy();
            assertThat(saved.lastPlan()).isEqualTo(preview);
            var allocations = registry.readState(id, state -> state.metals().orElseThrow().mining());
            assertThat(allocations).containsAllEntriesOf(preview.allocations());
            before = registry.readState(id, GameState::toSnapshot);
            assertThat(games.configureMining(id, "owner", request)).isFalse();
            assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        } finally { store.delete(id); }
    }

    @Test
    void settingsSurviveDatabaseReloadAndLegacyJsonDefaultsToOff() {
        var id = save();
        try {
            var view = games.miningAutomationFor(id, "owner").orElseThrow();
            assertThat(games.configureMining(id, "owner", reviewed(id, request(view, true)))).isTrue();
            var stored = registry.readState(id, GameState::toSnapshot);
            var restarted = new JpaGameSessionStore(repository, archives, mapper);
            var restored = restarted.load(id).orElseThrow().readState(GameState::copyOf);
            assertThat(GameState.toSnapshot(restored)).isEqualTo(stored);
            restored.nextTurn();
            assertThat(restored.miningAutomation().get(1).plannedTurn()).isEqualTo(restored.turn());
            ObjectNode old = (ObjectNode) mapper.valueToTree(stored);
            old.remove("miningAutomation");
            var legacy = GameState.fromSnapshot(mapper.treeToValue(old, GameStateSnapshot.class));
            assertThat(legacy.miningAutomation()).isEmpty();
            assertThat(legacy.metals()).isEqualTo(restored.metals());
        } finally { store.delete(id); }
    }

    @Test
    void manualProductionLeavesAutomationAndProtectionIsPersistent() {
        var id = save();
        try {
            var initial = games.miningAutomationFor(id, "owner").orElseThrow();
            assertThat(games.configureMining(id, "owner", reviewed(id, request(initial, true)))).isTrue();
            int turn = registry.readState(id, GameState::turn);
            assertThat(games.protectMiningSystem(id, "owner", turn, 1, true)).isTrue();
            assertThat(games.miningAutomationFor(id, "owner").orElseThrow().policy().protectedSystems()).contains(1);
            var view = games.miningAutomationFor(id, "owner").orElseThrow();
            var current = view.plans().get(1);
            int mining = current.mining() == 0 ? 1 : current.mining() - 1;
            assertThat(games.commitProduction(id, "owner", new ProductionAllocation(1, turn, current.expansion(), mining, current)).accepted()).isTrue();
            assertThat(games.miningAutomationFor(id, "owner").orElseThrow().policy().systems()).doesNotContain(1);
            int assigned = registry.readState(id, state -> state.metals().orElseThrow().mining().get(1));
            assertThat(assigned).isEqualTo(mining);
            assertThat(games.protectMiningSystem(id, "owner", turn, 999, true)).isFalse();
            registry.writeState(id, state -> { state.submittedThisTurn().add(1); return null; });
            view = games.miningAutomationFor(id, "owner").orElseThrow();
            assertThat(view.editable()).isFalse();
            assertThat(games.configureMining(id, "owner", request(view, false))).isFalse();
        } finally { store.delete(id); }
    }

    @Test
    void productionChangesBetweenPreviewAndSaveAndInvalidSelectionsAreRejected() {
        var id = save();
        try {
            var view = games.miningAutomationFor(id, "owner").orElseThrow();
            var request = reviewed(id, request(view, true));
            registry.writeState(id, state -> { state.planMining(2, 1); return null; });
            var before = registry.readState(id, GameState::toSnapshot);
            assertThat(games.configureMining(id, "owner", request)).isFalse();
            assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
            view = games.miningAutomationFor(id, "owner").orElseThrow();
            var invalid = new MiningRequest(view.turn(), view.policy(), view.plans(), true, 1, Set.of(3), Set.of());
            assertThat(games.configureMining(id, "owner", invalid)).isFalse();
            invalid = new MiningRequest(view.turn(), view.policy(), view.plans(), true, 11, Set.of(1), Set.of());
            assertThat(games.previewMining(id, "owner", invalid)).isEmpty();
        } finally { store.delete(id); }
    }

    private static MiningRequest request(MiningAutomationView view, boolean enabled) {
        return new MiningRequest(view.turn(), view.policy(), view.plans(), enabled, 1, Set.of(1, 2), Set.of());
    }

    private MiningRequest reviewed(GameId id, MiningRequest request) {
        return request.withReviewed(games.previewMining(id, "owner", request).orElseThrow());
    }

    @Test
    void aRouteAddedAfterReviewCannotSilentlyChangeTheApprovedGlobalPlan() {
        var id = save();
        try {
            var view = games.miningAutomationFor(id, "owner").orElseThrow();
            assertThat(games.configureMining(id, "owner", request(view, true))).isFalse();
            var request = reviewed(id, request(view, true));
            int source = request.reviewed().changes().keySet().stream().findFirst().orElseThrow();
            int target = source == 1 ? 2 : 1;
            registry.writeState(id, state -> {
                state.standingOrders().put(1, java.util.List.of(new de.zettsystems.starfare.game.values.StandingOrder(1, 1, source, target, 1)));
                return null;
            });
            var before = registry.readState(id, GameState::toSnapshot);
            assertThat(games.configureMining(id, "owner", request)).isFalse();
            assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        } finally { store.delete(id); }
    }

    private GameId save() {
        var state = MetalStates.running(0, 100);
        state.seatByUser().put("owner", 1);
        var id = GameId.newId();
        store.save(new GameSession(id, "Mining", "owner", Instant.now(), state));
        return id;
    }
}
