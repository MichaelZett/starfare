package de.zettsystems.starfare.game.values;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record GameOutcome(@Nullable Integer winnerId, @Nullable Instant finishedAt) {}
