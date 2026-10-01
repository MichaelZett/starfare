package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected registry is shared by design; access stays behind its session locks.")
public class DefaultGameTemplateService implements GameTemplateService {
    private final GameRegistry registry;
    private final GameArchiveStore archives;
    private final GameAccessPolicy access;
    private final Broadcaster broadcaster;

    public DefaultGameTemplateService(GameRegistry registry, GameArchiveStore archives,
                                      GameAccessPolicy access, Broadcaster broadcaster) {
        this.registry = registry;
        this.archives = archives;
        this.access = access;
        this.broadcaster = broadcaster;
    }

    @Override
    public Optional<GameTemplate> template(GameId source, String account) {
        return registry.find(source).flatMap(session -> registry.readState(source,
                state -> template(state, session.hostPlayerId(), account)))
                .or(() -> archives.load(source).flatMap(archive -> template(archive.state(), archive.hostPlayerId(), account)));
    }

    private Optional<GameTemplate> template(GameState state, @Nullable String host, String account) {
        if (account.isBlank() || !access.related(state, host, account)) { return Optional.empty(); }
        return state.originalSetup().map(setup -> new GameTemplate(setup, state.seatByUser()));
    }

    @Override
    public Optional<GameId> create(GameId source, String account, String name, boolean rematch) {
        if (name.isBlank() || name.length() > 100) { return Optional.empty(); }
        return template(source, account).map(template -> {
            GameId id = registry.createGame(template.setup(), account, name.strip());
            if (rematch) {
                registry.writeState(id, state -> {
                    template.participants().forEach((participant, seat) -> {
                        if (state.originalHumanPlayerIds().contains(seat)) { state.invitedSeats().put(participant, seat); }
                    });
                    return null;
                });
            }
            broadcaster.publish(new GameEvent.GameCreated(id));
            return id;
        });
    }
}
