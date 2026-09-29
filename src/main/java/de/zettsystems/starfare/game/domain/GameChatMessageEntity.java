package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "game_chat_messages")
public class GameChatMessageEntity extends AbstractBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_chat_message_seq")
    @SequenceGenerator(name = "game_chat_message_seq", sequenceName = "game_chat_message_seq", allocationSize = 50)
    private Long id;

    @Column(name = "game_id", nullable = false, length = 36)
    private String gameId;
    @Column(name = "sender_player_id", nullable = false, length = 30)
    private String senderPlayerId;
    @Column(nullable = false, length = 1000)
    private String text;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @SuppressWarnings("NullAway.Init")
    protected GameChatMessageEntity() {}

    @SuppressWarnings("NullAway.Init")
    public GameChatMessageEntity(String gameId, String senderPlayerId, String text, Instant sentAt) {
        this.gameId = gameId;
        this.senderPlayerId = senderPlayerId;
        this.text = text;
        this.sentAt = sentAt;
    }

    @Override public Long getId() { return id; }
    public String getGameId() { return gameId; }
    public String getSenderPlayerId() { return senderPlayerId; }
    public String getText() { return text; }
    public Instant getSentAt() { return sentAt; }
}
