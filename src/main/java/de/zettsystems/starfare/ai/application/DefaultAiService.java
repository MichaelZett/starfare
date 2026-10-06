package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DefaultAiService implements AiService {
    private final ProfilePlanning profiles;

    public DefaultAiService() {
        this(new DefaultProfilePlanning(new de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder(),
                new de.zettsystems.starfare.economy.application.DefaultEconomyService()));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultAiService(ProfilePlanning profiles) { this.profiles = profiles; }


    @Override
    public void planAiTurns(GameState state, FleetService fleetService) {
        Map<Integer, List<StarSystem>> byOwner = state.systems().stream()
                .filter(s -> s.ownerId() != null)
                .collect(Collectors.groupingBy(StarSystem::ownerId));

        for (Player p : state.players()) {
            doAiTurn(state, fleetService, p, byOwner.getOrDefault(p.id(), List.of()));
        }
    }

    private void doAiTurn(GameState state, FleetService fleetService, Player p, List<StarSystem> owned) {
        if (!p.ai() || owned.isEmpty()) {
            return;
        }
        if (p.aiStrategy() != de.zettsystems.starfare.ai.values.AiStrategy.BASELINE) {
            profiles.plan(state, fleetService, p);
            return;
        }
        var base = owned.stream().max(Comparator.comparingInt(StarSystem::garrison)).orElseThrow();
        int available = base.garrison() - Math.max(1, base.productionPerTurn());
        if (available <= 0) {
            return;
        }
        state.systems().stream()
                .filter(s -> !Objects.equals(s.ownerId(), p.id()))
                .min(Comparator.comparingDouble(s -> state.distance(base.id(), s.id())))
                .ifPresent(t -> fleetService.queueSend(state, p.id(), base.id(), t.id(), Math.max(1, available / 2)));
    }
}
