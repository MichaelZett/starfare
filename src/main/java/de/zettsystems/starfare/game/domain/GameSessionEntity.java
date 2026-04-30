package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "game_sessions")
public class GameSessionEntity extends AbstractBaseEntity<String> {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "host_username", length = 30)
    private @Nullable String hostUsername;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "state_json", nullable = false, columnDefinition = "TEXT")
    private String stateJson;

    @SuppressWarnings("NullAway.Init")
    protected GameSessionEntity() {}

    @SuppressWarnings("NullAway.Init")
    public GameSessionEntity(String id, String name, @Nullable String hostUsername, Instant createdAt, String stateJson) {
        this.id = id;
        this.name = name;
        this.hostUsername = hostUsername;
        this.createdAt = createdAt;
        this.stateJson = stateJson;
    }

    @Override
    public String getId() { return id; }
    public String getName() { return name; }

    public @Nullable String getHostUsername() {
        return hostUsername;
    }
    public Instant getCreatedAt() { return createdAt; }
    public String getStateJson() { return stateJson; }

    /**
     * Replaces the persisted JSON snapshot with a fresh serialization of the live state.
     */
    public void replaceSnapshot(String stateJson) {
        this.stateJson = stateJson;
    }
}
