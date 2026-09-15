package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/** Immutable, independently retained snapshot of a completed game. */
@Entity
@Table(name = "game_archives")
public class GameArchiveEntity extends AbstractBaseEntity<String> {
    @Id @Column(nullable = false, length = 36) private String id;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "host_player_id", length = 30) private @Nullable String hostPlayerId;
    @Column(name = "finished_at", nullable = false) private Instant finishedAt;
    @Column(name = "state_json", nullable = false, columnDefinition = "TEXT") private String stateJson;

    @SuppressWarnings("NullAway.Init") protected GameArchiveEntity() {}

    @SuppressWarnings("NullAway.Init")
    public GameArchiveEntity(String id, String name, @Nullable String hostPlayerId, Instant finishedAt, String stateJson) {
        this.id = id; this.name = name; this.hostPlayerId = hostPlayerId; this.finishedAt = finishedAt; this.stateJson = stateJson;
    }
    @Override public String getId() { return id; }
    public String getName() { return name; }
    public @Nullable String getHostPlayerId() { return hostPlayerId; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getStateJson() { return stateJson; }
    public void replaceSnapshot(String json) { stateJson = json; }
}
