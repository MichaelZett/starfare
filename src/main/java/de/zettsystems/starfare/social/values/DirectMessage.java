package de.zettsystems.starfare.social.values;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A direct message between two users. The social module persists it and broadcasts it live to
 * attached user interfaces.
 */
public record DirectMessage(long id, String from, String to, String text, Instant sentAt, @Nullable Instant readAt) {

    public static final int MAX_LENGTH = 1000;
    public boolean read() { return readAt != null; }
}
