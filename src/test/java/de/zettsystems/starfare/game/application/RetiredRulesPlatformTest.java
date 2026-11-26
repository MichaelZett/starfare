package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.ChatAccess;
import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.gamesplatformapi.MatchDirectory;
import de.zettsystems.gamesplatformapi.MatchSummary;
import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameSessionEntity;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Games of the removed extended versions 1.x no longer load. They must not disturb the platform: its lobby, the game
 * page, the finished games and the open decisions leave them out instead of failing, and their rows stay stored.
 */
class RetiredRulesPlatformTest extends AbstractIntegrationTest {
    private static final String RUNNING = "retired-running";
    private static final String FINISHED = "retired-finished";
    private static final AccountId ACCOUNT = AccountId.of(17);
    private static final Instant CREATED = Instant.parse("2026-11-20T10:00:00Z");

    @Autowired private JpaGameSessionStore store;
    @Autowired private GameSessionRepository sessions;
    @Autowired private GameArchiveRepository archives;
    @Autowired private MatchDirectory directory;
    @Autowired private GameService games;
    @Autowired private ObjectMapper mapper;

    @AfterEach
    void dropRetiredRows() {
        sessions.deleteById(RUNNING);
        archives.deleteById(FINISHED);
    }

    @Test
    void retiredExtendedGamesAreLeftOutOfEveryPlatformListWithoutErrors() {
        GameId current = registry.createGame(GameSetup.defaults().selectRuleset(RulesetRef.EXTENDED), "17", "Current");
        String json = sessions.findById(current.value()).orElseThrow().getStateJson();
        sessions.saveAndFlush(new GameSessionEntity(RUNNING, "Retired running", "17", CREATED, retired(json, false)));
        archives.saveAndFlush(new GameArchiveEntity(FINISHED, "Retired finished", "17", CREATED, retired(json, true)));

        assertThatCode(store::loadFromDatabase).doesNotThrowAnyException();

        assertThat(directory.matchesOf(ACCOUNT)).extracting(MatchSummary::matchId)
                .contains(current.value()).doesNotContain(RUNNING, FINISHED);
        assertThat(directory.openMatches(AccountId.of(23))).extracting(MatchSummary::matchId).doesNotContain(RUNNING, FINISHED);
        assertThat(directory.invitationsOf(ACCOUNT)).isEmpty();
        assertThat(directory.finishedMatchesOf(ACCOUNT, 0, 50)).extracting(FinishedMatch::matchId).doesNotContain(RUNNING, FINISHED);
        assertThat(games.finishedMatchesOf("17", 0, 100)).noneMatch(match -> FINISHED.equals(match.matchId()));
        assertThat(directory.chatAccess(RUNNING, ACCOUNT)).isEqualTo(ChatAccess.NONE);
        assertThat(directory.chatAccess(FINISHED, ACCOUNT)).isEqualTo(ChatAccess.NONE);
        assertThat(registry.find(GameId.of(RUNNING))).isEmpty();
        assertThat(sessions.findById(RUNNING)).as("kept for inspection or a deliberate deletion").isPresent();
        assertThat(archives.findById(FINISHED)).isPresent();
    }

    /** The stored game with the ruleset of the former extended version 1.3.0-r1, optionally finished. */
    private String retired(String json, boolean finished) {
        var tree = (ObjectNode) mapper.readTree(json);
        ((ObjectNode) tree.get("ruleset")).put("version", "1.3.0-r1");
        if (finished) {
            tree.put("gameOver", true);
            tree.put("finishedAt", CREATED.toString());
        }
        return mapper.writeValueAsString(tree);
    }
}
