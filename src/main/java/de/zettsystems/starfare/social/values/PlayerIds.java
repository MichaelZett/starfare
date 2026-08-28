package de.zettsystems.starfare.social.values;

import org.jspecify.annotations.Nullable;

/**
 * Player-id canonicalization for the social module: trim and treat empty/blank input as "no player".
 */
public final class PlayerIds {

    private PlayerIds() {
    }

    /**
     * Returns the trimmed form, or {@code null} if {@code playerId} is null/blank.
     */
    public static @Nullable String normalize(@Nullable String playerId) {
        if (playerId == null) {
            return null;
        }
        String t = playerId.trim();
        return t.isEmpty() ? null : t;
    }
}
