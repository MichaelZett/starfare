package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;
import java.time.Instant;
import java.util.List;

/** Frozen winners; winnerId remains the legacy individual-victory accessor. */
public record GameOutcome(@Nullable Integer winnerId, @Nullable Instant finishedAt,
                          @Nullable List<Integer> winnerIds, @Nullable Integer allianceId) {
    public GameOutcome(@Nullable Integer winnerId, @Nullable Instant finishedAt) {
        this(winnerId, finishedAt, winnerId == null ? List.of() : List.of(winnerId), null);
    }
    public GameOutcome {
        winnerIds = winnerIds == null ? (winnerId == null ? List.of() : List.of(winnerId))
                : winnerIds.stream().distinct().sorted().toList();
        if (allianceId != null && (winnerId != null || winnerIds.size() < 2)) {
            throw new IllegalArgumentException("Group victory requires multiple winners");
        }
        if (allianceId == null && !winnerIds.equals(winnerId == null ? List.of() : List.of(winnerId))) {
            throw new IllegalArgumentException("Individual outcome and winners disagree");
        }
    }
    @Override public List<Integer> winnerIds() { return java.util.Objects.requireNonNull(winnerIds); }
    public boolean hasWinner() { return !winnerIds().isEmpty(); }
    public boolean wonBy(@Nullable Integer player) { return player != null && winnerIds().contains(player); }
    public boolean allianceVictory() { return allianceId != null; }
}
