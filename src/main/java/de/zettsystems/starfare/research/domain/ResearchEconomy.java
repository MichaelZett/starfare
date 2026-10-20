package de.zettsystems.starfare.research.domain;

import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.research.values.ShipResearch;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Persisted with the economy and its replay frames; allocation is empire-wide, rounded once. */
public record ResearchEconomy(Map<Integer, ShipResearch> empires) {
    public ResearchEconomy { empires = Map.copyOf(empires); }
    public static ResearchEconomy establish(List<Player> players) {
        var empires = new HashMap<Integer, ShipResearch>();
        players.forEach(player -> empires.put(player.id(), ShipResearch.INITIAL));
        return new ResearchEconomy(empires);
    }
    public ResearchEconomy allocate(int player, int percent) {
        var next = new HashMap<>(empires);
        next.put(player, Objects.requireNonNull(next.get(player)).allocate(percent));
        return new ResearchEconomy(next);
    }
    public Map<Integer, Integer> reservations(List<StarSystem> systems, Map<Integer, Industry> industries) {
        var points = new HashMap<Integer, Integer>();
        systems.forEach(system -> points.put(system.id(), 0));
        empires.forEach((owner, research) -> {
            var owned = systems.stream().filter(s -> !s.neutral() && owner.equals(s.ownerId()))
                    .sorted(java.util.Comparator.comparingInt(StarSystem::id)).toList();
            int rate = research.level() == ShipResearch.MAX_LEVEL ? 0 : research.percent();
            int total = owned.stream().mapToInt(s -> Objects.requireNonNull(industries.get(s.id())).capacity()).sum();
            owned.forEach(s -> points.put(s.id(), Objects.requireNonNull(industries.get(s.id())).capacity() * rate / 100));
            int remainder = (int) ((long) total * rate / 100) - owned.stream().mapToInt(s -> Objects.requireNonNull(points.get(s.id()))).sum();
            var ranked = owned.stream().sorted(java.util.Comparator
                    .comparingInt((StarSystem s) -> Objects.requireNonNull(industries.get(s.id())).capacity() * rate % 100)
                    .reversed().thenComparingInt(StarSystem::id)).toList();
            for (int i = 0; i < remainder; i++) { points.merge(ranked.get(i).id(), 1, Integer::sum); }
        });
        return Map.copyOf(points);
    }
    public ResearchEconomy advance(List<StarSystem> systems, Map<Integer, Integer> reservations) {
        var next = new HashMap<>(empires);
        empires.forEach((owner, research) -> next.put(owner, research.advance(systems.stream()
                .filter(s -> !s.neutral() && owner.equals(s.ownerId()))
                .mapToLong(s -> reservations.getOrDefault(s.id(), 0)).sum())));
        return new ResearchEconomy(next);
    }
}
