package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Durable compact result retained after the detailed game session is removed. */
@Entity
@Table(name = "game_results")
public class GameResultEntity extends AbstractBaseEntity<String> {

    @Id
    @Column(name = "game_id", nullable = false, length = 36)
    private String gameId;

    @Column(name = "game_name", nullable = false, length = 200)
    private String gameName;

    @Column(name = "winner_player_id", length = 30)
    private @Nullable String winnerPlayerId;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "game_result_participants", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "player_id", nullable = false, length = 30)
    private Set<String> participants = new LinkedHashSet<>();

    @SuppressWarnings("NullAway.Init")
    protected GameResultEntity() {
    }

    public GameResultEntity(String gameId, String gameName, @Nullable String winnerPlayerId,
                            Instant finishedAt, Set<String> participants) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.winnerPlayerId = winnerPlayerId;
        this.finishedAt = finishedAt;
        this.participants.addAll(participants);
    }

    @Override
    public String getId() {
        return gameId;
    }

    public @Nullable String getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public Set<String> getParticipants() {
        return Set.copyOf(participants);
    }
}
