package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistierte Snapshots müssen ältere Schemata weiterlesen können: ein Feld, das
 * neu hinzukommt, fehlt in allen bereits gespeicherten {@code state_json}-Spalten.
 */
class GameStateSnapshotJsonTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static GameState sampleState() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
        state.intel().put(1, new java.util.HashMap<>());
        state.intel().get(1).put(1, new GameState.Intel(1, 2, 7));
        state.start();
        state.nextTurn();
        return state;
    }

    @Test
    void snapshotSurvivesJsonRoundTrip() {
        GameState state = sampleState();

        String json = objectMapper.writeValueAsString(GameState.toSnapshot(state));
        GameState restored = GameState.fromSnapshot(objectMapper.readValue(json, GameStateSnapshot.class));

        assertThat(restored.turn()).isEqualTo(state.turn());
        assertThat(restored.turnStartedAt()).isEqualTo(state.turnStartedAt());
        assertThat(restored.intel().get(1).get(1).garrison()).isEqualTo(7);
    }

    @Test
    void snapshotJsonWithoutTurnStartedAtStillLoads() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        json.remove("turnStartedAt");

        GameStateSnapshot legacy = objectMapper.treeToValue(json, GameStateSnapshot.class);

        assertThat(legacy.turnStartedAt()).isNull();
        assertThat(GameState.fromSnapshot(legacy).turnStartedAt()).isNotNull();
    }

    @Test
    void intelJsonWithoutGarrisonStillLoads() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        ((ObjectNode) json.get("intel").get("1").get("1")).remove("garrison");

        GameStateSnapshot legacy = objectMapper.treeToValue(json, GameStateSnapshot.class);

        assertThat(legacy.intel().get(1).get(1).garrison()).isNull();
        assertThat(legacy.intel().get(1).get(1).turn()).isEqualTo(2);
    }
}
