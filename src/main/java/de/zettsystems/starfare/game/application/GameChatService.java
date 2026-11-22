package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;

import java.time.Instant;

/**
 * Who may read and write a game's chat; the platform shows and stores the chat (MatchDirectory.chatAccess). The
 * former own chat only keeps its old messages until the table is dropped.
 */
public interface GameChatService {
    /** Whether the account may read the chat: it holds or held a seat in the game. */
    boolean mayRead(GameId gameId, String account);
    /** Whether the account may write now; who may only read sees the history without an input. */
    boolean mayWrite(GameId gameId, String account);
    /** Deletes old messages of the former own chat. */
    long removeMessagesOlderThan(Instant cutoff);
}
