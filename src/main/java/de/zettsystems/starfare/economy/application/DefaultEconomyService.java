package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.economy.values.EconomyRules;
import de.zettsystems.starfare.economy.values.IndustrialProduction;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DefaultEconomyService implements EconomyService {
    @Override
    public boolean allocateExpansion(GameState state, int playerId, int systemId, int points) {
        if (!state.ruleset().isSpaceward() || points < 0) { return false; }
        StarSystem system = state.findSystem(systemId).orElse(null);
        Industry industry = state.industries().get(systemId);
        if (system == null || system.neutral() || !Objects.equals(system.ownerId(), playerId)
                || industry == null || points > industry.capacity()
                || (industry.capacity() == EconomyRules.MAX_CAPACITY && points != 0)) { return false; }
        state.planIndustrialExpansion(systemId, industry.allocateExpansion(points));
        return true;
    }

    @Override
    public List<IndustrialProduction> produce(GameState state, Map<Integer, Integer> routedShips) {
        if (!state.ruleset().isSpaceward()) {
            throw new IllegalArgumentException("Industrial production requires the extended rules");
        }
        List<IndustrialProduction> results = new ArrayList<>();
        for (StarSystem system : List.copyOf(state.systems())) {
            Integer ownerId = system.ownerId();
            if (ownerId == null || system.neutral()) { continue; }
            Industry current = state.industries().get(system.id());
            if (current == null) { throw new IllegalStateException("Missing industry for system " + system.id()); }
            int shipped = routedShips.getOrDefault(system.id(), 0);
            int produced = system.productionPerTurn();
            Industry expanded = current.expandForOneTurn();
            state.completeIndustrialProduction(system.id(), expanded, shipped);
            results.add(new IndustrialProduction(ownerId, system.id(), system.name(), produced,
                    expanded.capacity() - current.capacity()));
        }
        return List.copyOf(results);
    }
}
