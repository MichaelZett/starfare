package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameOutcomeStatistics;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.ReplayEventMarker;
import de.zettsystems.starfare.game.values.ReplayFrame;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GameArchiveTest extends AbstractIntegrationTest {
    @Autowired private GameService games;
    @Autowired private GameSessionRepository repository;
    @Autowired private GameSessionStore sessions;

    @Test
    void reviewSelectionRequiresFinishedAccessibleGameAndExistingParticipant() {
        var id = games.newGame(GameSetup.defaults(), "host", "Review");
        games.joinGame(id, "host");
        assertThat(games.reviewFor(id, "host", 1, false)).isEmpty();
        games.startGame(id);
        assertThat(games.reviewFor(id, "host", 2, true)).isEmpty();
        registry.writeState(id, state -> { state.endGame(1); return null; });
        var snapshot = registry.readState(id, GameState::toSnapshot);
        long version = repository.findById(id.value()).orElseThrow().getVersion();
        assertThat(games.reviewFor(id, "stranger", 1, false)).isEmpty();
        assertThat(games.reviewFor(id, "", 1, true)).isEmpty();
        assertThat(games.reviewFor(id, "host", -1, false)).isEmpty();
        assertThat(games.reviewFor(id, "host", 999, true)).isEmpty();
        assertThat(games.reviewFor(id, "host", 1, true)).isPresent();
        assertThat(games.reviewFor(id, "host", 2, true)).isPresent();
        assertThat(games.reviewFor(id, "host", 2, false)).isPresent();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(snapshot);
        assertThat(repository.findById(id.value()).orElseThrow().getVersion()).isEqualTo(version);
    }

    @Test
    void finishedGameMovesToArchiveAndReviewDoesNotWrite() {
        var id = games.newGame(GameSetup.defaults(), "host", "Finished");
        assertThat(games.joinGame(id, "host")).isPresent();
        assertThat(games.startGame(id)).isTrue();
        registry.writeState(id, state -> { state.endGame(1); return null; });
        var before = registry.readState(id, GameState::toSnapshot);
        long version = repository.findById(id.value()).orElseThrow().getVersion();
        assertThat(games.visibleGamesFor("host", GameListScope.LOBBY)).isEmpty();
        assertThat(games.visibleGamesFor("host", GameListScope.ARCHIVE))
                .extracting(GameSummary::gameId)
                .contains(id);
        assertThat(games.reviewFor(id, "host")).isPresent();
        assertThat(games.reviewFor(id, "stranger")).isEmpty();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        assertThat(repository.findById(id.value()).orElseThrow().getVersion()).isEqualTo(version);
    }

    @Test
    void listsOnlyTheFinishedGamesTheAccountHeldASeatInLatestFirst() {
        var first = finishedGameOf("17", "First");
        var second = finishedGameOf("17", "Second");
        finishedGameOf("23", "Foreign");
        var running = games.newGame(GameSetup.defaults(), "17", "Running");
        games.joinGame(running, "17");
        games.startGame(running);
        sessions.delete(first);

        assertThat(games.finishedMatchesOf("17", 0, 10))
                .as("from the archive even without the operative session; neither foreign nor running games")
                .extracting(FinishedMatch::matchId).containsExactly(second.value(), first.value());
        assertThat(games.finishedMatchesOf("17", 1, 5)).extracting(FinishedMatch::matchId).containsExactly(first.value());
        assertThat(games.finishedMatchesOf("17", 0, 1)).extracting(FinishedMatch::matchId).containsExactly(second.value());
        assertThat(games.finishedMatchesOf("17", 2, 1)).isEmpty();
        var ended = java.util.Objects.requireNonNull(registry.readState(second, GameState::finishedAt));
        assertThat(games.finishedMatchesOf("17", 0, 1).getFirst().endedAt()).as("the end stored when archived")
                .isCloseTo(ended, org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MICROS));
    }

    private GameId finishedGameOf(String account, String name) {
        var id = games.newGame(GameSetup.defaults(), account, name);
        games.joinGame(id, account);
        games.startGame(id);
        registry.writeState(id, state -> { state.endGame(1); return null; });
        return id;
    }

    @Test
    void archivedGameRemainsOpenableAfterTheOperationalSessionWasRemoved() {
        var id = games.newGame(GameSetup.defaults(), "host", "Archived");
        games.joinGame(id, "host");
        games.startGame(id);
        registry.writeState(id, state -> { state.endGame(1); return null; });
        sessions.delete(id);

        assertThat(games.viewForAccount(id, "host")).isPresent();
        assertThat(games.hasStartedGame(id)).isTrue();
        assertThat(games.gameNameOf(id)).isEqualTo("Archived");
        assertThat(games.seatFor(id, "host")).isPresent();
    }

    @Test
    void commandsOnArchiveOnlyGameAreRejectedInsteadOfThrowing() {
        var id = games.newGame(GameSetup.defaults(), "host", "Archived");
        games.joinGame(id, "host");
        games.startGame(id);
        registry.writeState(id, state -> { state.endGame(1); return null; });
        sessions.delete(id);

        assertThat(games.sendFleet(id, 1, 1, 2, 1)).isFalse();
        assertThat(games.addStandingOrder(id, 1, 1, 2, 1)).isFalse();
        assertThat(games.setFleetWait(id, 1, 1)).isFalse();
        assertThat(games.disbandFleet(id, 1, 1)).isFalse();
    }

    @Test
    void outcomeStatisticsSumOnlyOwnEventsOfFinishedGame() {
        var id = games.newGame(GameSetup.defaults(), "host", "Outcome");
        games.joinGame(id, "host");
        games.startGame(id);
        assertThat(games.outcomeStatisticsFor(id, "host")).isEmpty();
        int seat = games.seatFor(id, "host").orElseThrow();
        int rival = seat + 1;
        TurnReport report = new TurnReport(90, List.of(), List.of(
                new TurnEvent.Production(seat, 1, "A", 5),
                new TurnEvent.Production(rival, 2, "B", 9),
                new TurnEvent.BattleWon(seat, 3, "C", 10, 4, 6, false),
                new TurnEvent.BattleWon(rival, 3, "C", 10, 4, 6, false),
                new TurnEvent.BattleLost(seat, 4, "D", 7, 9, 2),
                new TurnEvent.SystemLost(seat, rival, 5, "E", 8, 3, 5),
                new TurnEvent.DefenseHeld(seat, 6, "F", 2, 6, 4),
                new TurnEvent.Reinforcement(seat, 1, "A", 3, 8, "F1"),
                new TurnEvent.Victory(seat)));
        int ownSystems = registry.writeState(id, state -> {
            state.replayFrames().put(90, new ReplayFrame(90, state.systems(), state.fleets(), Map.of(seat, report)));
            state.endGame(seat);
            return (int) state.systems().stream().filter(system -> Objects.equals(system.ownerId(), seat)).count();
        });

        assertThat(games.outcomeStatisticsFor(id, "stranger")).isEmpty();
        assertThat(games.outcomeStatisticsFor(id, "host"))
                .contains(new GameOutcomeStatistics(0, ownSystems, 5, 16, 16));
    }

    @Test
    void replayEventMarkersExposeOnlyCombatEventsInTurnOrderForAnAuthorizedPerspective() {
        var id = games.newGame(GameSetup.defaults(), "host", "Replay markers");
        games.joinGame(id, "host");
        games.startGame(id);
        int seat = games.seatFor(id, "host").orElseThrow();
        TurnReport firstReport = new TurnReport(4, List.of(), List.of(
                new TurnEvent.Production(seat, 1, "Ignored", 3),
                new TurnEvent.BattleWon(seat, 11, "Alpha", 8, 5, 3, false),
                new TurnEvent.BattleLost(seat, 12, "Beta", 7, 9, 2)));
        TurnReport secondReport = new TurnReport(9, List.of(), List.of(
                new TurnEvent.SystemLost(seat, 2, 13, "Gamma", 5, 6, 1),
                new TurnEvent.DefenseHeld(seat, 14, "Delta", 4, 7, 5),
                new TurnEvent.CoalitionBattle(15, "Epsilon", 2, seat, "Host", List.of()),
                new TurnEvent.CoalitionBattle(16, "Zeta", seat, seat, "Host", List.of()),
                new TurnEvent.Victory(seat)));
        registry.writeState(id, state -> {
            state.replayFrames().put(9, new ReplayFrame(9, state.systems(), state.fleets(), Map.of(seat, secondReport)));
            state.replayFrames().put(4, new ReplayFrame(4, state.systems(), state.fleets(), Map.of(seat, firstReport)));
            state.endGame(seat);
            return null;
        });

        List<ReplayEventMarker> expected = List.of(
                new ReplayEventMarker(4, 11, "Alpha", 1, true),
                new ReplayEventMarker(4, 12, "Beta", 2, false),
                new ReplayEventMarker(9, 13, "Gamma", 0, true),
                new ReplayEventMarker(9, 14, "Delta", 1, false),
                new ReplayEventMarker(9, 15, "Epsilon", 2, true),
                new ReplayEventMarker(9, 16, "Zeta", 3, false));
        assertThat(games.replayEventMarkers(id, "host", seat)).containsExactlyElementsOf(expected);
        assertThat(games.replayEventMarkers(id, "stranger", seat)).isEmpty();
        assertThat(games.replayEventMarkers(id, "host", 999)).isEmpty();

        sessions.delete(id);

        assertThat(games.replayEventMarkers(id, "host", seat)).containsExactlyElementsOf(expected);
    }

    @Test
    void replayIsOnlyForAuthorizedAccountsKnownPerspectivesAndRecordedTurns() {
        var id = games.newGame(GameSetup.defaults(), "host", "Replay access");
        games.joinGame(id, "host");
        games.startGame(id);
        int seat = games.seatFor(id, "host").orElseThrow();
        registry.writeState(id, state -> {
            state.replayFrames().put(4, new ReplayFrame(4, state.systems(), state.fleets(), Map.of()));
            state.endGame(seat);
            return null;
        });

        assertReplayAccess(id, seat);
        sessions.delete(id);
        assertReplayAccess(id, seat);
    }

    private void assertReplayAccess(GameId id, int seat) {
        assertThat(games.replayTurns(id, "host")).contains(4);
        assertThat(games.replayTurns(id, "stranger")).isEmpty();
        assertThat(games.replayFor(id, "host", seat, 4)).isPresent();
        assertThat(games.replayFor(id, "stranger", seat, 4)).isEmpty();
        assertThat(games.replayFor(id, "host", 999, 4)).isEmpty();
        assertThat(games.replayFor(id, "host", seat, 999)).as("no recorded frame").isEmpty();
    }

    @Test
    void staleCommandsCannotChangeFinishedGame() {
        var id = games.newGame(GameSetup.defaults(), "host", "Finished");
        games.joinGame(id, "host");
        games.startGame(id);
        registry.writeState(id, state -> { state.endGame(1); return null; });
        var before = registry.readState(id, GameState::toSnapshot);
        assertThat(games.sendFleet(id, 1, 1, 2, 1)).isFalse();
        assertThat(games.addStandingOrder(id, 1, 1, 2, 1)).isFalse();
        assertThat(games.removeStandingOrder(id, 1, 1)).isFalse();
        assertThat(games.removeStandingOrderFrom(id, 1, 1)).isFalse();
        assertThat(games.cancelOrder(id, 1, 0)).isFalse();
        assertThat(games.setFleetWait(id, 1, 1)).isFalse();
        assertThat(games.disbandFleet(id, 1, 1)).isFalse();
        assertThat(games.submitTurn(id, 1)).isFalse();
        assertThat(games.abortGame(id, "host")).isFalse();
        assertThat(games.leaveObserve(id, "host")).isFalse();
        assertThat(games.revokeInvite(id, "guest")).isEmpty();
        assertThat(games.startGame(id)).isFalse();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
    }
}
