package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * The first end of a game. Its row keeps a game the winner continued from ending a second time;
 * results and statistics belong to the platform, which learns them from {@code MatchEnded}.
 */
@Entity
@Table(name = "game_results")
public class GameEndEntity extends AbstractBaseEntity<String> {

    @Id
    @Column(name = "game_id", nullable = false, length = 36)
    private String gameId;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @SuppressWarnings("NullAway.Init")
    protected GameEndEntity() {
    }

    public GameEndEntity(String gameId, Instant finishedAt) {
        this.gameId = gameId;
        this.finishedAt = finishedAt;
    }

    @Override
    public String getId() {
        return gameId;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
