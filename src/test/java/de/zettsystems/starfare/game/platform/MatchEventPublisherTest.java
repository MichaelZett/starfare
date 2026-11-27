package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.DecisionKind;
import de.zettsystems.gamesplatformapi.DecisionsOpened;
import de.zettsystems.gamesplatformapi.MatchStarted;
import de.zettsystems.gamesplatformapi.PendingDecision;
import de.zettsystems.gamesplatformapi.TurnMissed;
import de.zettsystems.gamesplatformapi.MatchChanged;
import de.zettsystems.gamesplatformapi.MatchEnded;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.starfare.game.application.DefaultBroadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchEventPublisherTest {

    private static final GameId ID = GameId.of("game-1");
    private static final AccountId SEVENTEEN = AccountId.of(17);
    private static final AccountId TWENTY_THREE = AccountId.of(23);

    private final DefaultBroadcaster broadcaster = new DefaultBroadcaster();
    private final GameRegistry registry = mock(GameRegistry.class);
    private final GameSession session = mock(GameSession.class);
    private final List<Object> published = new ArrayList<>();

    @BeforeEach
    void subscribe() {
        new MatchEventPublisher(broadcaster, registry, published::add).afterPropertiesSet();
        when(registry.find(ID)).thenReturn(Optional.of(session));
        when(session.summary()).thenReturn(game(2, false));
    }

    @Test
    void announcesChangesTheLobbyShows() {
        List<GameEvent> events = List.of(new GameEvent.GameCreated(ID), new GameEvent.PlayerJoined(ID, 1),
                new GameEvent.PlayerSubmitted(ID, 1), new GameEvent.SeatAbandoned(ID, 2), new GameEvent.GameStarted(ID),
                new GameEvent.GameContinued(ID), new GameEvent.VisibilityChanged(ID),
                new GameEvent.InvitationChanged(ID), new GameEvent.DiplomacyChanged(ID));

        when(session.summary()).thenReturn(game(0, false));
        events.forEach(broadcaster::publish);

        assertThat(published).hasSize(events.size()).allSatisfy(event ->
                assertThat(event).isInstanceOfSatisfying(MatchChanged.class, changed -> {
                    assertThat(changed.gameKey()).isEqualTo("starfare");
                    assertThat(changed.matchId()).isEqualTo("game-1");
                }));
    }

    @Test
    void ignoresWhatTheLobbyDoesNotShow() {
        broadcaster.publish(new GameEvent.ObserverJoined(ID, "23"));
        broadcaster.publish(new GameEvent.GameFinished(ID, 1));

        assertThat(published).isEmpty();
    }

    @Test
    void announcesANewTurnOnlyWhileTheGameRuns() {
        when(session.readState(any())).thenReturn(false, true);

        broadcaster.publish(new GameEvent.TurnAdvanced(ID, 2));
        broadcaster.publish(new GameEvent.TurnAdvanced(ID, 3));

        assertThat(published).singleElement().isInstanceOf(MatchChanged.class);
    }

    @Test
    void announcesTheStartAndTheTurnsOfEveryoneWithTheirDeadline() {
        GameSummary running = game(2, true);
        when(session.summary()).thenReturn(running);

        broadcaster.publish(new GameEvent.GameStarted(ID));

        assertThat(published).hasSize(3);
        assertThat(published.get(1)).isInstanceOfSatisfying(MatchStarted.class, started ->
                assertThat(started.participants()).containsExactly(SEVENTEEN, TWENTY_THREE));
        assertThat(published.get(2)).isInstanceOfSatisfying(DecisionsOpened.class, opened ->
                assertThat(opened.decisions()).extracting(PendingDecision::account, PendingDecision::decisionId,
                                PendingDecision::deadline)
                        .containsExactly(tuple(SEVENTEEN, "round-1", running.roundDeadline()),
                                tuple(TWENTY_THREE, "round-1", running.roundDeadline())));
    }

    @Test
    void announcesTheRemainingTurnsAgainAfterASubmissionAndANewRound() {
        when(session.summary()).thenReturn(game(2, true));
        when(session.readState(any())).thenReturn(false);

        broadcaster.publish(new GameEvent.PlayerSubmitted(ID, 1));
        broadcaster.publish(new GameEvent.SeatAbandoned(ID, 2));
        broadcaster.publish(new GameEvent.TurnAdvanced(ID, 2));

        assertThat(published).filteredOn(DecisionsOpened.class::isInstance).hasSize(3);
    }

    @Test
    void announcesTheAnswersToATreatyProposalWhenItOpens() {
        GameSummary running = game(2, true);
        when(session.summary()).thenReturn(withAnswers(running, Map.of(2, List.of(5))));

        broadcaster.publish(new GameEvent.DiplomacyChanged(ID));

        assertThat(published).hasSize(2);
        assertThat(published.getFirst()).isInstanceOf(MatchChanged.class);
        assertThat(published.get(1)).isInstanceOfSatisfying(DecisionsOpened.class, opened ->
                assertThat(opened.decisions()).extracting(PendingDecision::account, PendingDecision::decisionId,
                                PendingDecision::kind, PendingDecision::deadline)
                        .containsExactly(tuple(SEVENTEEN, "round-1", DecisionKind.TURN, running.roundDeadline()),
                                tuple(TWENTY_THREE, "round-1", DecisionKind.TURN, running.roundDeadline()),
                                tuple(TWENTY_THREE, "round-1-answer-5", DecisionKind.ANSWER, running.roundDeadline())));
    }

    @Test
    void opensNoNoticeForTurnsWithoutDeadline() {
        when(session.summary()).thenReturn(game(1, true));
        when(session.readState(any())).thenReturn(false);

        broadcaster.publish(new GameEvent.TurnAdvanced(ID, 2));

        assertThat(published).singleElement().isInstanceOf(MatchChanged.class);
    }

    @Test
    void reportsAMissedRoundOfAPlatformAccount() {
        Instant missedAt = Instant.parse("2026-11-20T10:00:00Z");

        broadcaster.publish(new GameEvent.RoundMissed(ID, "23", 4, 2, true, missedAt));
        broadcaster.publish(new GameEvent.RoundMissed(ID, "alice", 4, 1, false, missedAt));

        assertThat(published).containsExactly(
                new TurnMissed("starfare", "game-1", TWENTY_THREE, "round-4", missedAt, 2, true));
    }

    /** A game of the accounts 17 and 23, of which {@code joined} play; {@code running} starts it. */
    private static GameSummary game(int joined, boolean running) {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.players().add(new Player(2, "P2", false, "#222222"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
        state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
        if (joined > 0) {
            state.seatByUser().put("17", 1);
            state.seatByUser().put("23", 2);
        }
        for (int seat = 1; seat <= joined; seat++) {
            state.joinedHumanPlayerIds().add(seat);
            state.originalHumanPlayerIds().add(seat);
        }
        if (running) {
            state.start();
        }
        return GameSession.summaryOf(ID, "Andromeda", "17", state);
    }

    private static GameSummary withAnswers(GameSummary game, Map<Integer, List<Integer>> answers) {
        return new GameSummary(game.gameId(), game.name(), game.hostPlayerId(), game.turn(), game.started(),
                game.gameOver(), game.observersAllowed(), game.reentryAllowed(), game.players(), game.joinedHumanSeats(),
                game.seatByPlayer(), game.invitedSeats(), game.visibility(), game.outcome(), game.ruleset(),
                game.victoryRules(), game.submittedSeats(), game.roundStartedAt(), game.closedSeats(),
                game.roundDeadline(), game.tempo(), game.tutorial(), answers);
    }

    @Test
    void endsAnAbortedRunningGameWithItsParticipants() {
        broadcaster.publish(new GameEvent.GameAborted(ID, true, List.of("23", "alice", "17")));

        assertThat(published).singleElement().isInstanceOfSatisfying(MatchEnded.class, ended -> {
            assertThat(ended.matchId()).isEqualTo("game-1");
            assertThat(ended.result()).isEqualTo(MatchResult.aborted(List.of(AccountId.of(23), AccountId.of(17))));
            assertThat(ended.tutorial()).isFalse();
        });
    }

    @Test
    void marksAnAbortedTutorialSoItStaysOutOfTheStatistics() {
        broadcaster.publish(new GameEvent.GameAborted(ID, true, List.of("17"), true, "extended"));

        assertThat(published).singleElement().isInstanceOfSatisfying(MatchEnded.class,
                ended -> {
                    assertThat(ended.tutorial()).isTrue();
                    assertThat(ended.variant()).isEqualTo("extended");
                });
    }

    @Test
    void letsACancelledOpenGameLeaveTheLobby() {
        broadcaster.publish(new GameEvent.GameAborted(ID, false, List.of("17")));
        broadcaster.publish(new GameEvent.GameAborted(GameId.of("game-2"), true, List.of("alice")));

        assertThat(published).hasSize(2).allSatisfy(event -> assertThat(event).isInstanceOf(MatchChanged.class));
    }
}
