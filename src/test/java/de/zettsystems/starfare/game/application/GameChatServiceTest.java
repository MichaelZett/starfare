package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameChatMessage;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class GameChatServiceTest extends AbstractIntegrationTest {
    @Autowired
    private GameChatService chat;

    @Test
    void onlyParticipantsCanReadAndSendGameChatMessages() {
        GameId id = registry.createGame(GameSetup.defaults(), "alice", "chat");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.seatByUser().put("alice", 1);
            state.seatByUser().put("bob", 2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            state.start();
            return null;
        });

        assertThat(chat.send(id, "alice", "  hello team  ")).isTrue();
        assertThat(chat.send(id, "eve", "sneak in")).isFalse();
        assertThat(chat.messagesFor(id, "bob")).extracting(GameChatMessage::text).containsExactly("hello team");
        assertThat(chat.messagesFor(id, "eve")).isEmpty();
    }
}
