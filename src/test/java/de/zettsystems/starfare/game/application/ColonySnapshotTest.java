package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.testsupport.SpacewardStates;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ColonySnapshotTest extends AbstractIntegrationTest {
    @Autowired private ObjectMapper mapper;
    @Autowired private GameSessionStore store;
    @Autowired private GameSessionRepository repository;
    @Autowired private GameArchiveStore archives;
    @Autowired private de.zettsystems.starfare.turn.application.TurnEngine turns;

    @Test void databaseStartupRestoresTheInternalVersionAndCanResolveAnotherRound() {
        var state = ColonyStates.running();
        new DefaultEconomyService().produce(state, Map.of());
        var id = de.zettsystems.starfare.game.values.GameId.newId();
        try {
            store.save(new de.zettsystems.starfare.game.domain.GameSession(id, "W1 restart", null, java.time.Instant.now(), state));
            var restarted = new JpaGameSessionStore(repository, archives, mapper);
            restarted.loadFromDatabase();
            var session = restarted.load(id).orElseThrow();
            var restored = session.readState(GameState::copyOf);
            assertThat(restored.colonies()).isEqualTo(state.colonies());
            assertThat(restored.ruleset()).isEqualTo(de.zettsystems.starfare.game.values.RulesetRef.SPACEWARD_COLONIES);
            turns.advanceTurn(restored);
            assertThat(restored.turn()).isEqualTo(2);
            assertThat(restored.colonies().get(1).colony().population()).isEqualTo(733);
        } finally { store.delete(id); }
    }
    @Test void propertiesAndFractionalPopulationSurviveJsonWithoutGrowingOnReload() {
        var state = ColonyStates.running();
        new DefaultEconomyService().produce(state, Map.of());
        var json = mapper.writeValueAsString(GameState.toSnapshot(state));
        var restored = GameState.fromSnapshot(mapper.readValue(json, GameStateSnapshot.class));
        assertThat(restored.colonies()).isEqualTo(state.colonies());
        assertThat(restored.replayFrames().get(1).colonies().get(1).colony().population()).isEqualTo(690);
        assertThat(restored.colonies().get(1).colony().population()).isEqualTo(711);
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(restored)).colonies()).isEqualTo(state.colonies());
    }
    @Test void missingNewFieldsInOldGamesRemainCompatibleButNewGamesRejectMissingProperties() {
        var old = SpacewardStates.running();
        ObjectNode legacy = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(old));
        legacy.remove("colonies");
        assertThat(GameState.fromSnapshot(mapper.treeToValue(legacy, GameStateSnapshot.class)).colonies()).isEmpty();
        ObjectNode incomplete = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(ColonyStates.running()));
        incomplete.remove("colonies");
        assertThatThrownBy(() -> GameState.fromSnapshot(mapper.treeToValue(incomplete, GameStateSnapshot.class)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Missing stored colony");
    }
}
