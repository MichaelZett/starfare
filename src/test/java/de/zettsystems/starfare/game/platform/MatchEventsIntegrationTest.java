package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.MatchChanged;
import de.zettsystems.gamesplatformapi.MatchDirectory;
import de.zettsystems.gamesplatformapi.MatchEnded;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.gamesplatformapi.MatchStatus;
import de.zettsystems.gamesplatformapi.Placement;
import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameEndRecorder;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The platform's view of Starfare with a real database: events reach a listener only once the
 * change is committed (or stored, for changes Starfare keeps outside a transaction).
 */
@Import(MatchEventsIntegrationTest.Recorder.class)
class MatchEventsIntegrationTest extends AbstractIntegrationTest {

    private static final AccountId SEVENTEEN = AccountId.of(17);
    private static final AccountId TWENTY_THREE = AccountId.of(23);

    @Autowired private GameService games;
    @Autowired private GameEndRecorder gameEnds;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MatchDirectory directory;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private Recorder recorder;

    @BeforeEach
    void forgetEvents() {
        recorder.events.clear();
    }

    @Test
    void offersStarfareAsTheOnlyMatchDirectory() {
        assertThat(directory.gameKey()).isEqualTo("starfare");
    }

    @Test
    void announcesANewGameAndListsItOnceTheHostTakesASeat() {
        GameId id = games.newGame(GameSetup.defaults(), "17", "Andromeda");
        assertThat(recorder.of(id)).singleElement().isInstanceOf(MatchChanged.class);

        assertThat(games.joinGame(id, "17")).isPresent();

        assertThat(recorder.of(id)).hasSize(2).allSatisfy(event -> assertThat(event).isInstanceOf(MatchChanged.class));
        assertThat(directory.matchesOf(SEVENTEEN)).singleElement().satisfies(match -> {
            assertThat(match.matchId()).isEqualTo(id.value());
            assertThat(match.status()).isEqualTo(MatchStatus.OPEN);
            assertThat(match.viewPath()).isEqualTo("/starfare/match/" + id.value());
        });
    }

    @Test
    void endsAFinishedGameOnceAfterTheResultIsCommitted() {
        GameId id = finishedGame();

        gameEnds.recordFinishedGame(id);
        gameEnds.recordFinishedGame(id);

        assertThat(recorder.of(id)).singleElement().isInstanceOfSatisfying(MatchEnded.class, ended ->
                assertThat(ended.result()).isEqualTo(MatchResult.ranked(List.of(
                        new Placement(SEVENTEEN, 1), new Placement(TWENTY_THREE, 2)))));
    }

    @Test
    void announcesNothingWhenRecordingTheResultRollsBack() {
        GameId id = finishedGame();

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            gameEnds.recordFinishedGame(id);
            status.setRollbackOnly();
        });

        assertThat(recorder.of(id)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from game_results where game_id = ?", Integer.class, id.value()))
                .isZero();
    }

    @Test
    void endsAnAbortedRunningGameWithoutPlaces() {
        GameId id = runningGame();
        recorder.events.clear();

        games.abortGame(id);

        assertThat(recorder.of(id)).singleElement().isInstanceOfSatisfying(MatchEnded.class, ended ->
                assertThat(ended.result()).isEqualTo(MatchResult.aborted(List.of(SEVENTEEN, TWENTY_THREE))));
    }

    private GameId finishedGame() {
        GameId id = runningGame();
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });
        recorder.events.clear();
        return id;
    }

    private GameId runningGame() {
        GameId id = games.newGame(GameSetup.defaults(), "17", "Andromeda");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.seatByUser().put("17", 1);
            state.seatByUser().put("23", 2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            state.originalHumanPlayerIds().add(1);
            state.originalHumanPlayerIds().add(2);
            state.start();
            return null;
        });
        return id;
    }

    @TestConfiguration
    static class Recorder {
        final List<Object> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(fallbackExecution = true)
        void changed(MatchChanged event) {
            events.add(event);
        }

        @TransactionalEventListener(fallbackExecution = true)
        void ended(MatchEnded event) {
            events.add(event);
        }

        List<Object> of(GameId id) {
            return events.stream().filter(event -> switch (event) {
                case MatchChanged changed -> changed.matchId().equals(id.value());
                case MatchEnded ended -> ended.matchId().equals(id.value());
                default -> false;
            }).toList();
        }
    }
}
