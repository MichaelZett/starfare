package de.zettsystems.starfare.social.values;

import java.time.Instant;

/**
 * One ephemeral direct message between two online users. Not persisted; only live inside the
 * UI components of the sender and receiver while their sessions are attached.
 */
public record DirectMessage(String from, String to, String text, Instant sentAt) {

    public static final int MAX_LENGTH = 1000;
}
