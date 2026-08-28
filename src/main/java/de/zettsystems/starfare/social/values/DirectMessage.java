package de.zettsystems.starfare.social.values;

import java.time.Instant;

/**
 * A direct message between two users. The social module persists it and broadcasts it live to
 * attached user interfaces.
 */
public record DirectMessage(String from, String to, String text, Instant sentAt) {

    public static final int MAX_LENGTH = 1000;
}
