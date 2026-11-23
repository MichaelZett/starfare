package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;

/** Who may read and write a game's chat; the platform shows and stores the chat (MatchDirectory.chatAccess). */
public interface GameChatService {
    /** Whether the account may read the chat: it holds or held a seat in the game. */
    boolean mayRead(GameId gameId, String account);
    /** Whether the account may write now; who may only read sees the history without an input. */
    boolean mayWrite(GameId gameId, String account);
}
