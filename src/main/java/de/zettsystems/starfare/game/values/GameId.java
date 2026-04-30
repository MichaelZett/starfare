package de.zettsystems.starfare.game.values;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier for a game session. Opaque string value (UUID under the hood), stable for the lifetime
 * of the session. Use {@link #newId()} to create one and {@link #of(String)} to parse.
 */
public record GameId(String value) {
    public GameId {
        Objects.requireNonNull(value, "GameId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("GameId value must not be blank");
        }
    }

    public static GameId newId() {
        return new GameId(UUID.randomUUID().toString());
    }

    public static GameId of(String raw) {
        return new GameId(raw);
    }
}
