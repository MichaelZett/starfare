package de.zettsystems.starfare.game.values;

import java.time.Instant;

/** A persisted message addressed to all current participants of a game. */
public record GameChatMessage(GameId gameId, String senderPlayerId, String text, Instant sentAt) {

    public static final int MAX_LENGTH = 1000;
}
