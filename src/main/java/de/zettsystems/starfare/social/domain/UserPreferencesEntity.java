package de.zettsystems.starfare.social.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import de.zettsystems.starfare.social.values.Visibility;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_preferences")
public class UserPreferencesEntity extends AbstractBaseEntity<String> {

    @Id
    @Column(name = "player_id", nullable = false, length = 30)
    private String playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 16)
    private Visibility visibility;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @SuppressWarnings("NullAway.Init")
    protected UserPreferencesEntity() {
    }

    @SuppressWarnings("NullAway.Init")
    public UserPreferencesEntity(String playerId, Visibility visibility, Instant updatedAt) {
        this.playerId = playerId;
        this.visibility = visibility;
        this.updatedAt = updatedAt;
    }

    @Override
    public String getId() {
        return playerId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Updates visibility and bumps the audit timestamp atomically.
     */
    public void changeVisibility(Visibility visibility, Instant now) {
        this.visibility = visibility;
        this.updatedAt = now;
    }
}
