package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
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
    private volatile @Nullable String hostPlayerId;
    private final Instant createdAt;
    private final GameState state;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public GameSession(GameId id, String name, @Nullable String hostPlayerId, Instant createdAt) {
        this(id, name, hostPlayerId, createdAt, new GameState());
    }

    public GameSession(GameId id, String name, @Nullable String hostPlayerId, Instant createdAt, GameState state) {
        this.id = id;
        this.name = name;
        this.hostPlayerId = hostPlayerId;
        this.createdAt = createdAt;
        this.state = state;
    }

    public GameId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public @Nullable String hostPlayerId() {
        return hostPlayerId;
    }

    /**
     * Transfers the host role to {@code newHost} (null clears the host slot).
     */
    public void transferHostTo(@Nullable String newHost) {
        writeState(current -> { this.hostPlayerId = newHost; return null; });
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

    /**
     * Applies a state change and completes the supplied follow-up while the
     * session write lock is still held.
     */
    public <T> T writeStateAndThen(Function<GameState, T> fn, Consumer<GameSession> followUp) {
        lock.writeLock().lock();
        try {
            T result = fn.apply(state);
            followUp.accept(this);
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
