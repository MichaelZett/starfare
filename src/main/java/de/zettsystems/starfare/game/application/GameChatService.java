package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameChatMessage;
import de.zettsystems.starfare.game.values.GameId;

import java.util.List;
import java.time.Instant;

public interface GameChatService {
    List<GameChatMessage> messagesFor(GameId gameId, String account);
    boolean send(GameId gameId, String account, String text);
    long removeMessagesOlderThan(Instant cutoff);
}
