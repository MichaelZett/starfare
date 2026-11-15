package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.MatchSummary;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarfareMatchDirectoryTest {

    private static final AccountId VIEWER = AccountId.of(17);

    private final GameService games = mock(GameService.class);
    private final StarfareMatchDirectory directory = new StarfareMatchDirectory(games);

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

    private static GameSummary game(String name, Instant lastChange, Consumer<GameState> setup) {
        GameState state = new GameState();
        state.publishInLobby();
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.players().add(new Player(2, "P2", false, "#222222"));
        setup.accept(state);
        GameSummary summary = GameSession.summaryOf(GameId.of(name), name, null, state);
        return new GameSummary(summary.gameId(), summary.name(), summary.hostPlayerId(), summary.turn(),
                summary.started(), summary.gameOver(), summary.observersAllowed(), summary.reentryAllowed(),
                summary.players(), summary.joinedHumanSeats(), summary.seatByPlayer(), summary.invitedSeats(),
                summary.visibility(), summary.outcome(), summary.ruleset(), summary.victoryRules(),
                summary.submittedSeats(), lastChange);
    }
}
