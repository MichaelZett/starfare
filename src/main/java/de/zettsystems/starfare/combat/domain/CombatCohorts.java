package de.zettsystems.starfare.combat.domain;

import de.zettsystems.starfare.research.values.ShipCohorts;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Reconstructs exactly the member/contingent/generation survivors used after each volley. */
public record CombatCohorts(List<Force> forces) {
    public CombatCohorts { forces = List.copyOf(forces); }
    public record Force(int side, int player, int contingent, ShipCohorts ships) { }
    public double strength(int side, int survivors) {
        var present = forces.stream().filter(force -> force.side() == side).toList();
        Map<Integer, Double> members = new TreeMap<>();
        present.forEach(force -> members.merge(force.player(), (double) force.ships().count(), Double::sum));
        var memberSurvivors = ProportionalAllocation.distribute(survivors, members);
        long strength = 0;
        for (var player : members.keySet()) {
            var contingents = present.stream().filter(force -> force.player() == player).toList();
            Map<Integer, Double> weights = new TreeMap<>();
            contingents.forEach(force -> weights.put(force.contingent(), (double) force.ships().count()));
            var remaining = ProportionalAllocation.distribute(Objects.requireNonNull(memberSurvivors.get(player)), weights);
            for (var force : contingents) {
                strength += force.ships().survive(Objects.requireNonNull(remaining.get(force.contingent()))).strengthTenths();
            }
        }
        return strength / 10.0;
    }
}
