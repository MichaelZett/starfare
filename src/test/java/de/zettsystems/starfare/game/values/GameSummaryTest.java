package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GameSummaryTest {
    private static final String HOST = "host";
    private static final String SEATED = "bob";
    private static final List<Player> PLAYERS = List.of(new Player(1, "P1", false, "#111111"),
            new Player(2, "P2", false, "#222222"));

    @Test
    void aSeatedAccountJoinsItsFreeSeatBeforeTheStart() {
        assertThat(summary(false, false, Set.of(1), Set.of()).canJoin(SEATED)).isTrue();
        assertThat(summary(false, false, Set.of(1, 2), Set.of()).canJoin(SEATED)).as("already joined").isFalse();
    }

    @Test
    void afterTheStartOnlyAReopenableSeatOfAGameWithReentryCanBeTakenBack() {
        assertThat(summary(true, true, Set.of(1), Set.of()).canJoin(SEATED)).as("left, reentry allowed").isTrue();
        assertThat(summary(true, false, Set.of(1), Set.of()).canJoin(SEATED)).as("no reentry").isFalse();
        assertThat(summary(true, true, Set.of(1), Set.of(2)).canJoin(SEATED)).as("resigned or timed out").isFalse();
    }

    @Test
    void finishedGamesAndBlankAccountsCannotBeJoined() {
        GameSummary finished = new GameSummary(GameId.of("summary"), "Summary", HOST, 3, true, true, false, true,
                PLAYERS, Set.of(1), Map.of(SEATED, 2), Map.of(), GameVisibility.PUBLIC, new GameOutcome(1, null),
                RulesetRef.SECTOR_FORCES, null, Set.of(), Instant.EPOCH, Set.of());

        assertThat(finished.canJoin(SEATED)).isFalse();
        assertThat(summary(false, false, Set.of(1), Set.of()).canJoin(" ")).isFalse();
    }

    private static GameSummary summary(boolean started, boolean reentryAllowed, Set<Integer> joined, Set<Integer> closed) {
        return new GameSummary(GameId.of("summary"), "Summary", HOST, 1, started, false, false, reentryAllowed,
                PLAYERS, joined, Map.of(SEATED, 2), Map.of(), GameVisibility.PRIVATE, new GameOutcome(null, null),
                RulesetRef.SECTOR_FORCES, null, Set.of(), Instant.EPOCH, closed);
    }
}
