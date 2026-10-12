package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.testsupport.MetalStates;
import de.zettsystems.starfare.turn.application.TurnEngine;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class MetalSnapshotTest extends AbstractIntegrationTest {
    @Autowired private ObjectMapper mapper;
    @Autowired private GameSessionStore store;
    @Autowired private GameSessionRepository repository;
    @Autowired private GameArchiveStore archives;
    @Autowired private TurnEngine turns;
    @Autowired private GameRegistry registry;
    @Autowired private GameService games;

    @Test void databaseStartupPreservesDepletionAndResolvesTheNextRound() {
        var state = MetalStates.running(2, 5);
        new DefaultEconomyService().allocateProduction(state, 1, 1, 1, 2);
        turns.advanceTurn(state);
        var id = GameId.newId();
        try {
            store.save(new GameSession(id, "W2 restart", null, Instant.now(), state));
            var restarted = new JpaGameSessionStore(repository, archives, mapper);
            restarted.loadFromDatabase();
            var restored = restarted.load(id).orElseThrow().readState(GameState::copyOf);
            assertThat(restored.metals()).isEqualTo(state.metals());
            assertThat(restored.ruleset()).isEqualTo(RulesetRef.SPACEWARD_METALS);
            assertThat(restored.replayFrames()).isEqualTo(state.replayFrames());
            assertThat(restored.metals().orElseThrow().deposits().get(1).remaining()).isZero();
            int garrison = restored.getSystem(1).garrison();
            turns.advanceTurn(restored);
            assertThat(restored.turn()).isEqualTo(3);
            assertThat(restored.getSystem(1).garrison()).isEqualTo(garrison);
        } finally { store.delete(id); }
    }
    @Test void newAndHistoricalFieldsSurviveJsonAndOldRulesNeedNoResources() {
        var state = MetalStates.running(2, 5);
        var json = mapper.writeValueAsString(GameState.toSnapshot(state));
        var restored = GameState.fromSnapshot(mapper.readValue(json, GameStateSnapshot.class));
        assertThat(GameState.toSnapshot(restored)).isEqualTo(GameState.toSnapshot(state));
        ObjectNode old = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(ColonyStates.running()));
        old.remove("metals");
        assertThat(GameState.fromSnapshot(mapper.treeToValue(old, GameStateSnapshot.class)).metals()).isEmpty();
        ObjectNode missing = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        missing.remove("metals");
        var input1 = mapper.treeToValue(missing, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(input1)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Missing stored metal");
        ObjectNode history = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) history.path("replayFrames").path("1")).remove("metals");
        var input2 = mapper.treeToValue(history, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(input2)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Incomplete stored metal");
    }
    @Test void orchestratorRejectsStaleRoundsAndSubmittedSeatsBeforeChangingResources() {
        var state = MetalStates.running(2, 5);
        var id = GameId.newId();
        try {
            store.save(new GameSession(id, "W2 planning", null, Instant.now(), state));
            assertThat(games.allocateProduction(id, 1, 0, 1, 1, 2)).isFalse();
            assertThat(games.allocateProduction(id, 2, 1, 1, 1, 2)).isFalse();
            assertThat(games.allocateProduction(id, 1, 1, 1, 1, 2)).isTrue();
            assertThat(games.submitTurn(id, 1)).isTrue();
            var before = registry.readState(id, GameState::toSnapshot);
            assertThat(games.allocateProduction(id, 1, 1, 1, 0, 0)).isFalse();
            assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        } finally { store.delete(id); }
    }
}
