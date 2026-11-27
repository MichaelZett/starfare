package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, independently retained snapshot of a completed game. Besides the snapshot it keeps what a list of
 * finished games shows (tutorial, aborted, the seats with place and departure), so that listing reads no snapshot.
 * An archive stored before these columns existed is not {@linkplain #isListed() listed} until it is filled.
 */
@Entity
@Table(name = "game_archives")
public class GameArchiveEntity extends AbstractBaseEntity<String> {
    @Id @Column(nullable = false, length = 36) private String id;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "host_player_id", length = 30) private @Nullable String hostPlayerId;
    @Column(name = "finished_at", nullable = false) private Instant finishedAt;
    @Column(name = "state_json", nullable = false, columnDefinition = "TEXT") private String stateJson;
    @Column(nullable = false) private boolean tutorial;
    @Column(nullable = false) private boolean aborted;
    @Column(nullable = false) private boolean listed;
    @ElementCollection
    @CollectionTable(name = "game_archive_seats", joinColumns = @JoinColumn(name = "archive_id"))
    private List<Seat> seats = new ArrayList<>();

    @SuppressWarnings("NullAway.Init") protected GameArchiveEntity() {}

    /** An archive that is not listed yet: {@link #list} fills what the list of finished games shows. */
    @SuppressWarnings("NullAway.Init")
    public GameArchiveEntity(String id, String name, @Nullable String hostPlayerId, Instant finishedAt, String stateJson) {
        this.id = id; this.name = name; this.hostPlayerId = hostPlayerId; this.finishedAt = finishedAt; this.stateJson = stateJson;
    }
    @Override public String getId() { return id; }
    public String getName() { return name; }
    public @Nullable String getHostPlayerId() { return hostPlayerId; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getStateJson() { return stateJson; }
    public boolean isTutorial() { return tutorial; }
    public boolean isAborted() { return aborted; }
    public boolean isListed() { return listed; }
    public List<Seat> getSeats() { return List.copyOf(seats); }
    public void transferHostTo(@Nullable String host) { hostPlayerId = host; }
    public void replaceSnapshot(String json) { stateJson = json; }

    /** Replaces what the list of finished games shows; no seats for a game nobody can list. */
    public void list(boolean tutorial, boolean aborted, List<Seat> seats) {
        this.tutorial = tutorial;
        this.aborted = aborted;
        this.seats.clear();
        this.seats.addAll(seats);
        this.listed = true;
    }

    /**
     * An account that held a seat. A ranked game gives each platform account its place and departure (the name of
     * a {@code de.zettsystems.gamesplatformapi.Departure}); other accounts and aborted games have neither.
     */
    @Embeddable
    public static class Seat {
        @Column(nullable = false, length = 30) private String account;
        @Column(nullable = false) private int seat;
        @Column private @Nullable Integer place;
        @Column(length = 20) private @Nullable String departure;

        @SuppressWarnings("NullAway.Init") protected Seat() {}

        public Seat(String account, int seat, @Nullable Integer place, @Nullable String departure) {
            this.account = account; this.seat = seat; this.place = place; this.departure = departure;
        }
        public String getAccount() { return account; }
        public int getSeat() { return seat; }
        public @Nullable Integer getPlace() { return place; }
        public @Nullable String getDeparture() { return departure; }
    }
}
