package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameVisibility;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

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
    void snapshotJsonWithoutReplayFramesStillLoads() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        json.remove("replayFrames");

        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));

        assertThat(restored.replayFrames()).isEmpty();
    }

    @Test
    void snapshotJsonWithoutBattlePresentationChoiceUsesDefault() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        json.remove("battlePresentationEnabled");

        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));

        assertThat(restored.battlePresentationEnabled())
                .isEqualTo(GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED);
    }

    @Test
    void systemJsonWithoutGarrisonReserveStillLoads() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        ((ObjectNode) json.get("systems").get(0)).remove("garrisonReserve");

        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));

        assertThat(restored.systems().getFirst().garrisonReserve()).isZero();
    }

    @Test
    void intelJsonWithoutGarrisonStillLoads() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        ((ObjectNode) json.get("intel").get("1").get("1")).remove("garrison");

        GameStateSnapshot legacy = objectMapper.treeToValue(json, GameStateSnapshot.class);

        assertThat(legacy.intel().get(1).get(1).garrison()).isNull();
        assertThat(legacy.intel().get(1).get(1).turn()).isEqualTo(2);
    }

    @Test
    void standingOrderJsonWithoutShipsStillLoads() {
        GameState state = sampleState();
        state.standingOrders().put(1, new java.util.ArrayList<>(
                List.of(new StandingOrder(1, 1, 1, 2, 3))));

        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) json.get("standingOrders").get("1").get(0)).remove("ships");

        GameStateSnapshot legacy = objectMapper.treeToValue(json, GameStateSnapshot.class);

        StandingOrder restored = legacy.standingOrders().get(1).getFirst();
        assertThat(restored.ships()).as("fehlende Menge darf die Partie nicht kosten").isZero();
        assertThat(restored.toSystemId()).isEqualTo(2);
    }

    @Test
    void legacyArchiveRemainsPublicWithoutInventedCompletionDate() {
        GameState state = sampleState();
        state.endGame(1);
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        json.remove("visibility");
        json.remove("finishedAt");
        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));
        assertThat(restored.visibility()).isEqualTo(GameVisibility.PUBLIC);
        assertThat(restored.gameOver()).isTrue();
        assertThat(restored.finishedAt()).isNull();
    }

    @Test
    void privateArchiveRoundTripPreservesVisibilityAndCompletionDate() {
        GameState state = sampleState();
        state.endGame(1, java.time.Instant.parse("2026-09-09T12:00:00Z"));
        GameState restored = GameState.fromSnapshot(objectMapper.readValue(
                objectMapper.writeValueAsString(GameState.toSnapshot(state)), GameStateSnapshot.class));
        assertThat(restored.visibility()).isEqualTo(state.visibility());
        assertThat(restored.finishedAt()).isEqualTo(state.finishedAt());
    }
}
