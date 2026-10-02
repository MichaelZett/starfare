package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class RulesetPersistenceTest extends AbstractIntegrationTest {
    private static final String INVALID = "ruleset-invalid";
    private static final String VALID = "ruleset-valid";
    @Autowired private ObjectMapper mapper;
    @Autowired private GameSessionRepository sessions;
    @Autowired private GameArchiveRepository archiveRepository;
    @Autowired private GameArchiveStore archives;
    @Autowired private RulesetCatalog catalog;

    @AfterEach
    void cleanup() {
        sessions.deleteById(INVALID);
        sessions.deleteById(VALID);
        archiveRepository.deleteById(INVALID);
    }

    private GameState state(RulesetRef ref) {
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(ref));
        return state;
    }

    @Test
    void newReferenceSurvivesJsonCopyRestartAndArchive() {
        GameState state = state(RulesetRef.SECTOR_FORCES);
        state.start();
        state.endGame(null);
        var snapshot = GameState.toSnapshot(state);
        assertThat(snapshot.ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(GameState.copyOf(state).ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        String json = mapper.writeValueAsString(snapshot);
        assertThat(GameState.fromSnapshot(mapper.readValue(json, GameStateSnapshot.class)).ruleset())
                .isEqualTo(RulesetRef.SECTOR_FORCES);
        sessions.save(new GameSessionEntity(VALID, "Valid", "host", Instant.now(), json));
        var store = new JpaGameSessionStore(sessions, archives, mapper, catalog);
        assertThat(store.load(GameId.of(VALID)).orElseThrow().readState(GameState::ruleset))
                .isEqualTo(RulesetRef.SECTOR_FORCES);
        archiveRepository.save(new GameArchiveEntity(INVALID, "Archive", "host", Instant.now(), json));
        assertThat(archives.load(GameId.of(INVALID)).orElseThrow().state().ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
    }

    @Test
    void missingReferencesInGameAndOriginalSetupDefaultToSectorForces() {
        var tree = mapper.readTree(mapper.writeValueAsString(GameState.toSnapshot(state(RulesetRef.SECTOR_FORCES))));
        ((tools.jackson.databind.node.ObjectNode) tree).remove("ruleset");
        ((tools.jackson.databind.node.ObjectNode) tree.get("originalSetup")).remove("ruleset");
        var restored = GameState.fromSnapshot(mapper.treeToValue(tree, GameStateSnapshot.class));
        assertThat(restored.ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(restored.originalSetup().orElseThrow().ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
    }

    @Test
    void unknownStoredVersionIsSkippedWithoutOverwriteWhileClassicStillLoads() {
        String invalidJson = mapper.writeValueAsString(GameState.toSnapshot(state(new RulesetRef("classic", "9.0.0"))));
        sessions.save(new GameSessionEntity(INVALID, "Unsupported", "host", Instant.now(), invalidJson));
        sessions.save(new GameSessionEntity(VALID, "Valid", "host", Instant.now(),
                mapper.writeValueAsString(GameState.toSnapshot(state(RulesetRef.SECTOR_FORCES)))));
        var before = sessions.findById(INVALID).orElseThrow();
        long version = before.getVersion();
        var store = new JpaGameSessionStore(sessions, archives, mapper, catalog);
        store.loadFromDatabase();
        assertThat(store.load(GameId.of(INVALID))).isEmpty();
        assertThat(store.listIds()).contains(GameId.of(VALID)).doesNotContain(GameId.of(INVALID));
        var after = sessions.findById(INVALID).orElseThrow();
        assertThat(after.getStateJson()).isEqualTo(invalidJson);
        assertThat(after.getVersion()).isEqualTo(version);
        archiveRepository.save(new GameArchiveEntity(INVALID, "Unsupported archive", "host", Instant.now(), invalidJson));
        assertThat(archives.load(GameId.of(INVALID))).isEmpty();
        assertThat(archiveRepository.findById(INVALID).orElseThrow().getStateJson()).isEqualTo(invalidJson);
    }

    @Test
    void unsupportedSavesAreRejectedBeforeDatabaseAndCacheMutation() {
        GameState state = state(RulesetRef.SPACEWARD);
        var session = new GameSession(GameId.of(INVALID), "Unsupported", "host", Instant.now(), state);
        var store = new JpaGameSessionStore(sessions, archives, mapper, catalog);
        assertThatThrownBy(() -> store.save(session)).isInstanceOf(IllegalArgumentException.class);
        assertThat(sessions.existsById(INVALID)).isFalse();
        assertThat(store.loadedIds()).doesNotContain(GameId.of(INVALID));
    }

    @Test
    void contradictoryOrIncompleteReferencesAreNeverDefaulted() {
        var tree = (tools.jackson.databind.node.ObjectNode) mapper.readTree(
                mapper.writeValueAsString(GameState.toSnapshot(state(RulesetRef.SECTOR_FORCES))));
        ((tools.jackson.databind.node.ObjectNode) tree.get("ruleset")).put("version", "9.0.0");
        var snapshot = mapper.treeToValue(tree, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(snapshot)).isInstanceOf(IllegalArgumentException.class);
        ((tools.jackson.databind.node.ObjectNode) tree.get("ruleset")).remove("version");
        assertThatThrownBy(() -> mapper.treeToValue(tree, GameStateSnapshot.class)).isInstanceOf(RuntimeException.class);
    }
}
