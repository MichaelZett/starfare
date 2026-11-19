package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable lobby-level view of one game: lifecycle flags plus who sits where. Seats are
 * player IDs of the seat holders; {@code joinedHumanSeats} lists the human seats that have
 * actually joined, {@code invitedSeats} the reservations by invited player ID.
 * {@code submittedSeats} are the seats that have submitted the current round, and
 * {@code roundStartedAt} is when the current round began (creation time while the game is open).
 * {@code closedSeats} are seats nobody may take back: resigned or timed out.
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
        java.time.Instant roundStartedAt,
        Set<Integer> closedSeats) {
    public GameSummary {
        submittedSeats = Set.copyOf(submittedSeats);
        closedSeats = Set.copyOf(closedSeats);
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
                java.time.Instant.EPOCH, Set.of());
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
