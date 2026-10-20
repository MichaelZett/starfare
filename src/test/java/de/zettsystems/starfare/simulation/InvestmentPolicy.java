package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/** All policies share the same military planner; these interventions change allocations only. */
enum InvestmentPolicy {
    SHIPS, EARLY, LIGHT, LATE;

    void plan(GameState state, int player, InvestmentModel model, Map<Integer, Integer> targets) {
        var view = new DefaultPlayerViewBuilder().forPlayer(state, player);
        var owned = new HashSet<Integer>();
        var economy = new de.zettsystems.starfare.economy.application.DefaultEconomyService();
        for (var system : view.systems()) {
            if (!Objects.equals(system.ownerId(), player)) { continue; }
            owned.add(system.id());
            var industry = Objects.requireNonNull(system.industry());
            int target = targets.computeIfAbsent(system.id(), _ -> industry.capacity() + 1);
            boolean threatened = view.systems().stream().anyMatch(contact -> contact.ownerId() != null
                    && !Objects.equals(contact.ownerId(), player) && state.travelRounds(system.id(), contact.id()) <= 2);
            int price = model.cost(industry.capacity(), Objects.requireNonNull(industry.colony()).quality());
            boolean invest = this != SHIPS && !threatened && !industry.atMaximum()
                    && (this != EARLY || state.turn() <= 20 && price <= 10)
                    && (this != LATE || state.turn() >= 40)
                    && (this == LIGHT || industry.capacity() < target);
            int mining = Objects.requireNonNull(industry.metal()).miningPoints();
            int points = invest ? Math.min(industry.usableCapacity() - mining, Math.max(1, industry.usableCapacity() / 4)) : 0;
            if (!economy.allocateExpansion(state, player, system.id(), points)) { throw new IllegalStateException("Invalid investment"); }
        }
        targets.keySet().retainAll(owned);
    }
}
