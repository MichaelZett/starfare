package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.economy.values.IndustrialProduction;
import de.zettsystems.starfare.game.domain.GameState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shipbuilding stays real; only next-round industry is replaced at the round boundary. */
final class InvestmentEconomy extends DefaultEconomyService {
    private final InvestmentModel model;
    private final Map<Integer, Industry> grown = new HashMap<>();
    private final Map<Integer, Integer> owners = new HashMap<>();
    final Map<Integer, Integer> built = new HashMap<>();
    final Map<Integer, Integer> acquired = new HashMap<>();
    InvestmentEconomy(InvestmentModel model) { this.model = model; }

    @Override public List<IndustrialProduction> produce(GameState state, Map<Integer, Integer> routed) {
        grown.clear(); owners.clear(); built.clear(); acquired.clear();
        for (var system : state.systems()) {
            owners.put(system.id(), system.ownerId() == null ? 0 : system.ownerId());
            if (system.neutral()) { continue; }
            var before = state.industries().get(system.id());
            var after = model.grow(before, state.colonies().get(system.id()).quality());
            grown.put(system.id(), after);
            built.merge(Objects.requireNonNull(system.ownerId()), after.capacity() - before.capacity(), Integer::sum);
        }
        return super.produce(state, routed).stream().map(result -> new IndustrialProduction(
                result.ownerId(), result.systemId(), result.systemName(), result.ships(), result.metalMined(), result.metalConsumed(),
                grown.get(result.systemId()).capacity() - state.industries().get(result.systemId()).capacity() + result.capacityGained())).toList();
    }

    GameState finish(GameState state) {
        var industries = new HashMap<>(state.industries());
        for (var system : state.systems()) {
            var desired = grown.get(system.id());
            if (desired != null) {
                int allocation = industries.get(system.id()).expansionAllocation();
                industries.put(system.id(), new Industry(desired.capacity(), desired.capacity() == 50 ? 0 : allocation, desired.expansionProgress()));
            }
            Integer owner = system.ownerId();
            if (owner != null && owner != owners.get(system.id()).intValue()) {
                var captured = industries.get(system.id());
                if (model == InvestmentModel.HALF_CAPTURE) {
                    captured = Industry.establish(Math.max(1, Math.ceilDiv(captured.capacity(), 2)));
                    industries.put(system.id(), captured);
                }
                acquired.merge(owner, captured.capacity(), Integer::sum);
            }
        }
        state.replayFrames().clear();
        return industries.equals(state.industries()) ? state : InvestmentSnapshots.industry(state, industries);
    }
}
