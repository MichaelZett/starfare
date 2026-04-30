package de.zettsystems.starfare.social.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import de.zettsystems.starfare.social.values.FriendshipStatus;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_a", "user_b"}))
public class FriendshipEntity extends AbstractBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "friendship_seq")
    @SequenceGenerator(name = "friendship_seq", sequenceName = "friendship_seq", allocationSize = 50)
    private Long id;

    @Column(name = "user_a", nullable = false, length = 30)
    private String userA;

    @Column(name = "user_b", nullable = false, length = 30)
    private String userB;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FriendshipStatus status;

    @Column(name = "requested_by", length = 30)
    private @Nullable String requestedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @SuppressWarnings("NullAway.Init")
    protected FriendshipEntity() {
    }

    @SuppressWarnings("NullAway.Init")
    public FriendshipEntity(String userA, String userB, FriendshipStatus status, @Nullable String requestedBy,
                            Instant now) {
        this.userA = userA;
        this.userB = userB;
        this.status = status;
        this.requestedBy = requestedBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getUserA() {
        return userA;
    }

    public String getUserB() {
        return userB;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public @Nullable String getRequestedBy() {
        return requestedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Accepts a pending friend request: clears the requester and flips status to ACCEPTED.
     */
    public void accept(Instant now) {
        this.status = FriendshipStatus.ACCEPTED;
        this.requestedBy = null;
        this.updatedAt = now;
    }

    /**
     * Marks the relationship as blocked by {@code blocker}.
     */
    public void block(String blocker, Instant now) {
        this.status = FriendshipStatus.BLOCKED;
        this.requestedBy = blocker;
        this.updatedAt = now;
    }
}
