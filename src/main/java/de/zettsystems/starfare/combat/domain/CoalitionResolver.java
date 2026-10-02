package de.zettsystems.starfare.combat.domain;

import de.zettsystems.starfare.report.values.CoalitionSide;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

/** Simultaneous proportional fire. All damage in a volley uses the same starting forces. */
public final class CoalitionResolver {
    private CoalitionResolver() { }

    public static List<CoalitionSide> resolve(List<CoalitionSide> forces, int percent, IntToDoubleFunction roll) {
        var sorted = forces.stream().sorted(java.util.Comparator.comparingInt(CoalitionSide::id))
                .map(side -> new CoalitionSide(side.id(), side.strength(), side.members().stream()
                        .sorted(java.util.Comparator.comparingInt(CoalitionSide.Member::playerId)).toList())).toList();
        validate(sorted);
        Map<Integer, Integer> remaining = new LinkedHashMap<>();
        Map<Integer, Double> factors = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Double>> damage = new LinkedHashMap<>();
        double variation = Math.clamp(percent, 0, 30) / 100.0;
        for (var side : sorted) {
            remaining.put(side.id(), side.ships());
            double draw = roll.applyAsDouble(side.id());
            if (!Double.isFinite(draw)) { throw new IllegalArgumentException("Combat rolls must be finite"); }
            factors.put(side.id(), 1 - variation + 2 * variation * Math.clamp(draw, 0, 1));
            damage.put(side.id(), new LinkedHashMap<>());
        }
        while (remaining.values().stream().filter(ships -> ships > 0).count() > 1) {
            remaining = volley(remaining, factors, damage);
        }
        Map<Integer, Integer> destroyed = destroyed(sorted, remaining, damage);
        List<CoalitionSide> result = new ArrayList<>();
        for (var side : sorted) {
            var weights = memberWeights(side);
            var survivors = ProportionalAllocation.distribute(java.util.Objects.requireNonNull(remaining.get(side.id())), weights);
            var kills = ProportionalAllocation.distribute(destroyed.getOrDefault(side.id(), 0), weights);
            result.add(new CoalitionSide(side.id(), side.ships() * java.util.Objects.requireNonNull(factors.get(side.id())), side.members().stream()
                    .map(member -> new CoalitionSide.Member(member.playerId(), member.name(), member.ships(),
                            java.util.Objects.requireNonNull(survivors.get(member.playerId())), java.util.Objects.requireNonNull(kills.get(member.playerId())))).toList()));
        }
        return List.copyOf(result);
    }

    private static void validate(List<CoalitionSide> sides) {
        var players = sides.stream().flatMap(side -> side.members().stream()).map(CoalitionSide.Member::playerId).toList();
        if (sides.stream().map(CoalitionSide::id).distinct().count() != sides.size()
                || players.stream().distinct().count() != players.size() || sides.stream().anyMatch(side -> side.members().isEmpty())) {
            throw new IllegalArgumentException("Each force belongs to exactly one combat side");
        }
    }

    private static Map<Integer, Integer> volley(Map<Integer, Integer> ships, Map<Integer, Double> factors,
                                               Map<Integer, Map<Integer, Double>> damage) {
        Map<Integer, Double> strengths = new LinkedHashMap<>();
        ships.forEach((id, count) -> strengths.put(id, count * java.util.Objects.requireNonNull(factors.get(id))));
        double total = strengths.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<Integer, Integer> next = new LinkedHashMap<>();
        for (var target : strengths.entrySet()) {
            double incoming = 0;
            for (var source : strengths.entrySet()) {
                if (source.getKey().equals(target.getKey()) || source.getValue() == 0 || target.getValue() == 0) { continue; }
                double fire = source.getValue() * target.getValue() / (total - source.getValue());
                incoming += fire;
                java.util.Objects.requireNonNull(damage.get(target.getKey())).merge(source.getKey(), fire, Double::sum);
            }
            int count = java.util.Objects.requireNonNull(ships.get(target.getKey()));
            int lost = count == 0 ? 0 : Math.clamp((long) Math.ceil(count * incoming / target.getValue()), 0, count);
            next.put(target.getKey(), count - lost);
        }
        return next;
    }

    private static Map<Integer, Double> memberWeights(CoalitionSide side) {
        Map<Integer, Double> weights = new LinkedHashMap<>();
        side.members().forEach(member -> weights.put(member.playerId(), (double) member.ships()));
        return weights;
    }

    private static Map<Integer, Integer> destroyed(List<CoalitionSide> initial, Map<Integer, Integer> remaining,
                                                  Map<Integer, Map<Integer, Double>> damage) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (var target : initial) {
            var shares = ProportionalAllocation.distribute(target.ships() - java.util.Objects.requireNonNull(remaining.get(target.id())), java.util.Objects.requireNonNull(damage.get(target.id())));
            shares.forEach((id, lost) -> result.merge(id, lost, Integer::sum));
        }
        return result;
    }
}
