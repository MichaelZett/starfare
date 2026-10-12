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
        StarSystem system = state.systems().stream().filter(candidate -> candidate.id() == systemId).findFirst().orElse(null);
        Industry industry = state.industries().get(systemId);
        if (system == null || system.neutral() || !Objects.equals(system.ownerId(), playerId)
                || industry == null || points > state.usableIndustrialCapacity(systemId)
                || points + state.metals().map(metal -> metal.mining().get(systemId)).orElse(0) > state.usableIndustrialCapacity(systemId)
                || (industry.capacity() == EconomyRules.MAX_CAPACITY && points != 0)) { return false; }
        state.planIndustrialExpansion(systemId, industry.allocateExpansion(points));
        return true;
    }

    @Override
    public boolean allocateProduction(GameState state, int playerId, int systemId, int expansion, int mining) {
        if (!state.ruleset().usesMetals() || expansion < 0 || mining < 0) { return false; }
        StarSystem system = state.systems().stream().filter(candidate -> candidate.id() == systemId).findFirst().orElse(null);
        Industry industry = state.industries().get(systemId);
        if (system == null || system.neutral() || !Objects.equals(system.ownerId(), playerId) || industry == null
                || (long) expansion + mining > state.usableIndustrialCapacity(systemId)
                || (industry.capacity() == EconomyRules.MAX_CAPACITY && expansion != 0)) { return false; }
        state.planMining(systemId, 0);
        state.planIndustrialExpansion(systemId, industry.allocateExpansion(expansion));
        state.planMining(systemId, mining);
        return true;
    }

    @Override
    public List<IndustrialProduction> produce(GameState state, Map<Integer, Integer> routedShips) {
        if (!state.ruleset().isSpaceward()) {
            throw new IllegalArgumentException("Industrial production requires Spaceward rules");
        }
        if (state.ruleset().usesMetals()) {
            if (!routedShips.isEmpty()) { throw new IllegalArgumentException("W2 production must precede relocations"); }
            if (state.ruleset().usesRecycling() && Objects.requireNonNull(state.metals().orElseThrow().recycling()).orders().values().stream().anyMatch(ships -> ships != 0)) {
                throw new IllegalStateException("Recycling must settle before production");
            }
            var forecast = state.metalPreview();
            var results = state.systems().stream().filter(system -> system.ownerId() != null && !system.neutral())
                    .map(system -> new IndustrialProduction(Objects.requireNonNull(system.ownerId()), system.id(), system.name(),
                            java.util.Objects.requireNonNull(forecast.ships().get(system.id())))).toList();
            state.completeMetalProduction(forecast);
            return results;
        }
        List<IndustrialProduction> results = new ArrayList<>();
        for (StarSystem system : List.copyOf(state.systems())) {
            Integer ownerId = system.ownerId();
            if (ownerId == null || system.neutral()) { continue; }
            Industry current = state.industries().get(system.id());
            if (current == null) { throw new IllegalStateException("Missing industry for system " + system.id()); }
            int shipped = routedShips.getOrDefault(system.id(), 0);
            int produced = system.productionPerTurn();
            state.completeIndustrialProduction(system.id(), current.expandForOneTurn(), shipped);
            results.add(new IndustrialProduction(ownerId, system.id(), system.name(), produced));
        }
        return List.copyOf(results);
    }
}
