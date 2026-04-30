package de.zettsystems.starfare.social.values;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Username canonicalization for the social module: trim, lowercase (ROOT locale),
 * and treat empty/blank input as "no user".
 */
public final class Usernames {

    private Usernames() {
    }

    /**
     * Returns the canonical lowercase form, or {@code null} if {@code name} is null/blank.
     */
    public static @Nullable String normalize(@Nullable String name) {
        if (name == null) {
            return null;
        }
        String t = name.trim().toLowerCase(Locale.ROOT);
        return t.isEmpty() ? null : t;
    }
}
