package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.Departure;
import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.gamesplatformapi.Placement;
import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** The listing stored beside an archive gives the platform the same entry as the archived state. */
class ArchiveListingTest {
    private static final GameId ID = GameId.of("6f0c5c0e-1111-4222-8333-944445555666");
    private static final Instant ENDED = Instant.parse("2026-11-19T20:00:00Z");

    @Test
    void aRankedGameKeepsPlacesAndDeparturesOfItsPlatformAccounts() {
        GameState state = finishedGame(false);
        state.seatByUser().put("fixture", 3);
        state.joinedHumanPlayerIds().remove(2);
        state.endGame(1);

        FinishedMatch match = listedAndRead("Andromeda", state).orElseThrow();

        assertThat(match).isEqualTo(new FinishedMatch("starfare", ID.value(), "Andromeda", ENDED,
                StarfareMatches.result(state).orElseThrow(), false, "/starfare/map/" + ID.value()));
        assertThat(match.placementOf(AccountId.of(23))).contains(new Placement(AccountId.of(23), 2, Departure.RESIGNED));
    }

    @Test
    void anAbortedGameNamesItsAccountsInSeatOrder() {
        GameState state = finishedGame(false);
        state.endGame(null);

        FinishedMatch match = listedAndRead(" ", state).orElseThrow();

        assertThat(match.result()).isEqualTo(MatchResult.aborted(java.util.List.of(AccountId.of(17), AccountId.of(23))));
        assertThat(match.title()).as("the id without a name").isEqualTo(ID.value());
    }

    @Test
    void marksATutorial() {
        GameState state = finishedGame(true);
        state.endGame(1);

        assertThat(listedAndRead("Andromeda", state)).hasValueSatisfying(match -> assertThat(match.tutorial()).isTrue());
    }

    @Test
    void aGameWithoutPlatformAccountsHasNoEntry() {
        GameState state = finishedGame(false);
        state.seatByUser().clear();
        state.seatByUser().put("fixture", 1);
        state.endGame(1);

        assertThat(listedAndRead("Andromeda", state)).isEmpty();
    }

    @Test
    void anUnreadableArchiveIsListedWithoutSeats() {
        var entity = new GameArchiveEntity(ID.value(), "Broken", null, ENDED, "{}");

        ArchiveListing.unlistable(entity);

        assertThat(entity.isListed()).isTrue();
        assertThat(entity.getSeats()).isEmpty();
    }

    private static Optional<FinishedMatch> listedAndRead(String name, GameState state) {
        var entity = new GameArchiveEntity(ID.value(), name, "17", ENDED, "{}");
        assertThat(entity.isListed()).isFalse();
        ArchiveListing.list(entity, state);
        assertThat(entity.isListed()).isTrue();
        return ArchiveListing.finished(ID, entity.getName(), entity.getFinishedAt(), entity.isTutorial(),
                entity.isAborted(), entity.getSeats().reversed());
    }

    /** Accounts 17 (seat 1) and 23 (seat 2), each with a home system. */
    private static GameState finishedGame(boolean tutorial) {
        GameState state = new GameState();
        if (tutorial) {
            state.rememberSetup(GameSetup.defaults().chooseTutorial(true));
        }
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.players().add(new Player(2, "P2", false, "#222222"));
        state.players().add(new Player(3, "P3", false, "#333333"));
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
