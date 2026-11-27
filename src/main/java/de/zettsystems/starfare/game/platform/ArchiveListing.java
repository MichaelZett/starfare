package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.Departure;
import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.gamesplatformapi.Placement;
import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the platform's list of finished games shows of an archive, stored beside its snapshot: computed from the
 * state once when the game is archived ({@link #list}), read back without the snapshot ({@link #finished}). The
 * result is the one {@link StarfareMatches#result(GameState)} gives.
 */
public final class ArchiveListing {
    private ArchiveListing() {
    }

    /** Fills the entity's listing from the archived state: every seat, places and departures if ranked. */
    public static void list(GameArchiveEntity entity, GameState state) {
        Optional<MatchResult> result = StarfareMatches.result(state);
        List<GameArchiveEntity.Seat> seats = new ArrayList<>();
        state.seatByUser().entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> {
            Optional<Placement> placement = result.flatMap(found -> StarfareMatches.account(entry.getKey())
                    .flatMap(found::placementOf));
            seats.add(new GameArchiveEntity.Seat(entry.getKey(), entry.getValue(),
                    placement.map(Placement::place).orElse(null),
                    placement.map(found -> found.departure().name()).orElse(null)));
        });
        entity.list(state.tutorial(), result.map(MatchResult.Aborted.class::isInstance).orElse(false), seats);
    }

    /** An archive that cannot be read: listed without seats, so nobody's list shows it and it is not filled again. */
    public static void unlistable(GameArchiveEntity entity) {
        entity.list(false, false, List.of());
    }

    /**
     * The platform's entry for a listed archive; empty if no seat holder is a platform account. The result names the
     * accounts in seat order, like {@link StarfareMatches#result(GameState)}.
     */
    public static Optional<FinishedMatch> finished(GameId id, String name, Instant endedAt, boolean tutorial,
                                                   boolean aborted, List<GameArchiveEntity.Seat> seats) {
        List<GameArchiveEntity.Seat> bySeat = seats.stream()
                .sorted(Comparator.comparingInt(GameArchiveEntity.Seat::getSeat)).toList();
        Optional<MatchResult> result = aborted ? aborted(bySeat) : ranked(bySeat);
        String title = name.isBlank() ? id.value() : name;
        return result.map(found -> new FinishedMatch(StarfareMatches.GAME_KEY, id.value(), title, endedAt, found,
                tutorial, StarfareMatches.viewPath(id)));
    }

    private static Optional<MatchResult> aborted(List<GameArchiveEntity.Seat> seats) {
        List<AccountId> accounts = seats.stream().map(seat -> StarfareMatches.account(seat.getAccount()))
                .flatMap(Optional::stream).toList();
        return accounts.isEmpty() ? Optional.empty() : Optional.of(MatchResult.aborted(accounts));
    }

    private static Optional<MatchResult> ranked(List<GameArchiveEntity.Seat> seats) {
        List<Placement> placements = seats.stream().flatMap(seat -> placement(seat).stream()).toList();
        return placements.isEmpty() ? Optional.empty() : Optional.of(MatchResult.ranked(placements));
    }

    private static Optional<Placement> placement(GameArchiveEntity.Seat seat) {
        Integer place = seat.getPlace();
        String departure = seat.getDeparture();
        if (place == null) {
            return Optional.empty();
        }
        return StarfareMatches.account(seat.getAccount()).map(account -> new Placement(account, place,
                departure == null ? Departure.NONE : Departure.valueOf(departure)));
    }
}
