package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/** Eine vom Spieler ausgewertete Schlacht: Event {@code eventIndex} im Bericht der Runde {@code turn}. */
@Entity
@Table(name = "battle_acknowledgements")
public class BattleAcknowledgementEntity extends AbstractBaseEntity<BattleAcknowledgementEntity.Key> {

    @EmbeddedId
    private Key id;

    @Column(name = "acknowledged_at", nullable = false)
    private Instant acknowledgedAt;

    @SuppressWarnings("NullAway.Init")
    protected BattleAcknowledgementEntity() {
    }

    public BattleAcknowledgementEntity(Key id, Instant acknowledgedAt) {
        this.id = id;
        this.acknowledgedAt = acknowledgedAt;
    }

    @Override
    public Key getId() {
        return id;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    @Embeddable
    public record Key(
            @Column(name = "player_id", nullable = false, length = 30) String playerId,
            @Column(name = "game_id", nullable = false, length = 36) String gameId,
            @Column(name = "turn", nullable = false) int turn,
            @Column(name = "event_index", nullable = false) int eventIndex) {
    }
}
