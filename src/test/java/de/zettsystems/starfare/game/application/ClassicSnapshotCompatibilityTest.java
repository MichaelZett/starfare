package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameSessionEntity;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.turn.application.DefaultTurnEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Frozen, synthetic old-format JSON must survive the real application mapper and store. */
class ClassicSnapshotCompatibilityTest extends AbstractIntegrationTest {
    private static final String RUNNING = "legacy-running.json";
    private static final String ARCHIVE = "legacy-archive.json";
    @Autowired private ObjectMapper mapper;
    @Autowired private GameSessionRepository repository;
    @Autowired private GameArchiveStore archives;
    @Autowired private GameArchiveRepository archiveRepository;
    @Autowired private GameService games;
    @Autowired private GameTemplateService templates;
    private final PlayerViewBuilder views = new DefaultPlayerViewBuilder();

    @AfterEach
    void deleteFixtures() {
        repository.deleteById("classic-" + RUNNING);
        repository.deleteById("classic-" + ARCHIVE);
        archiveRepository.deleteById("classic-retained-archive");
    }

    private static String json(String name) throws IOException {
        return new ClassPathResource("classic/" + name).getContentAsString(StandardCharsets.UTF_8);
    }

    private GameState restore(String name) throws IOException {
        return GameState.fromSnapshot(mapper.readValue(json(name), GameStateSnapshot.class));
    }

    @Test
    void runningFixturePreservesCommandsAndExplicitLegacyDefaults() throws IOException {
        GameState state = restore(RUNNING);
        assertThat(state.turn()).isEqualTo(4);
        assertThat(state.originalSetup()).isEmpty();
        assertThat(state.replayFrames()).isEmpty();
        assertThat(state.visibility()).isEqualTo(GameVisibility.PUBLIC);
        assertThat(state.finishedAt()).isNull();
        assertThat(state.roundRules()).isEqualTo(new RoundRules(null, null, AttackOrder.RANDOM));
        assertThat(state.victorySystemPercent()).isEqualTo(70);
        assertThat(state.combatRandomnessPercent()).isEqualTo(10);
        assertThat(state.battlePresentationEnabled()).isTrue();
        assertThat(state.players().getFirst().empireNameOrName()).isEqualTo("Fixture Alpha");
        assertThat(state.systems()).extracting(StarSystem::garrisonReserve).containsOnly(0);
        assertThat(state.intel().get(1).get(3)).isEqualTo(new GameState.Intel(2, 2, null));
        assertThat(state.standingOrders().get(1)).containsExactly(new StandingOrder(1, 1, 1, 2, 0));
        assertThat(state.pendingOrders().get(1)).containsExactly(
                new FleetOrder.Send(1, 1, 2, 2), new FleetOrder.Wait(1, 7));
        assertThat(state.reports().get(1).events()).containsExactly(
                new TurnEvent.Reinforcement(1, 2, "Relay", 3, 0, "F1"));
        assertThat(state.undoOrders(1)).isFalse();
        assertThat(state.invitedSeats()).isEmpty();
        assertThat(state.missedRounds()).isEmpty();
        assertThat(state.stragglerSince()).isNull();
        assertThat(state.turnStartedAt()).isNotNull();
    }

    @Test
    void runningFixtureCanContinueWithTheSameFleetNumbersAndArrivalRules() throws IOException {
        GameState state = restore(RUNNING);
        DefaultReportService report = new DefaultReportService();
        var engine = new DefaultTurnEngine(new DefaultCombatService(report), new DefaultAiService(),
                report, new DefaultFleetService());

        engine.advanceTurn(state);
        assertThat(state.turn()).isEqualTo(5);
        assertThat(state.systems()).extracting(StarSystem::garrison).containsExactly(10, 5, 12, 7);
        assertThat(state.fleets()).containsExactly(
                new Fleet(7, 1, 2, 1, 2, 3, 3, 6), new Fleet(8, 1, 3, 1, 2, 2, 4, 6));
        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.waitThisTurn()).isEmpty();

