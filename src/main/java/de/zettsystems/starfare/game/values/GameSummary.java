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
        Map<String, Integer> invitedSeats
) {

    public boolean allHumansJoined() {
        return players.stream()
                .filter(p -> !p.ai())
                .allMatch(p -> joinedHumanSeats.contains(p.id()));
    }

    public boolean hasOpenHumanSeat() {
        return players.stream()
                .filter(p -> !p.ai())
                .anyMatch(p -> !joinedHumanSeats.contains(p.id()));
    }
}
