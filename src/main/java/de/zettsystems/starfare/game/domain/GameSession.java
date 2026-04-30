package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * A single running game session with its own mutable state and lock.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "GameState is intentionally shared with the wrapping session; access is funneled "
                + "through readState()/writeState() and serialized by the ReentrantReadWriteLock owned here.")
public class GameSession {
    private final GameId id;
    private final String name;
    private volatile @Nullable String hostUsername;
    private final Instant createdAt;
    private final GameState state;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public GameSession(GameId id, String name, @Nullable String hostUsername, Instant createdAt) {
        this(id, name, hostUsername, createdAt, new GameState());
    }

    public GameSession(GameId id, String name, @Nullable String hostUsername, Instant createdAt, GameState state) {
        this.id = id;
        this.name = name;
        this.hostUsername = hostUsername;
        this.createdAt = createdAt;
        this.state = state;
    }

    public GameId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public @Nullable String hostUsername() {
        return hostUsername;
    }

    /**
     * Transfers the host role to {@code newHost} (null clears the host slot).
     */
    public void transferHostTo(@Nullable String newHost) {
        this.hostUsername = newHost;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public <T> T readState(Function<GameState, T> fn) {
        lock.readLock().lock();
        try {
            return fn.apply(state);
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T writeState(Function<GameState, T> fn) {
        lock.writeLock().lock();
        try {
            return fn.apply(state);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
