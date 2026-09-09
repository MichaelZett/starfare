package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable lobby-level view of one game: lifecycle flags plus who sits where. Seats are
 * player IDs of the seat holders; {@code joinedHumanSeats} lists the human seats that have
 * actually joined, {@code invitedSeats} the reservations by invited player ID.
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
        GameOutcome outcome
) {

    public boolean belongsTo(String account) {
        return account.equals(hostPlayerId) || seatByPlayer.containsKey(account);
    }

    public boolean canJoin(String account) {
        if (account.isBlank() || gameOver) { return false; }
        Integer seat = seatByPlayer.get(account);
        if (seat != null) {
            return !joinedHumanSeats.contains(seat) && (!started || reentryAllowed);
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
