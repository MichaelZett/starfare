package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

/**
 * Client-facing view of a star system with fog-of-war data.
 * Ownership/garrison/production/color/lastSeen are null when unknown due to fog-of-war.
 */
public record VisibleSystem(
        int id, String name, double x, double y,
        @Nullable Integer ownerId, @Nullable Integer garrison, @Nullable Integer productionPerTurn,
        boolean fullyVisible, @Nullable String colorHex, @Nullable Integer lastSeenTurn
) { }
