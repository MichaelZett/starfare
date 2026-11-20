package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.Departure;
import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.gamesplatformapi.MatchStatus;
import de.zettsystems.gamesplatformapi.MatchSummary;
import de.zettsystems.gamesplatformapi.Placement;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.SectorScore;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Maps Starfare's games onto the platform contract (games-platform-api). Starfare names an
 * account by the zs-identity user id as a string; ids that are no positive number (only test
 * fixtures use such names) have no platform account and are left out.
 */
public final class StarfareMatches {

    /** The game key, as in {@code games/starfare/game.properties}. */
    public static final String GAME_KEY = "starfare";

    private StarfareMatches() {
    }

    /** The platform account behind a Starfare account, if it is a zs-identity user id. */
    public static Optional<AccountId> account(String account) {
        try {
            long value = Long.parseLong(account);
            return value > 0 ? Optional.of(AccountId.of(value)) : Optional.empty();
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    /** The Starfare account for a platform account. */
    public static String accountName(AccountId account) {
        return Long.toString(account.value());
    }

    /** The map of a running or finished game. */
    public static String viewPath(GameId id) {
        return "/" + GAME_KEY + "/map/" + URLEncoder.encode(id.value(), StandardCharsets.UTF_8);
    }

    /** The waiting room of a game that has not started: players join there and the host starts it. */
    public static String waitingRoomPath(GameId id) {
        return "/" + GAME_KEY + "/match/" + URLEncoder.encode(id.value(), StandardCharsets.UTF_8);
    }

    /**
     * The platform's view of a game, or empty for a game without a seat for people (computer
     * opponents only). Seats held by a person count as seats for people even after the person
     * left and a computer opponent took over, because the person may come back.
     */
    public static Optional<MatchSummary> summary(GameSummary game) {
        long humanSeats = game.players().stream().filter(player -> humanSeat(game, player)).count();
        if (humanSeats == 0) {
            return Optional.empty();
        }
        int seats = game.players().size();
        MatchStatus status = status(game);
        Map<Integer, AccountId> accounts = accountsBySeat(game.seatByPlayer());
        List<AccountId> toMove = status == MatchStatus.RUNNING ? accounts.entrySet().stream()
                .filter(entry -> game.joinedHumanSeats().contains(entry.getKey())
                        && !game.submittedSeats().contains(entry.getKey()))
                .map(Map.Entry::getValue).toList() : List.of();
        String title = game.name().isBlank() ? game.gameId().value() : game.name();
        return Optional.of(new MatchSummary(GAME_KEY, game.gameId().value(), title, status,
                List.copyOf(accounts.values()), seats, seats - (int) humanSeats, toMove, game.roundStartedAt(),
                status == MatchStatus.OPEN ? waitingRoomPath(game.gameId()) : viewPath(game.gameId())));
    }

    /**
     * The platform's entry for a finished game the account held a seat in, with the given end; empty if the
     * account had no seat or no account of the game is a platform account.
     */
    public static Optional<FinishedMatch> finished(GameId id, String name, Instant endedAt, GameState state,
                                                   String account) {
        if (!state.seatByUser().containsKey(account)) {
            return Optional.empty();
        }
        return result(state).map(result -> new FinishedMatch(GAME_KEY, id.value(), name.isBlank() ? id.value() : name,
                endedAt, result, false, viewPath(id)));
    }

    private static MatchStatus status(GameSummary game) {
        if (game.gameOver()) {
            return MatchStatus.FINISHED;
        }
        return game.started() ? MatchStatus.RUNNING : MatchStatus.OPEN;
    }

    private static boolean humanSeat(GameSummary game, Player player) {
        return !player.ai() || game.seatByPlayer().containsValue(player.id())
                || game.invitedSeats().containsValue(player.id());
    }

    /**
     * The result of a finished game, or empty if no account took part.
     *
     * <p>Winners share place 1. The others follow by the number of systems they hold at the end, or by
     * their {@link SectorScore} when a SectorForces game reached its round limit; computer opponents count, so an account behind a winning computer opponent is on place 2.
     * An account removed for missed deadlines keeps the place it held then ({@link Departure#TIMED_OUT}),
     * so a computer opponent winning with its seat is not its win. An account whose seat a computer
     * opponent took over otherwise (left or removed) has resigned; an account without systems and
     * fleets was eliminated. A game that ended without a winner has no ranking and counts as aborted.
     */
    public static Optional<MatchResult> result(GameState state) {
        Map<Integer, AccountId> accounts = accountsBySeat(state.seatByUser());
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        Set<Integer> winners = Set.copyOf(state.outcome().winnerIds());
        if (winners.isEmpty()) {
            return Optional.of(MatchResult.aborted(List.copyOf(accounts.values())));
        }
        Map<Integer, Long> systems = state.systems().stream().map(StarSystem::ownerId).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(owner -> owner, Collectors.counting()));
        Set<Integer> fleetOwners = state.fleets().stream().map(Fleet::ownerId).collect(Collectors.toSet());
        Map<Integer, Long> standing = state.roundLimitReached() ? scores(state) : systems;
        List<Placement> placements = accounts.entrySet().stream().map(entry -> {
            int seat = entry.getKey();
            Departure departure = departure(state, seat, systems, fleetOwners);
            Integer timedOutPlace = state.timedOutPlaces().get(seat);
            if (departure == Departure.TIMED_OUT && timedOutPlace != null) {
                return new Placement(entry.getValue(), timedOutPlace, departure);
            }
            long better = state.players().stream()
                    .filter(other -> ranksBefore(other.id(), seat, winners, standing)).count();
            return new Placement(entry.getValue(), 1 + (int) better, departure);
        }).toList();
        return Optional.of(MatchResult.ranked(placements));
    }

    private static Map<Integer, Long> scores(GameState state) {
        return state.players().stream().collect(Collectors.toMap(Player::id,
                player -> (long) SectorScore.of(state, player.id()).points()));
    }

    private static boolean ranksBefore(int other, int seat, Set<Integer> winners, Map<Integer, Long> systems) {
        if (winners.contains(seat)) {
            return false;
        }
        return winners.contains(other) || systems.getOrDefault(other, 0L) > systems.getOrDefault(seat, 0L);
    }

    private static Departure departure(GameState state, int seat, Map<Integer, Long> systems,
                                       Set<Integer> fleetOwners) {
        if (state.timedOut(seat)) {
            return Departure.TIMED_OUT;
        }
        if (!state.joinedHumanPlayerIds().contains(seat)) {
            return Departure.RESIGNED;
        }
        return systems.containsKey(seat) || fleetOwners.contains(seat) ? Departure.NONE : Departure.ELIMINATED;
    }

    /** The platform accounts of the given seat holders, in seat order. */
    public static List<AccountId> accounts(List<String> accounts) {
        return accounts.stream().map(StarfareMatches::account).flatMap(Optional::stream).distinct().toList();
    }

    private static Map<Integer, AccountId> accountsBySeat(Map<String, Integer> seatByAccount) {
        Map<Integer, AccountId> bySeat = new TreeMap<>();
        seatByAccount.forEach((name, seat) -> account(name).ifPresent(account -> bySeat.put(seat, account)));
        return bySeat;
    }
}
