package de.zettsystems.starfare.combat.domain;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Largest remainders, with ascending IDs resolving exact ties. */
public final class ProportionalAllocation {
    private ProportionalAllocation() { }
    public static Map<Integer, Integer> distribute(int total, Map<Integer, Double> weights) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        weights.keySet().stream().sorted().forEach(id -> result.put(id,
                sum > 0 ? (int) Math.floor(total * java.util.Objects.requireNonNull(weights.get(id)) / sum) : 0));
        int left = total - result.values().stream().mapToInt(Integer::intValue).sum();
        if (sum <= 0 || left <= 0) { return result; }
        var order = weights.keySet().stream().sorted(Comparator
                .comparingDouble((Integer id) -> total * java.util.Objects.requireNonNull(weights.get(id)) / sum - java.util.Objects.requireNonNull(result.get(id))).reversed()
                .thenComparingInt(Integer::intValue)).toList();
        for (int i = 0; i < left; i++) { result.merge(order.get(i % order.size()), 1, Integer::sum); }
        return result;
    }
}
