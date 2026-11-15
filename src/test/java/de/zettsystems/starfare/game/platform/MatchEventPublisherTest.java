package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.MatchChanged;
import de.zettsystems.gamesplatformapi.MatchEnded;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.starfare.game.application.DefaultBroadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchEventPublisherTest {

    private static final GameId ID = GameId.of("game-1");

    private final DefaultBroadcaster broadcaster = new DefaultBroadcaster();
    private final GameRegistry registry = mock(GameRegistry.class);
    private final GameSession session = mock(GameSession.class);
    private final List<Object> published = new ArrayList<>();

    @BeforeEach
    void subscribe() {
        new MatchEventPublisher(broadcaster, registry, published::add).afterPropertiesSet();
        when(registry.find(ID)).thenReturn(Optional.of(session));
    }

    @Test
    void announcesChangesTheLobbyShows() {
        List<GameEvent> events = List.of(new GameEvent.GameCreated(ID), new GameEvent.PlayerJoined(ID, 1),
                new GameEvent.PlayerSubmitted(ID, 1), new GameEvent.SeatAbandoned(ID, 2), new GameEvent.GameStarted(ID),
                new GameEvent.GameContinued(ID), new GameEvent.VisibilityChanged(ID),
                new GameEvent.InvitationChanged(ID));

        events.forEach(broadcaster::publish);

        assertThat(published).hasSize(events.size()).allSatisfy(event ->
                assertThat(event).isInstanceOfSatisfying(MatchChanged.class, changed -> {
                    assertThat(changed.gameKey()).isEqualTo("starfare");
                    assertThat(changed.matchId()).isEqualTo("game-1");
                }));
    }

    @Test
    void ignoresWhatTheLobbyDoesNotShow() {
        broadcaster.publish(new GameEvent.ChatMessage(ID, "17", "hello", java.time.Instant.now()));
        broadcaster.publish(new GameEvent.DiplomacyChanged(ID));
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
    void endsAnAbortedRunningGameWithItsParticipants() {
        broadcaster.publish(new GameEvent.GameAborted(ID, true, List.of("23", "alice", "17")));

        assertThat(published).singleElement().isInstanceOfSatisfying(MatchEnded.class, ended -> {
            assertThat(ended.matchId()).isEqualTo("game-1");
            assertThat(ended.result()).isEqualTo(MatchResult.aborted(List.of(AccountId.of(23), AccountId.of(17))));
        });
    }

    @Test
    void letsACancelledOpenGameLeaveTheLobby() {
        broadcaster.publish(new GameEvent.GameAborted(ID, false, List.of("17")));
        broadcaster.publish(new GameEvent.GameAborted(GameId.of("game-2"), true, List.of("alice")));

        assertThat(published).hasSize(2).allSatisfy(event -> assertThat(event).isInstanceOf(MatchChanged.class));
    }
}
