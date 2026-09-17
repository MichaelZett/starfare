package de.zettsystems.starfare.social.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "direct_messages")
public class DirectMessageEntity extends AbstractBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "direct_message_seq")
    @SequenceGenerator(name = "direct_message_seq", sequenceName = "direct_message_seq", allocationSize = 50)
    private Long id;

    @Column(name = "sender_player_id", nullable = false, length = 30)
    private String senderPlayerId;

    @Column(name = "recipient_player_id", nullable = false, length = 30)
    private String recipientPlayerId;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
    @Column(name = "read_at") private @Nullable Instant readAt;
    @Column(name = "sender_archived_at") private @Nullable Instant senderArchivedAt;
    @Column(name = "recipient_archived_at") private @Nullable Instant recipientArchivedAt;

    @SuppressWarnings("NullAway.Init")
    protected DirectMessageEntity() {
    }

    @SuppressWarnings("NullAway.Init")
    public DirectMessageEntity(String senderPlayerId, String recipientPlayerId, String text, Instant sentAt) {
        this.senderPlayerId = senderPlayerId;
        this.recipientPlayerId = recipientPlayerId;
        this.text = text;
        this.sentAt = sentAt;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getSenderPlayerId() {
        return senderPlayerId;
    }

    public String getRecipientPlayerId() {
        return recipientPlayerId;
    }

    public String getText() {
        return text;
    }

    public Instant getSentAt() {
        return sentAt;
    }
    public @Nullable Instant getReadAt() { return readAt; }
}
