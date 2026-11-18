package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameOutcome;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Player;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveResultTest {
    private static final String ACCOUNT = "7";
    private static final String STRANGER = "8";

    @Test
    void allMatchesEveryGameIncludingThoseWithoutAnOwnSeat() {
        assertThat(ArchiveView.ArchiveResult.ALL.matches(finished(2), ACCOUNT)).isTrue();
        assertThat(ArchiveView.ArchiveResult.ALL.matches(finished(null), STRANGER)).isTrue();
    }

    @Test
    void victoryMatchesOnlyGamesTheAccountWon() {
        assertThat(ArchiveView.ArchiveResult.VICTORY.matches(finished(1), ACCOUNT)).isTrue();
        assertThat(ArchiveView.ArchiveResult.VICTORY.matches(finished(2), ACCOUNT)).isFalse();
        assertThat(ArchiveView.ArchiveResult.VICTORY.matches(finished(null), ACCOUNT)).isFalse();
        assertThat(ArchiveView.ArchiveResult.VICTORY.matches(finished(1), STRANGER)).isFalse();
    }

    @Test
    void victoryIncludesAJointAllianceVictory() {
        GameSummary alliance = summary(new GameOutcome(null, Instant.EPOCH, List.of(1, 2), 4));

        assertThat(ArchiveView.ArchiveResult.VICTORY.matches(alliance, ACCOUNT)).isTrue();
        assertThat(ArchiveView.ArchiveResult.DEFEAT.matches(alliance, ACCOUNT)).isFalse();
    }

    @Test
    void defeatMatchesOnlyGamesSomebodyElseWon() {
        assertThat(ArchiveView.ArchiveResult.DEFEAT.matches(finished(2), ACCOUNT)).isTrue();
        assertThat(ArchiveView.ArchiveResult.DEFEAT.matches(finished(1), ACCOUNT)).isFalse();
        assertThat(ArchiveView.ArchiveResult.DEFEAT.matches(finished(null), ACCOUNT)).isFalse();
        assertThat(ArchiveView.ArchiveResult.DEFEAT.matches(finished(2), STRANGER)).isFalse();
    }

    @Test
    void noWinnerMatchesOnlyOwnGamesWithoutAWinner() {
        assertThat(ArchiveView.ArchiveResult.NO_WINNER.matches(finished(null), ACCOUNT)).isTrue();
        assertThat(ArchiveView.ArchiveResult.NO_WINNER.matches(finished(1), ACCOUNT)).isFalse();
        assertThat(ArchiveView.ArchiveResult.NO_WINNER.matches(finished(2), ACCOUNT)).isFalse();
        assertThat(ArchiveView.ArchiveResult.NO_WINNER.matches(finished(null), STRANGER)).isFalse();
    }

    private static GameSummary finished(@Nullable Integer winner) {
        return summary(new GameOutcome(winner, Instant.EPOCH));
    }

    /** The account sits on seat 1, an opponent on seat 2. */
    private static GameSummary summary(GameOutcome outcome) {
        List<Player> players = List.of(new Player(1, "Seven", false, "#ff0000"), new Player(2, "Rival", false, "#00ff00"));
        return new GameSummary(GameId.of("archive-result"), "Archive", ACCOUNT, 12, true, true, false, false,
                players, Set.of(1, 2), Map.of(ACCOUNT, 1, "9", 2), Map.of(), GameVisibility.PUBLIC, outcome);
    }
}
