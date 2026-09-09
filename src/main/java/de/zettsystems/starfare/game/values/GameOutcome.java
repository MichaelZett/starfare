package de.zettsystems.starfare.game.values;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
public record GameOutcome(@Nullable Integer winnerId, @Nullable Instant finishedAt) {}
