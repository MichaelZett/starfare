package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistierte Snapshots müssen ältere Schemata weiterlesen können: ein Feld, das
 * neu hinzukommt, fehlt in allen bereits gespeicherten {@code state_json}-Spalten.
 */
class GameStateSnapshotJsonTest extends AbstractIntegrationTest {
    @Test
    void aiProfilesSurviveJsonAndMissingFieldsUseTheHistoricalBaseline() {
        var profile = de.zettsystems.starfare.ai.values.AiStrategy.INDUSTRY_ADAPTIVE;
        GameState state = new GameState();
        state.players().add(new Player(1, "AI", true, "#fff"));
        state.systems().add(new StarSystem(1, "Home", 0, 0, 1, 20, 5, false));
        state.players().replaceAll(p -> new Player(p.id(), p.name(), true, p.colorHex(), p.empireName(), profile));
        // Remembering the setup also determines the snapshot's rule version.
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE)
                .chooseAiStrategies(java.util.Collections.nCopies(GameSetup.defaults().aiPlayers(), profile)));
        state.start();
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        var restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));
        assertThat(restored.players()).extracting(Player::aiStrategy).containsExactly(profile);
        assertThat(restored.originalSetup().orElseThrow().aiStrategies()).containsOnly(profile);
        ((ObjectNode) json.get("players").get(0)).remove("aiStrategy");
        ((ObjectNode) json.get("originalSetup")).remove("aiStrategies");
        var legacy = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));
        assertThat(legacy.players()).extracting(Player::aiStrategy)
                .containsExactly(de.zettsystems.starfare.ai.values.AiStrategy.BASELINE);
        assertThat(legacy.originalSetup().orElseThrow().strategyForAi(0))
                .isEqualTo(de.zettsystems.starfare.ai.values.AiStrategy.BASELINE);
    }

    @Test
    void originalSetupSurvivesJsonAndOldSnapshotsRemainReadable() {
        GameState state = sampleState(true);
        state.rememberOrders(1);
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));
        assertThat(restored.originalSetup()).hasValue(GameSetup.defaults());
        assertThat(restored.undoOrders(1)).isTrue();
        json.remove(List.of("originalSetup", "previousOrders"));
        GameState old = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));
        assertThat(old.originalSetup()).isEmpty();
        assertThat(old.undoOrders(1)).isFalse();
    }

    @Test
    void sectorOptionsSurviveJsonAndOldSetupsReadAsOff() {
        var chosen = new SectorOptions(EventFrequency.RARE,
                java.util.EnumSet.of(CosmicEvent.SUPERNOVA, CosmicEvent.PIRATES), true, 45);
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().withSectorOptions(chosen));
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        assertThat(GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class)).sectorOptions())
                .isEqualTo(chosen);
        ((ObjectNode) json.get("originalSetup")).remove("sectorOptions");
        assertThat(GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class)).sectorOptions())
                .isEqualTo(SectorOptions.none());
    }

    @Test
    void theTutorialChoiceSurvivesJsonAndOldSetupsAreNoTutorial() {
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().chooseTutorial(true));
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        assertThat(GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class)).tutorial()).isTrue();
        ((ObjectNode) json.get("originalSetup")).remove("tutorial");
        assertThat(GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class)).tutorial()).isFalse();
        assertThat(new GameState().tutorial()).as("a game without its setup").isFalse();
    }

    @Test
    void cosmicStateSurvivesJsonAndOldSnapshotsHaveNone() {
        GameState state = sampleState(true);
        state.updateCosmic(cosmic -> cosmic.rememberHomes(List.of(1)).destabilize(2, 5).explode(3)
                .launchPirates(3, 1, 12, 9));
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));
        assertThat(restored.cosmic()).isEqualTo(state.cosmic());
        assertThat(restored.destroyed(3)).isTrue();
        assertThat(GameState.copyOf(restored).cosmic()).isEqualTo(state.cosmic());
        json.remove("cosmic");
        assertThat(GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class)).cosmic())
                .isEqualTo(CosmicState.EMPTY);
    }

    @Autowired
    private ObjectMapper objectMapper;

    private static GameState sampleState() {
        return sampleState(false);
    }

    private static GameState sampleState(boolean rememberSetup) {
        GameState state = new GameState();
        if (rememberSetup) { state.rememberSetup(GameSetup.defaults()); }
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
        state.intel().put(1, new HashMap<>());
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
    void roundRulesAndClocksSurviveJsonRoundTrip() {
        GameState state = sampleState();
        state.configureRoundRules(new RoundRules(Duration.ofDays(2), Duration.ofHours(6), AttackOrder.STRONGEST_FIRST));
        state.joinedHumanPlayerIds().addAll(List.of(1, 2));
        state.submittedThisTurn().add(1);
        state.updateStragglerClock(Instant.parse("2026-09-18T10:00:00Z"));
        state.missedRounds().put(2, 1);

        String json = objectMapper.writeValueAsString(GameState.toSnapshot(state));
        GameState restored = GameState.fromSnapshot(objectMapper.readValue(json, GameStateSnapshot.class));

        assertThat(restored.roundRules()).isEqualTo(state.roundRules());
        assertThat(restored.stragglerSince()).isEqualTo(Instant.parse("2026-09-18T10:00:00Z"));
        assertThat(restored.missedRounds()).containsEntry(2, 1);
    }

    @Test
    void aTempoSurvivesJsonRoundTripAndOlderRulesStayWithoutOne() {
        GameState state = sampleState();
        state.configureRoundRules(RoundRules.forTempo(GameTempo.RELAXED, AttackOrder.RANDOM));

        String json = objectMapper.writeValueAsString(GameState.toSnapshot(state));
        GameState restored = GameState.fromSnapshot(objectMapper.readValue(json, GameStateSnapshot.class));
        assertThat(restored.roundRules().tempo()).isEqualTo(GameTempo.RELAXED);
        assertThat(restored.roundRules().roundLimit()).isEqualTo(Duration.ofHours(72));

        ObjectNode older = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) older.get("roundRules")).remove("tempo");
        ((ObjectNode) older.get("roundRules")).put("roundLimit", Duration.ofMinutes(10).toString());
        GameState old = GameState.fromSnapshot(objectMapper.treeToValue(older, GameStateSnapshot.class));
        assertThat(old.roundRules().tempo()).isNull();
        assertThat(old.roundRules().roundLimit()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void snapshotJsonWithoutRoundRulesUsesDefaults() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        json.remove(List.of("roundRules", "stragglerSince", "missedRounds"));

        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));

        assertThat(restored.roundRules()).isEqualTo(RoundRules.defaults());
        assertThat(restored.stragglerSince()).isNull();
        assertThat(restored.missedRounds()).isEmpty();
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
    void combatRandomnessSurvivesJsonRoundTripAndLegacySnapshotsUseDefault() {
        GameState state = sampleState();
        state.configureLobby(true, true, true, 30);
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));

        assertThat(GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class))
                .combatRandomnessPercent()).isEqualTo(30);

        json.remove("combatRandomnessPercent");
        assertThat(GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class))
                .combatRandomnessPercent()).isEqualTo(GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT);
    }

    @Test
    void systemJsonWithTheRemovedGarrisonReserveStillLoads() {
        GameState state = sampleState();
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(state));
        assertThat(json.get("systems").get(0).has("garrisonReserve")).isFalse();
        ((ObjectNode) json.get("systems").get(0)).put("garrisonReserve", 3);

        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));

        assertThat(restored.systems()).isEqualTo(state.systems());
    }

    @Test
    void playerJsonWithoutEmpireNameStillLoads() {
        ObjectNode json = (ObjectNode) objectMapper.valueToTree(GameState.toSnapshot(sampleState()));
        ((ObjectNode) json.get("players").get(0)).remove("empireName");

        GameState restored = GameState.fromSnapshot(objectMapper.treeToValue(json, GameStateSnapshot.class));

        assertThat(restored.players().getFirst().empireNameOrName()).isEqualTo("P1");
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
        state.standingOrders().put(1, new ArrayList<>(
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
        state.endGame(1, Instant.parse("2026-09-09T12:00:00Z"));
        GameState restored = GameState.fromSnapshot(objectMapper.readValue(
                objectMapper.writeValueAsString(GameState.toSnapshot(state)), GameStateSnapshot.class));
        assertThat(restored.visibility()).isEqualTo(state.visibility());
        assertThat(restored.finishedAt()).isEqualTo(state.finishedAt());
    }
}