        engine.advanceTurn(state);
        assertThat(state.turn()).isEqualTo(6);
        assertThat(state.systems()).extracting(StarSystem::garrison).containsExactly(12, 11, 15, 7);
        assertThat(state.fleets()).isEmpty();
        assertThat(state.gameOver()).isFalse();
        assertThat(state.reports().get(1).events()).containsExactly(
                new TurnEvent.Production(1, 1, "Home", 2), new TurnEvent.Production(1, 2, "Relay", 1),
                new TurnEvent.Reinforcement(1, 2, "Relay", 5, 11, "F2,3"));
    }

    @Test
    void archiveFramesKeepHistoricalOwnershipStrengthsAndVictoryThreshold() throws IOException {
        GameState state = restore(ARCHIVE);
        var before = GameState.toSnapshot(state);
        assertThat(state.gameOver()).isTrue();
        assertThat(state.winnerId()).isOne();
        assertThat(state.finishedAt()).isNull();
        assertThat(state.originalSetup()).isEmpty();
        assertThat(state.replayFrames()).containsOnlyKeys(3, 4);
        assertThat(state.reports().get(1).events()).containsExactly(
                new TurnEvent.BattleWon(1, 3, "Frontier", 10, 4, 6, true, 11, 4, "", ""),
                new TurnEvent.Victory(1, 70));

        var early = views.forReplay(state, state.replayFrames().get(3), 1);
        assertThat(early.turn()).isEqualTo(3);
        assertThat(early.systems().get(2).ownerId()).isNull();
        assertThat(early.systems().get(2).garrison()).isEqualTo(4);
        assertThat(early.ownFleets()).containsExactly(new Fleet(7, 1, 2, 1, 3, 10, 2, 5));
        var finalFrame = views.forReplay(state, state.replayFrames().get(4), 1);
        assertThat(finalFrame.systems().get(2).ownerId()).isOne();
        assertThat(finalFrame.report().events()).containsExactly(
                new TurnEvent.BattleWon(1, 3, "Frontier", 10, 4, 6, true, 10, 4, "", ""),
                new TurnEvent.Victory(1, 70));
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {RUNNING, ARCHIVE})
    void oldFixturesSurviveWritingAndReadingTheCurrentFormat(String fixture) throws IOException {
        GameState state = restore(fixture);
        GameStateSnapshot before = GameState.toSnapshot(state);
        var written = mapper.writeValueAsString(before);
        var restored = GameState.fromSnapshot(mapper.readValue(written, GameStateSnapshot.class));
        assertThat(GameState.toSnapshot(restored)).isEqualTo(before);
    }

    @Test
    void retainedArchiveSupportsReviewWithoutAnOperationalSessionOrInventedTemplate() throws IOException {
        GameId id = GameId.of("classic-retained-archive");
        String originalJson = json(ARCHIVE);
        Instant retainedAt = Instant.parse("2026-09-01T10:00:00Z");
        var entity = archiveRepository.saveAndFlush(new GameArchiveEntity(id.value(), "Classic archive",
                "fixture-alpha", retainedAt, originalJson));
        long version = entity.getVersion();

        assertThat(registry.find(id)).isEmpty();
        var archived = archives.load(id).orElseThrow();
        assertThat(archived.finishedAt()).isEqualTo(retainedAt);
        assertThat(archived.state().finishedAt()).isNull();
        var replay = games.replayFor(id, "fixture-alpha", 1, 3).orElseThrow();
        assertThat(replay.systems().get(2).ownerId()).isNull();
        assertThat(replay.ownFleets()).hasSize(1);
        var review = games.reviewFor(id, "fixture-alpha", 1, false).orElseThrow();
        assertThat(review.systems().get(2).ownerId()).isOne();
        assertThat(templates.template(id, "fixture-alpha")).isEmpty();
        assertThat(templates.create(id, "fixture-alpha", "Do not invent settings", true)).isEmpty();
        assertThat(games.continueAfterVictory(id, "fixture-alpha")).isFalse();
        assertThat(repository.findById(id.value())).isEmpty();
        var untouched = archiveRepository.findById(id.value()).orElseThrow();
        assertThat(untouched.getStateJson()).isEqualTo(originalJson);
        assertThat(untouched.getVersion()).isEqualTo(version);
    }

    @ParameterizedTest
    @ValueSource(strings = {RUNNING, ARCHIVE})
    void databaseReloadAndReadOnlyViewsNeverRewriteLegacyRows(String fixture) throws IOException {
        GameId id = GameId.of("classic-" + fixture);
        String originalJson = json(fixture);
        var entity = repository.saveAndFlush(new GameSessionEntity(id.value(), "Classic fixture", "fixture-alpha",
                Instant.parse("2026-09-01T10:00:00Z"), originalJson));
        long version = entity.getVersion();
        var restarted = new JpaGameSessionStore(repository, archives, mapper);
        restarted.loadFromDatabase();
        var session = restarted.load(id).orElseThrow();
        assertThat(restarted.listIds()).contains(id);
        assertThat(session.hostPlayerId()).isEqualTo("fixture-alpha");
        session.readState(state -> {
            var before = GameState.toSnapshot(state);
            for (int player : List.of(1, 2)) {
                views.forPlayer(state, player);
                views.forReview(state, player, true);
                views.forReview(state, player, false);
                state.replayFrames().values().forEach(frame -> views.forReplay(state, frame, player));
            }
            views.forObserver(state).systems().forEach(system ->
                    assertThat(SystemComposition.forVisible(id, system)).isPresent());
            assertThat(GameState.toSnapshot(state)).isEqualTo(before);
            assertThat(state.originalSetup()).isEmpty();
            return null;
        });
        var untouched = repository.findById(id.value()).orElseThrow();
        assertThat(untouched.getStateJson()).isEqualTo(originalJson);
        assertThat(untouched.getVersion()).isEqualTo(version);
    }
}
