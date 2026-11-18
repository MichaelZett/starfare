package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameOutcome;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Player;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LobbyFilterTest {
    private static final String HOST = "1";
    private static final String SEATED = "2";
    private static final String STRANGER = "3";
    private static final List<Player> PLAYERS = List.of(new Player(1, "Host", false, "#ff0000"),
            new Player(2, "Guest", false, "#00ff00"));

    @Test
    void allMatchesEveryGame() {
        assertThat(LobbyView.LobbyFilter.ALL.matches(row(lobby(Set.of(1, 2)), STRANGER))).isTrue();
        assertThat(LobbyView.LobbyFilter.ALL.matches(row(finished(), null))).isTrue();
    }

    @Test
    void ownMatchesSeatedAccountsAndTheHostButNotStrangers() {
        GameSummary game = new GameSummary(GameId.of("lobby-own"), "Own", HOST, 1, false, false, false, false,
                PLAYERS, Set.of(2), Map.of(SEATED, 2), Map.of(), GameVisibility.PUBLIC, new GameOutcome(null, null));

        assertThat(LobbyView.LobbyFilter.OWN.matches(row(game, SEATED))).as("seated, not hosting").isTrue();
        assertThat(LobbyView.LobbyFilter.OWN.matches(row(game, HOST))).as("hosting, not seated").isTrue();
        assertThat(LobbyView.LobbyFilter.OWN.matches(row(game, STRANGER))).isFalse();
        assertThat(LobbyView.LobbyFilter.OWN.matches(row(game, null))).isFalse();
    }

    @Test
    void openMatchesOnlyGamesThatHaveNotStartedAndStillHaveAFreeHumanSeat() {
        assertThat(LobbyView.LobbyFilter.OPEN.matches(row(lobby(Set.of(1)), STRANGER))).isTrue();
        assertThat(LobbyView.LobbyFilter.OPEN.matches(row(lobby(Set.of(1, 2)), STRANGER))).as("all seats taken").isFalse();
        assertThat(LobbyView.LobbyFilter.OPEN.matches(row(running(Set.of(1)), STRANGER))).as("already started").isFalse();
    }

    @Test
    void runningMatchesOnlyStartedGamesThatAreNotOver() {
        assertThat(LobbyView.LobbyFilter.RUNNING.matches(row(running(Set.of(1, 2)), STRANGER))).isTrue();
        assertThat(LobbyView.LobbyFilter.RUNNING.matches(row(finished(), STRANGER))).as("game over").isFalse();
        assertThat(LobbyView.LobbyFilter.RUNNING.matches(row(lobby(Set.of(1)), STRANGER))).as("not started").isFalse();
    }

    private static LobbyView.LobbyGameRow row(GameSummary summary, @Nullable String account) {
        return LobbyView.LobbyGameRow.from(summary, account);
    }

    private static GameSummary lobby(Set<Integer> joined) {
        return game(false, false, joined);
    }

    private static GameSummary running(Set<Integer> joined) {
        return game(true, false, joined);
    }

    private static GameSummary finished() {
        return game(true, true, Set.of(1, 2));
    }

    private static GameSummary game(boolean started, boolean over, Set<Integer> joined) {
        return new GameSummary(GameId.of("lobby-filter"), "Filter", HOST, 3, started, over, false, false,
                PLAYERS, joined, Map.of(HOST, 1), Map.of(), GameVisibility.PUBLIC, new GameOutcome(null, null));
    }
}
