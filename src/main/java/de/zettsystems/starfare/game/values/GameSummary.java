package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable lobby-level view of one game: lifecycle flags plus who sits where. Seats are
 * player IDs of the seat holders; {@code joinedHumanSeats} lists the human seats that have
 * actually joined, {@code invitedSeats} the reservations by invited player ID.
 * {@code submittedSeats} are the seats that have submitted the current round, and
 * {@code roundStartedAt} is when the current round began (creation time while the game is open).
 * {@code closedSeats} are seats nobody may take back: resigned or timed out.
 * {@code roundDeadline} is when the current round ends at the latest, {@code null} without a deadline
 * (fewer than two people playing, not started or over); {@code tempo} is the tempo chosen at creation,
 * {@code null} for a game without one; {@code tutorial} whether it was created as a tutorial.
 * {@code pendingAnswers} lists per seat the treaty proposals (by id, ascending) that still wait for that
 * seat's vote; empty without diplomacy.
 */
public record GameSummary(
        GameId gameId,
        String name,
        @Nullable String hostPlayerId,
        int turn,
        boolean started,
        boolean gameOver,
        boolean observersAllowed,
        boolean reentryAllowed,
        List<Player> players,
        Set<Integer> joinedHumanSeats,
        Map<String, Integer> seatByPlayer,
        Map<String, Integer> invitedSeats,
        GameVisibility visibility,
        GameOutcome outcome,
        RulesetRef ruleset,
        @Nullable VictoryRules victoryRules,
        Set<Integer> submittedSeats,
        Instant roundStartedAt,
        Set<Integer> closedSeats,
        @Nullable Instant roundDeadline,
        @Nullable GameTempo tempo,
        boolean tutorial,
        Map<Integer, List<Integer>> pendingAnswers) {
    public GameSummary {
        submittedSeats = Set.copyOf(submittedSeats);
        closedSeats = Set.copyOf(closedSeats);
        pendingAnswers = pendingAnswers.entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().sorted().toList()));
    }

    /** A summary without open treaty proposals. */
    public GameSummary(
        GameId gameId,
        String name,
        @Nullable String hostPlayerId,
        int turn,
        boolean started,
        boolean gameOver,
        boolean observersAllowed,
        boolean reentryAllowed,
        List<Player> players,
        Set<Integer> joinedHumanSeats,
        Map<String, Integer> seatByPlayer,
        Map<String, Integer> invitedSeats,
        GameVisibility visibility,
        GameOutcome outcome,
        RulesetRef ruleset,
        @Nullable VictoryRules victoryRules,
        Set<Integer> submittedSeats,
        Instant roundStartedAt,
        Set<Integer> closedSeats,
        @Nullable Instant roundDeadline,
        @Nullable GameTempo tempo,
        boolean tutorial) {
        this(gameId, name, hostPlayerId, turn, started, gameOver, observersAllowed, reentryAllowed, players,
                joinedHumanSeats, seatByPlayer, invitedSeats, visibility, outcome, ruleset, victoryRules, submittedSeats,
                roundStartedAt, closedSeats, roundDeadline, tempo, tutorial, Map.of());
    }

    /** A summary without a round deadline and tempo. */
    public GameSummary(
        GameId gameId,
        String name,
        @Nullable String hostPlayerId,
        int turn,
        boolean started,
        boolean gameOver,
        boolean observersAllowed,
        boolean reentryAllowed,
        List<Player> players,
        Set<Integer> joinedHumanSeats,
        Map<String, Integer> seatByPlayer,
        Map<String, Integer> invitedSeats,
        GameVisibility visibility,
        GameOutcome outcome,
        RulesetRef ruleset,
        @Nullable VictoryRules victoryRules,
        Set<Integer> submittedSeats,
        Instant roundStartedAt,
        Set<Integer> closedSeats) {
        this(gameId, name, hostPlayerId, turn, started, gameOver, observersAllowed, reentryAllowed, players,
                joinedHumanSeats, seatByPlayer, invitedSeats, visibility, outcome, ruleset, victoryRules, submittedSeats,
                roundStartedAt, closedSeats, null, null, false);
    }

    public GameSummary(
        GameId gameId,
        String name,
        @Nullable String hostPlayerId,
        int turn,
        boolean started,
        boolean gameOver,
        boolean observersAllowed,
        boolean reentryAllowed,
        List<Player> players,
        Set<Integer> joinedHumanSeats,
        Map<String, Integer> seatByPlayer,
        Map<String, Integer> invitedSeats,
        GameVisibility visibility,
        GameOutcome outcome,
        RulesetRef ruleset,
        @Nullable VictoryRules victoryRules) {
        this(gameId, name, hostPlayerId, turn, started, gameOver, observersAllowed, reentryAllowed, players,
                joinedHumanSeats, seatByPlayer, invitedSeats, visibility, outcome, ruleset, victoryRules, Set.of(),
                Instant.EPOCH, Set.of());
    }

    public GameSummary(
        GameId gameId,
        String name,
        @Nullable String hostPlayerId,
        int turn,
        boolean started,
        boolean gameOver,
        boolean observersAllowed,
        boolean reentryAllowed,
        List<Player> players,
        Set<Integer> joinedHumanSeats,
        Map<String, Integer> seatByPlayer,
        Map<String, Integer> invitedSeats,
        GameVisibility visibility,
        GameOutcome outcome,
        RulesetRef ruleset
) {
        this(gameId, name, hostPlayerId, turn, started, gameOver, observersAllowed, reentryAllowed, players, joinedHumanSeats, seatByPlayer, invitedSeats, visibility, outcome, ruleset, null);
    }

    public GameSummary(GameId gameId,
            String name,
            @Nullable String hostPlayerId,
            int turn,
            boolean started,
            boolean gameOver,
            boolean observersAllowed,
            boolean reentryAllowed,
            List<Player> players,
            Set<Integer> joinedHumanSeats,
            Map<String, Integer> seatByPlayer,
            Map<String, Integer> invitedSeats,
            GameVisibility visibility,
            GameOutcome outcome) {
        this(gameId, name, hostPlayerId, turn, started, gameOver, observersAllowed, reentryAllowed, players,
                joinedHumanSeats, seatByPlayer, invitedSeats, visibility, outcome, RulesetRef.SECTOR_FORCES);
    }


    @Override public VictoryRules victoryRules() {
        return victoryRules == null ? VictoryRules.defaults(ruleset, GameConfig.VICTORY_SYSTEM_PERCENT) : victoryRules;
    }

    public boolean belongsTo(String account) {
        return account.equals(hostPlayerId) || seatByPlayer.containsKey(account);
    }

    public boolean canJoin(String account) {
        if (account.isBlank() || gameOver) { return false; }
        Integer seat = seatByPlayer.get(account);
        if (seat != null) {
            return !joinedHumanSeats.contains(seat) && !closedSeats.contains(seat) && (!started || reentryAllowed);
        }
        return !started && (invitedSeats.containsKey(account)
                || ((visibility == GameVisibility.PUBLIC || account.equals(hostPlayerId)) && hasOpenHumanSeat()));
    }

    public boolean allHumansJoined() {
        return players.stream()
                .filter(p -> !p.ai())
                .allMatch(p -> joinedHumanSeats.contains(p.id()));
    }

    public boolean hasOpenHumanSeat() {
        return players.stream()
                .filter(p -> !p.ai())
                .anyMatch(p -> !joinedHumanSeats.contains(p.id()) && !invitedSeats.containsValue(p.id()));
    }
}
