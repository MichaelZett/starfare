package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.Departure;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.gamesplatformapi.MatchStatus;
import de.zettsystems.gamesplatformapi.MatchSummary;
import de.zettsystems.gamesplatformapi.Placement;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StarfareMatchesTest {

    private static final AccountId SEVENTEEN = AccountId.of(17);
    private static final AccountId TWENTY_THREE = AccountId.of(23);
    private static final GameId ID = GameId.of("6f0c5c0e-1111-4222-8333-944445555666");

    @Nested
    class Accounts {

        @Test
        void readsZsIdentityUserIds() {
            assertThat(StarfareMatches.account("17")).contains(SEVENTEEN);
            assertThat(StarfareMatches.accountName(SEVENTEEN)).isEqualTo("17");
        }

        @Test
        void leavesOutNamesThatAreNoUserIds() {
            assertThat(StarfareMatches.account("alice")).isEmpty();
            assertThat(StarfareMatches.account("0")).isEmpty();
            assertThat(StarfareMatches.accounts(List.of("alice", "23", "17", "23")))
                    .containsExactly(TWENTY_THREE, SEVENTEEN);
        }
    }

    @Nested
    class Summary {

        @Test
        void describesAnOpenGameWithAComputerOpponentAndAFreeSeat() {
            GameState state = new GameState();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.players().add(new Player(3, "AI", true, "#333333"));
            state.seatByUser().put("17", 1);
            state.joinedHumanPlayerIds().add(1);

            MatchSummary summary = summaryOf(state);

            assertThat(summary.gameKey()).isEqualTo("starfare");
            assertThat(summary.matchId()).isEqualTo(ID.value());
            assertThat(summary.title()).isEqualTo("Andromeda");
            assertThat(summary.status()).isEqualTo(MatchStatus.OPEN);
            assertThat(summary.participants()).containsExactly(SEVENTEEN);
            assertThat(summary.seats()).isEqualTo(3);
            assertThat(summary.computerSeats()).isOne();
            assertThat(summary.isJoinable()).isTrue();
            assertThat(summary.toMove()).isEmpty();
            assertThat(summary.lastChange()).isEqualTo(state.turnStartedAt());
            assertThat(summary.viewPath()).as("an open game opens its waiting room").isEqualTo("/starfare/match/" + ID.value());
        }

        @Test
        void waitsForThePeopleWhoHaveNotSubmittedTheRound() {
            GameState state = runningGame();
            state.submittedThisTurn().add(1);

            MatchSummary summary = summaryOf(state);

            assertThat(summary.status()).isEqualTo(MatchStatus.RUNNING);
            assertThat(summary.participants()).containsExactly(SEVENTEEN, TWENTY_THREE);
            assertThat(summary.toMove()).containsExactly(TWENTY_THREE);
            assertThat(summary.viewPath()).as("a running game opens its map").isEqualTo("/starfare/map/" + ID.value());
        }

        @Test
        void keepsTheSeatOfAPersonWhoLeftForPeople() {
            GameState state = runningGame();
            state.updatePlayer(2, Player::asAi);
            state.joinedHumanPlayerIds().remove(2);

            MatchSummary summary = summaryOf(state);

            assertThat(summary.computerSeats()).isZero();
            assertThat(summary.participants()).containsExactly(SEVENTEEN, TWENTY_THREE);
            assertThat(summary.toMove()).containsExactly(SEVENTEEN);
        }

        @Test
        void countsAnInvitedSeatAsSeatForPeople() {
            GameState state = new GameState();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", true, "#222222"));
            state.seatByUser().put("17", 1);
            state.invitedSeats().put("23", 2);

            assertThat(summaryOf(state).computerSeats()).isZero();
        }

        @Test
        void leavesOutAGameOfComputerOpponentsOnly() {
            GameState state = new GameState();
            state.players().add(new Player(1, "AI 1", true, "#111111"));
            state.players().add(new Player(2, "AI 2", true, "#222222"));

            assertThat(StarfareMatches.summary(GameSession.summaryOf(ID, "Andromeda", null, state))).isEmpty();
        }

        @Test
        void fallsBackToTheIdWithoutAName() {
            GameState state = runningGame();

            assertThat(StarfareMatches.summary(GameSession.summaryOf(ID, " ", null, state)).orElseThrow().title())
                    .isEqualTo(ID.value());
        }

        private MatchSummary summaryOf(GameState state) {
            return StarfareMatches.summary(GameSession.summaryOf(ID, "Andromeda", "17", state)).orElseThrow();
        }
    }

    @Nested
    class Result {

        @Test
        void ranksTheWinnerFirstAndTheOthersBySystems() {
            GameState state = runningGame();
            state.players().add(new Player(3, "AI", true, "#333333"));
            state.systems().add(new StarSystem(4, "S4", 0, 500, 3, 10, 2, false));
            state.systems().add(new StarSystem(5, "S5", 0, 900, 3, 10, 2, false));
            state.endGame(1);

            MatchResult result = StarfareMatches.result(state).orElseThrow();

            assertThat(result).isInstanceOfSatisfying(MatchResult.Ranked.class, ranked ->
                    assertThat(ranked.placements()).containsExactly(
                            new Placement(SEVENTEEN, 1), new Placement(TWENTY_THREE, 3)));
        }

        @Test
        void ranksByScoreWhenTheRoundLimitEndedTheGame() {
            GameState state = new GameState();
            state.rememberSetup(de.zettsystems.starfare.game.values.GameSetup.defaults().withSectorOptions(
                    new de.zettsystems.starfare.game.values.SectorOptions(
                            de.zettsystems.starfare.game.values.EventFrequency.OFF, null, false, 10)));
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.players().add(new Player(3, "AI", true, "#333333"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 60, 2, false));
            state.systems().add(new StarSystem(4, "S4", 0, 500, 3, 1, 2, false));
            state.systems().add(new StarSystem(5, "S5", 0, 900, 3, 1, 2, false));
            state.seatByUser().put("17", 1);
            state.seatByUser().put("23", 2);
            state.joinedHumanPlayerIds().addAll(List.of(1, 2));
            state.originalHumanPlayerIds().addAll(List.of(1, 2));
            state.start();
            while (state.turn() < 10) {
                state.nextTurn();
            }
            state.endGame(1);

            // By systems the AI (two systems) would come before P2 (one system with 60 ships).
            assertThat(StarfareMatches.result(state).orElseThrow()).isInstanceOfSatisfying(MatchResult.Ranked.class,
                    ranked -> assertThat(ranked.placements()).containsExactly(
                            new Placement(SEVENTEEN, 1), new Placement(TWENTY_THREE, 2)));
        }

        @Test
        void putsEveryAccountBehindAWinningComputerOpponent() {
            GameState state = runningGame();
            state.players().add(new Player(3, "AI", true, "#333333"));
            state.endGame(3);

            MatchResult result = StarfareMatches.result(state).orElseThrow();

            assertThat(result.winners()).isEmpty();
            assertThat(result.placeOf(SEVENTEEN)).hasValue(2);
            assertThat(result.placeOf(TWENTY_THREE)).hasValue(2);
        }

        @Test
        void marksAPersonWhoseSeatAComputerOpponentTookOverAsResigned() {
            GameState state = runningGame();
            state.updatePlayer(2, Player::asAi);
            state.joinedHumanPlayerIds().remove(2);
            state.endGame(1);

            assertThat(placementOf(state, TWENTY_THREE).departure()).isEqualTo(Departure.RESIGNED);
        }

        @Test
        void marksAPersonWithoutSystemsAndFleetsAsEliminated() {
            GameState state = runningGame();
            state.updateSystem(2, system -> new StarSystem(2, "S2", 1000, 0, 1, 10, 2, false));
            state.endGame(1);

            assertThat(placementOf(state, TWENTY_THREE).departure()).isEqualTo(Departure.ELIMINATED);
            assertThat(placementOf(state, SEVENTEEN).departure()).isEqualTo(Departure.NONE);
        }

        @Test
        void keepsAPersonWithAFleetLeftInTheGame() {
            GameState state = runningGame();
            state.updateSystem(2, system -> new StarSystem(2, "S2", 1000, 0, 1, 10, 2, false));
            state.addFleet(2, 1, 3, 5);
            state.endGame(1);

            assertThat(placementOf(state, TWENTY_THREE).departure()).isEqualTo(Departure.NONE);
        }

        @Test
        void countsAGameWithoutWinnerAsAborted() {
            GameState state = runningGame();
            state.endGame(null);

            assertThat(StarfareMatches.result(state).orElseThrow()).isEqualTo(
                    MatchResult.aborted(List.of(SEVENTEEN, TWENTY_THREE)));
        }

        @Test
        void hasNoResultWithoutAccounts() {
            GameState state = runningGame();
            state.seatByUser().clear();
            state.endGame(1);

            assertThat(StarfareMatches.result(state)).isEmpty();
        }

        private Placement placementOf(GameState state, AccountId account) {
            MatchResult.Ranked ranked = (MatchResult.Ranked) StarfareMatches.result(state).orElseThrow();
            return ranked.placements().stream().filter(p -> p.account().equals(account)).findFirst().orElseThrow();
        }
    }

    private static GameState runningGame() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.players().add(new Player(2, "P2", false, "#222222"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
        state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
        state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 2, true));
        state.seatByUser().put("17", 1);
        state.seatByUser().put("23", 2);
        state.joinedHumanPlayerIds().add(1);
        state.joinedHumanPlayerIds().add(2);
        state.originalHumanPlayerIds().add(1);
        state.originalHumanPlayerIds().add(2);
        state.start();
        return state;
    }
}
