package de.zettsystems.starfare.economy.domain;

import java.util.Objects;
import de.zettsystems.starfare.economy.values.MetalRules;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure empire-wide forecast, shared by display and resolution. No collection order bias. */
public record MetalProduction(MetalEconomy economy, Map<Integer, Integer> ships,
                              Map<Integer, Long> extracted, Map<Integer, Long> openingStocks,
                              @org.jspecify.annotations.Nullable RecyclingEconomy plannedRecycling) {
    public MetalProduction(MetalEconomy economy, Map<Integer, Integer> ships, Map<Integer, Long> extracted,
                           Map<Integer, Long> openingStocks) { this(economy, ships, extracted, openingStocks, null); }
    public MetalProduction {
        ships = Map.copyOf(ships);
        extracted = Map.copyOf(extracted);
        openingStocks = Map.copyOf(openingStocks);
    }
    public static MetalProduction forPlan(List<StarSystem> systems, Map<Integer, Industry> industries,
                                          Map<Integer, ColonyEconomy> colonies, MetalEconomy current) {
        return forPlan(systems, industries, colonies, current, RulesetRef.SPACEWARD_METALS);
    }
    public static MetalProduction forPlan(List<StarSystem> systems, Map<Integer, Industry> industries,
                                          Map<Integer, ColonyEconomy> colonies, MetalEconomy current, RulesetRef rules) {
        var settled = Recycling.calculate(systems, current);
        var forecast = calculate(systems, industries, colonies, settled.economy(), rules);
        return new MetalProduction(forecast.economy(), forecast.ships(), forecast.extracted(), current.stocks(), current.recycling());
    }
    public static MetalProduction calculate(List<StarSystem> systems, Map<Integer, Industry> industries,
                                            Map<Integer, ColonyEconomy> colonies, MetalEconomy current) {
        return calculate(systems, industries, colonies, current, RulesetRef.SPACEWARD_METALS);
    }
    public static MetalProduction calculate(List<StarSystem> systems, Map<Integer, Industry> industries,
                                            Map<Integer, ColonyEconomy> colonies, MetalEconomy current, RulesetRef rules) {
        var deposits = new HashMap<>(current.deposits());
        var stocks = new HashMap<>(current.stocks());
        Map<Integer, Long> extracted = new HashMap<>();
        Map<Integer, List<StarSystem>> empires = new HashMap<>();
        Map<Integer, Integer> desired = new HashMap<>();
        for (StarSystem system : systems) {
            Integer owner = system.ownerId();
            if (owner == null || system.neutral()) { continue; }
            int mining = Objects.requireNonNull(current.mining().get(system.id()));
            int capacity = Objects.requireNonNull(colonies.get(system.id())).shipbuilding(Objects.requireNonNull(industries.get(system.id())), rules.limitsIndustryByLabor());
            if (mining > capacity) { throw new IllegalArgumentException("Mining exceeds available capacity"); }
            long amount = Objects.requireNonNull(deposits.get(system.id())).extraction(mining);
            deposits.put(system.id(), Objects.requireNonNull(deposits.get(system.id())).extract(amount));
            stocks.compute(owner, (_, stock) -> Math.addExact(Objects.requireNonNull(stock), amount));
            extracted.put(system.id(), amount);
            desired.put(system.id(), capacity - mining);
            empires.computeIfAbsent(owner, _ -> new ArrayList<>()).add(system);
        }
        Map<Integer, Integer> ships = new HashMap<>();
        empires.forEach((owner, owned) -> distribute(owner, owned, desired, stocks, ships, rules.sharesMetalEqually()));
        return new MetalProduction(new MetalEconomy(deposits, stocks, current.mining(), current.recycling()), ships, extracted, current.stocks());
    }
    private static void distribute(int owner, List<StarSystem> owned, Map<Integer, Integer> desired,
                                   Map<Integer, Long> stocks, Map<Integer, Integer> ships, boolean equally) {
        int total = owned.stream().mapToInt(system -> Objects.requireNonNull(desired.get(system.id()))).sum();
        long stock = Objects.requireNonNull(stocks.get(owner));
        int buildable = (int) Math.min(total, stock / MetalRules.SHIP_COST);
        if (total == 0) { owned.forEach(system -> ships.put(system.id(), 0)); return; }
        if (equally) {
            distributeEqually(owned, desired, ships, buildable);
            stocks.put(owner, stock - (long) buildable * MetalRules.SHIP_COST);
            return;
        }
        for (StarSystem system : owned) { ships.put(system.id(), buildable * Objects.requireNonNull(desired.get(system.id())) / total); }
        int remainder = buildable - owned.stream().mapToInt(system -> Objects.requireNonNull(ships.get(system.id()))).sum();
        var ranked = owned.stream().sorted(Comparator
                .comparingInt((StarSystem system) -> buildable * Objects.requireNonNull(desired.get(system.id())) % total).reversed()
                .thenComparingInt(StarSystem::id)).toList();
        for (int index = 0; index < remainder; index++) { ships.merge(ranked.get(index).id(), 1, Integer::sum); }
        stocks.put(owner, stock - (long) buildable * MetalRules.SHIP_COST);
    }

    /** Equal shares grow together; satisfied and inactive yards leave their share to the rest. */
    private static void distributeEqually(List<StarSystem> owned, Map<Integer, Integer> desired,
                                         Map<Integer, Integer> ships, int buildable) {
        var active = owned.stream().filter(system -> Objects.requireNonNull(desired.get(system.id())) > 0)
                .sorted(Comparator.comparingInt(StarSystem::id)).toList();
        owned.forEach(system -> ships.put(system.id(), 0));
        int remaining = buildable;
        while (remaining > 0) {
            for (var system : active) {
                int id = system.id();
                if (Objects.requireNonNull(ships.get(id)) >= Objects.requireNonNull(desired.get(id))) { continue; }
                ships.compute(id, (_, count) -> Objects.requireNonNull(count) + 1);
                if (--remaining == 0) { break; }
            }
        }
    }
}
