package de.zettsystems.starfare.social.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "direct_messages")
public final class DirectMessageEntity extends AbstractBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "direct_message_seq")
    @SequenceGenerator(name = "direct_message_seq", sequenceName = "direct_message_seq", allocationSize = 50)
    private Long id;

    @Column(name = "sender_player_id", nullable = false, length = 30)
    private String senderUsername;

    @Column(name = "recipient_player_id", nullable = false, length = 30)
    private String recipientUsername;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @SuppressWarnings("NullAway.Init")
    protected DirectMessageEntity() {
    }

    @SuppressWarnings("NullAway.Init")
    public DirectMessageEntity(String senderUsername, String recipientUsername, String text, Instant sentAt) {
        this.senderUsername = senderUsername;
        this.recipientUsername = recipientUsername;
        this.text = text;
        this.sentAt = sentAt;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getText() {
        return text;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
