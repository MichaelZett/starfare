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

    @Override
    public void doAiTurns(GameState state, FleetService fleetService) {
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
        var base = owned.stream().max(Comparator.comparingInt(StarSystem::garrison)).orElseThrow();
        int available = base.garrison() - Math.max(1, base.productionPerTurn());
        if (available <= 0) {
            return;
        }
        state.systems().stream()
                .filter(s -> !Objects.equals(s.ownerId(), p.id()))
                .min(Comparator.comparingDouble(s -> state.distance(base.id(), s.id())))
                .ifPresent(t -> fleetService.sendFleet(state, p.id(), base.id(), t.id(), Math.max(1, available / 2)));
    }
}
