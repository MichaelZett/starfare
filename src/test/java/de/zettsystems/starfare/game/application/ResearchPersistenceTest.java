package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.testsupport.ColonyStates;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class ResearchPersistenceTest extends AbstractIntegrationTest {
    @Autowired ObjectMapper mapper;
    @Autowired GameService games;
    @Autowired GameSessionStore store;
    @Autowired GameSessionRepository repository;
    @Autowired GameArchiveStore archives;
    @Autowired de.zettsystems.starfare.turn.application.TurnEngine turns;

    @Test void previewCreationEstablishesResearchBeforeTheFirstSave() {
        var preview = new DefaultGameRegistry(store, new DefaultGameAccessPolicy(),
                de.zettsystems.starfare.game.values.RulesetCatalog.withShipResearch());
        var id = preview.createGame(de.zettsystems.starfare.game.values.GameSetup.defaults()
                .selectRuleset(RulesetRef.SPACEWARD_SHIP_RESEARCH), "owner", "Research preview");
        try {
            var snapshot = preview.readState(id, GameState::toSnapshot);
            assertThat(snapshot.metals().research().empires()).hasSameSizeAs(snapshot.players());
            assertThat(snapshot.metals().research().empires().values()).allMatch(research -> research.percent() == 0 && research.level() == 0);
            assertThat(GameState.fromSnapshot(snapshot).ruleset()).isEqualTo(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        } finally { store.delete(id); }
    }

    @Test void ownBudgetRejectsStrangersStaleTurnsAndExternalEditsBeforeAnyMutation() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        state.start(); state.seatByUser().put("owner", 1);
        var id = save(state);
        try {
            var before = registry.readState(id, GameState::toSnapshot);
            assertThat(games.shipResearchFor(id, "stranger")).isEmpty();
            assertThat(games.planShipResearch(id, "stranger", 1, 0, 50)).isFalse();
            assertThat(games.planShipResearch(id, "owner", 0, 0, 50)).isFalse();
            assertThat(games.planShipResearch(id, "owner", 1, 0, 101)).isFalse();
            assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
            assertThat(games.planShipResearch(id, "owner", 1, 0, 25)).isTrue();
            var view = games.shipResearchFor(id, "owner").orElseThrow();
            assertThat(view.research().percent()).isEqualTo(25);
            assertThat(view.pointsPerTurn()).isEqualTo(3);
            assertThat(games.planShipResearch(id, "owner", 1, 0, 50)).isFalse();
            registry.writeState(id, s -> { s.submittedThisTurn().add(1); return null; });
            assertThat(games.planShipResearch(id, "owner", 1, 25, 50)).isFalse();
            assertThat(games.shipResearchFor(id, "owner").orElseThrow().editable()).isFalse();
        } finally { store.delete(id); }
    }

    @Test void completedResearchReportsAndMixedShipsSurviveDatabaseReloadAndNextRound() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        state.start(); state.planShipResearch(1, 100);
        var economy = new DefaultEconomyService();
        for (int i = 0; i < 7; i++) { economy.produce(state, java.util.Map.of()); state.nextTurn(); }
        state.planShipResearch(1, 50);
        turns.advanceTurn(state);
        assertThat(state.reports().get(1).events()).contains(new TurnEvent.ResearchCompleted(1, 1));
        state.planShipResearch(1, 0); turns.advanceTurn(state);
        assertThat(state.getSystem(1).composition().levels()).containsKeys(0, 1);
        var id = save(state);
        try {
            var restarted = new JpaGameSessionStore(repository, archives, mapper);
            var restored = restarted.load(id).orElseThrow().readState(GameState::copyOf);
            assertThat(GameState.toSnapshot(restored)).isEqualTo(GameState.toSnapshot(state));
            turns.advanceTurn(restored);
            assertThat(restored.shipTechnology(1)).isEqualTo(1);
            assertThatCode(() -> GameState.toSnapshot(restored)).doesNotThrowAnyException();
        } finally { store.delete(id); }
    }

    @Test void oldJsonWithoutGenerationOrResearchFieldsLoadsAsLevelZeroButR1RequiresResearch() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_EARLY_INDUSTRY); state.start();
        assertThat(new DefaultFleetService().sendFleet(state, 1, 1, 2, 2)).isTrue();
        ObjectNode legacy = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        removeNewFields(legacy);
        var restored = GameState.fromSnapshot(mapper.treeToValue(legacy, GameStateSnapshot.class));
        assertThat(restored.getSystem(1).composition().levels()).containsOnlyKeys(0);
        assertThat(restored.fleets().getFirst().composition().levels()).containsOnlyKeys(0);
        assertThat(restored.metals().orElseThrow().research()).isNull();
        var research = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH); research.start();
        ObjectNode broken = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(research));
        ((ObjectNode) broken.path("metals")).remove("research");
        var snapshot = mapper.treeToValue(broken, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(snapshot)).isInstanceOf(IllegalArgumentException.class);
    }
    private GameId save(GameState state) {
        var id = GameId.newId();
        store.save(new GameSession(id, "Research", "owner", Instant.now(), state));
        return id;
    }
    private static void removeNewFields(JsonNode node) {
        if (node instanceof ObjectNode object) { object.remove("composition"); object.remove("research"); }
        for (JsonNode child : node) { removeNewFields(child); }
    }
}
