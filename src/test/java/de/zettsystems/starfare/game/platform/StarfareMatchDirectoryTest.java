package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.ChatAccess;
import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.gamesplatformapi.MatchSummary;
import de.zettsystems.starfare.game.application.GameChatService;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarfareMatchDirectoryTest {

    private static final AccountId VIEWER = AccountId.of(17);

    private final GameService games = mock(GameService.class);
    private final GameChatService chat = mock(GameChatService.class);
    private final StarfareMatchDirectory directory = new StarfareMatchDirectory(games, chat);

    @Test
    void belongsToStarfare() {
        assertThat(directory.gameKey()).isEqualTo("starfare");
    }

    @Test
    void listsTheGamesOfTheAccountMostRecentlyChangedFirst() {
        GameSummary older = game("older", Instant.parse("2026-11-01T10:00:00Z"), state -> state.seatByUser().put("17", 1));
        GameSummary newer = game("newer", Instant.parse("2026-11-02T10:00:00Z"), state -> state.seatByUser().put("17", 2));
        GameSummary foreign = game("foreign", Instant.parse("2026-11-03T10:00:00Z"), state -> state.seatByUser().put("23", 1));
        when(games.visibleGamesFor("17", GameListScope.LOBBY)).thenReturn(List.of(older, foreign, newer));

        assertThat(directory.matchesOf(VIEWER)).extracting(MatchSummary::title).containsExactly("newer", "older");
    }

    @Test
    void listsAGameTheAccountOrganisesWithoutASeatAsItsOwnNotAsOpen() {
        GameSummary organised = game("organised", "17", Instant.parse("2026-11-01T10:00:00Z"), state -> { });
        when(games.visibleGamesFor("17", GameListScope.LOBBY)).thenReturn(List.of(organised));

        assertThat(directory.matchesOf(VIEWER)).extracting(MatchSummary::title).containsExactly("organised");
        assertThat(directory.openMatches(VIEWER)).isEmpty();
    }

    @Test
    void offersOnlyGamesTheViewerCanJoin() {
        GameSummary joinable = game("joinable", Instant.parse("2026-11-01T10:00:00Z"), state -> state.seatByUser().put("23", 1));
        GameSummary own = game("own", Instant.parse("2026-11-02T10:00:00Z"), state -> state.seatByUser().put("17", 1));
        GameSummary privateGame = game("private", Instant.parse("2026-11-03T10:00:00Z"), state -> {
            state.seatByUser().put("23", 1);
            state.makePrivate();
        });
        GameSummary reserved = game("reserved", Instant.parse("2026-11-04T10:00:00Z"), state -> {
            state.seatByUser().put("23", 1);
            state.joinedHumanPlayerIds().add(1);
            state.invitedSeats().put("42", 2);
        });
        when(games.visibleGamesFor("17", GameListScope.LOBBY)).thenReturn(List.of(joinable, own, privateGame, reserved));

        assertThat(directory.openMatches(VIEWER)).extracting(MatchSummary::title).containsExactly("joinable");
    }

    @Test
    void offersNoRunningGame() {
        GameSummary running = game("running", Instant.parse("2026-11-01T10:00:00Z"), state -> {
            state.seatByUser().put("23", 1);
            state.joinedHumanPlayerIds().add(1);
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.start();
        });
        when(games.visibleGamesFor("17", GameListScope.LOBBY)).thenReturn(List.of(running));

        assertThat(directory.openMatches(VIEWER)).isEmpty();
    }

    @Test
    void pagesThroughTheFinishedGamesOfTheAccount() {
        when(games.finishedMatchesOf("17")).thenReturn(List.of(finished("c"), finished("b"), finished("a")));

        assertThat(directory.finishedMatchesOf(VIEWER, 1, 5)).extracting(FinishedMatch::matchId).containsExactly("b", "a");
        assertThat(directory.finishedMatchesOf(VIEWER, 0, 1)).extracting(FinishedMatch::matchId).containsExactly("c");
        assertThat(directory.finishedMatchesOf(VIEWER, 3, 1)).isEmpty();
    }

    @Test
    void rejectsANegativeOffsetAndAnEmptyPage() {
        assertThatIllegalArgumentException().isThrownBy(() -> directory.finishedMatchesOf(VIEWER, -1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> directory.finishedMatchesOf(VIEWER, 0, 0));
    }

    @Test
    void answersChatAccessFromTheChatRulesOfTheGame() {
        when(chat.mayWrite(GameId.of("playing"), "17")).thenReturn(true);
        when(chat.mayRead(GameId.of("playing"), "17")).thenReturn(true);
        when(chat.mayRead(GameId.of("left"), "17")).thenReturn(true);

        assertThat(directory.chatAccess("playing", VIEWER)).isEqualTo(ChatAccess.WRITE);
        assertThat(directory.chatAccess("left", VIEWER)).isEqualTo(ChatAccess.READ);
        assertThat(directory.chatAccess("watched", VIEWER)).isEqualTo(ChatAccess.NONE);
        assertThat(directory.chatAccess(" ", VIEWER)).isEqualTo(ChatAccess.NONE);
    }

    private static FinishedMatch finished(String id) {
        return new FinishedMatch("starfare", id, id, Instant.parse("2026-11-01T10:00:00Z"),
                MatchResult.aborted(List.of(VIEWER)), false, "/starfare/map/" + id);
    }

    private static GameSummary game(String name, Instant lastChange, Consumer<GameState> setup) {
        return game(name, null, lastChange, setup);
    }

    private static GameSummary game(String name, @Nullable String host, Instant lastChange, Consumer<GameState> setup) {
        GameState state = new GameState();
        state.publishInLobby();
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.players().add(new Player(2, "P2", false, "#222222"));
        setup.accept(state);
        GameSummary summary = GameSession.summaryOf(GameId.of(name), name, host, state);
        return new GameSummary(summary.gameId(), summary.name(), summary.hostPlayerId(), summary.turn(),
                summary.started(), summary.gameOver(), summary.observersAllowed(), summary.reentryAllowed(),
                summary.players(), summary.joinedHumanSeats(), summary.seatByPlayer(), summary.invitedSeats(),
                summary.visibility(), summary.outcome(), summary.ruleset(), summary.victoryRules(),
                summary.submittedSeats(), lastChange, summary.closedSeats());
    }
}
