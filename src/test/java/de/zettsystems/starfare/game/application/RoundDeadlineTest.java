package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RoundStatus;
import de.zettsystems.starfare.game.values.StarSystem;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rundenlimit (Standard 5 min) und Nachzügler-Limit (Standard 1 min). Die Tests rufen
 * {@link GameService#enforceRoundDeadline(GameId, Instant)} selbst mit einem gewählten
 * Zeitpunkt auf; der Scheduler ist in {@link AbstractIntegrationTest} abgeschaltet.
 */
class RoundDeadlineTest extends AbstractIntegrationTest {

    private static final Duration FAR_BEYOND_ANY_LIMIT = Duration.ofDays(8);

    @Autowired
    private GameService game;

    private GameId runningGameWithTwoHumans() {
        return runningGameWithTwoHumans(null);
    }

    private GameId runningGameWithTwoHumans(@Nullable String host) {
        GameId id = host == null ? registry.createGame(GameSetup.defaults())
                : registry.createGame(GameSetup.defaults(), host, "deadline-game");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, true);
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 1, true));
            state.originalHumanPlayerIds().add(1);
            state.originalHumanPlayerIds().add(2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            state.seatByUser().put("alice", 1);
            state.seatByUser().put("bob", 2);
            state.start();
            return null;
        });
        return id;
    }

    private boolean isAi(GameId id, int seatId) {
        return registry.readState(id, state -> state.players().stream()
                .filter(p -> p.id() == seatId).findFirst().orElseThrow().ai());
    }

    private int turn(GameId id) {
        return registry.readState(id, GameState::turn);
    }

    /** Sitz 1 gibt jede Runde ab, Sitz 2 verpasst die Frist. */
    private void seatTwoMissesRounds(GameId id, int rounds) {
        for (int i = 0; i < rounds; i++) {
            game.submitTurn(id, 1);
            assertThat(game.enforceRoundDeadline(id, Instant.now().plus(FAR_BEYOND_ANY_LIMIT))).isTrue();
        }
    }

    @Test
    void roundLimitEndsTheRoundWithoutHandingSeatsToTheAi() {
        GameId id = runningGameWithTwoHumans();
        int before = turn(id);

        assertThat(game.enforceRoundDeadline(id, Instant.now().plus(Duration.ofMinutes(4)))).isFalse();
        assertThat(game.enforceRoundDeadline(id, Instant.now().plus(Duration.ofMinutes(6)))).isTrue();

        assertThat(turn(id)).isEqualTo(before + 1);
        assertThat(isAi(id, 1)).isFalse();
        assertThat(isAi(id, 2)).isFalse();
        Map<Integer, Integer> missed = registry.readState(id, state -> Map.copyOf(state.missedRounds()));
        assertThat(missed).containsEntry(1, 1).containsEntry(2, 1);
    }

    @Test
    void stragglerLimitStartsOnceOnlyOnePlayerIsMissing() {
        GameId id = runningGameWithTwoHumans();
        Instant soon = Instant.now().plus(Duration.ofSeconds(90));
        assertThat(game.enforceRoundDeadline(id, soon)).as("beide fehlen noch: nur das Rundenlimit zählt").isFalse();

        game.submitTurn(id, 1);

        assertThat(registry.readState(id, GameState::stragglerSince)).isNotNull();
        assertThat(game.enforceRoundDeadline(id, soon)).as("Nachzügler-Limit 1 min ist abgelaufen").isTrue();
        assertThat(registry.readState(id, GameState::stragglerSince)).as("neue Runde, neue Uhr").isNull();
    }

    @Test
    void lateSeatMovesWithTheOrdersGivenSoFar() {
        GameId id = runningGameWithTwoHumans();
        assertThat(game.sendFleet(id, 2, 2, 3, 4)).isTrue();
        game.submitTurn(id, 1);

        game.enforceRoundDeadline(id, Instant.now().plus(FAR_BEYOND_ANY_LIMIT));

        boolean fleetLaunched = registry.readState(id, state -> state.fleets().stream().anyMatch(f -> f.ownerId() == 2));
        assertThat(fleetLaunched).isTrue();
    }

    @Test
    void seatGoesToTheAiAfterTooManyMissedRoundsInARow() {
        GameId id = runningGameWithTwoHumans();

        seatTwoMissesRounds(id, GameConfig.MAX_MISSED_ROUNDS - 1);
        assertThat(isAi(id, 2)).isFalse();

        seatTwoMissesRounds(id, 1);
        assertThat(isAi(id, 2)).isTrue();
        assertThat(isAi(id, 1)).isFalse();
        Set<Integer> joined = registry.readState(id, GameState::joinedHumanPlayerIds);
        assertThat(joined).containsExactly(1);
    }

    @Test
    void submittingResetsTheMissedRoundCounter() {
        GameId id = runningGameWithTwoHumans();
        seatTwoMissesRounds(id, GameConfig.MAX_MISSED_ROUNDS - 1);

        game.submitTurn(id, 1);
        game.submitTurn(id, 2);
        seatTwoMissesRounds(id, 1);

        assertThat(isAi(id, 2)).isFalse();
    }

    @Test
    void handedOverSeatCanBeReclaimedWhenReentryIsAllowed() {
        GameId id = runningGameWithTwoHumans();
        seatTwoMissesRounds(id, GameConfig.MAX_MISSED_ROUNDS);

        Map<String, Integer> seats = registry.readState(id, GameState::seatByUser);
        assertThat(seats).as("die Sitzzuordnung bleibt, sonst greift kein Wiedereinstieg")
                .containsEntry("bob", 2);
        assertThat(registry.claimSeat(id, "bob")).contains(2);
        assertThat(isAi(id, 2)).as("Sitz ist wieder menschlich").isFalse();
    }

    @Test
    void handedOverSeatStaysAiWhenReentryIsForbidden() {
        GameId id = runningGameWithTwoHumans();
        registry.writeState(id, state -> {
            state.configureLobby(true, false);
            return null;
        });
        seatTwoMissesRounds(id, GameConfig.MAX_MISSED_ROUNDS);

        assertThat(registry.claimSeat(id, "bob")).isEmpty();
        assertThat(isAi(id, 2)).isTrue();
    }

    @Test
    void gameWithASingleHumanHasNoDeadline() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.joinedHumanPlayerIds().add(1);
            state.start();
            return null;
        });

        assertThat(registry.readState(id, GameState::roundDeadline)).isNull();
        assertThat(game.enforceRoundDeadline(id, Instant.now().plus(FAR_BEYOND_ANY_LIMIT))).isFalse();
    }

    @Test
    void gameThatHasNotStartedIsUntouched() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            return null;
        });

        assertThat(game.enforceRoundDeadline(id, Instant.now().plus(FAR_BEYOND_ANY_LIMIT))).isFalse();
    }

    @Test
    void finishedGameIsUntouched() {
        GameId id = runningGameWithTwoHumans();
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });

        assertThat(game.enforceRoundDeadline(id, Instant.now().plus(FAR_BEYOND_ANY_LIMIT))).isFalse();
    }

    @Test
    void hostRoleMovesOnWhenTheHostIsHandedToTheAi() {
        GameId id = runningGameWithTwoHumans("bob");

        seatTwoMissesRounds(id, GameConfig.MAX_MISSED_ROUNDS);

        assertThat(registry.require(id).hostPlayerId())
                .as("bob ist raus, alice übernimmt")
                .isEqualTo("alice");
    }

    @Test
    void viewShowsSubmissionsAndTheStragglerClock() {
        GameId id = runningGameWithTwoHumans();
        game.submitTurn(id, 1);

        RoundStatus status = game.viewFor(id, 1).roundStatus();

        assertThat(status.seats()).extracting(RoundStatus.Seat::submitted).containsExactly(true, false);
        assertThat(status.stragglerId()).isEqualTo(2);
        assertThat(status.deadline()).isNotNull()
                .isBefore(Instant.now().plus(Duration.ofSeconds(61)));
    }
}
