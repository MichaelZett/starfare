package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.gamesplatformapi.MatchDirectory;
import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The platform builds its pages from Starfare's lists (running, open, invitations, finished). Their cost must not
 * grow with the number of games: no lookup per game, the lobby read once per request, only the finished games of
 * the requested page read (platform measurement 25.11.2026).
 */
class MatchDirectoryQueryBudgetTest extends AbstractIntegrationTest {
    private static final AccountId VIEWER = AccountId.of(17);
    private static final int FOREIGN_GAMES = 6;

    @Autowired private GameService games;
    @Autowired private MatchDirectory directory;
    @Autowired private GameArchiveRepository archives;
    @Autowired private EntityManagerFactory entityManagers;

    private Statistics statistics;

    @BeforeEach
    void manyGamesOfOthersAndSomeOfTheViewer() {
        archives.deleteAll();
        statistics = entityManagers.unwrap(SessionFactory.class).getStatistics();
        started("17", "Own running");
        finished("17", "Own first");
        finished("17", "Own second");
        for (int game = 0; game < FOREIGN_GAMES; game++) {
            started("23", "Foreign running " + game);
            finished("23", "Foreign finished " + game);
        }
    }

    @AfterEach
    void leaveTheRequest() {
        RequestContextHolder.resetRequestAttributes();
        archives.deleteAll();
    }

    @Test
    void theListsOfOnePageReadTheLobbyOnceWithoutALookupPerGame() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        long statements = statementsFor(() -> {
            assertThat(directory.matchesOf(VIEWER)).hasSize(1);
            assertThat(directory.openMatches(VIEWER)).isEmpty();
            assertThat(directory.invitationsOf(VIEWER)).isEmpty();
        });

        assertThat(statements).as("one list of the game ids for all three lists, nothing per foreign game")
                .isLessThanOrEqualTo(1);
    }

    @Test
    void withoutARequestEachListStillCostsOneQuery() {
        assertThat(statementsFor(() -> directory.matchesOf(VIEWER))).isLessThanOrEqualTo(1);
    }

    @Test
    void aPageOfFinishedGamesReadsOnlyItsOwnArchives() {
        statistics.clear();
        var page = directory.finishedMatchesOf(VIEWER, 0, 1);

        assertThat(page).extracting(FinishedMatch::title).containsExactly("Own second");
        assertThat(statistics.getEntityLoadCount()).as("archives read for a page of one").isLessThanOrEqualTo(1);
        assertThat(statistics.getPrepareStatementCount()).as("game ids, own archive ids, the page")
                .isLessThanOrEqualTo(3);
        assertThat(directory.finishedMatchesOf(VIEWER, 1, 5)).extracting(FinishedMatch::title)
                .containsExactly("Own first");
    }

    private long statementsFor(Runnable action) {
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    private GameId started(String account, String name) {
        GameId id = games.newGame(GameSetup.defaults(), account, name);
        games.joinGame(id, account);
        games.startGame(id);
        return id;
    }

    private void finished(String account, String name) {
        GameId id = started(account, name);
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });
    }
}
