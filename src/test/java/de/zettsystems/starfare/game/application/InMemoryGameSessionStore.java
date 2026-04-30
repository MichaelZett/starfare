package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGameSessionStore implements GameSessionStore {
    private final ConcurrentHashMap<GameId, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(GameSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public Optional<GameSession> load(GameId id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public List<GameId> listIds() {
        List<GameSession> all = new ArrayList<>(sessions.values());
        all.sort(Comparator.comparing(GameSession::createdAt));
        return all.stream().map(GameSession::id).toList();
    }

    @Override
    public void delete(GameId id) {
        sessions.remove(id);
    }
}
