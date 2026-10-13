package de.zettsystems.starfare.economy.values;

/**
 * What each production function turns its points into next round, read from an authorized
 * forecast. Points, yield per point and the resulting ships, metal or progress stay separate.
 */
public record ProductionOutcome(int shipPoints, int plannedShips, int ships,
                                int miningPoints, int yieldPerPoint, long metal, boolean depositLimited,
                                int expansionPoints, int progress, int nextProgress, int cost, boolean atMaximum) {

    public static ProductionOutcome of(AllocationSplit allocation, IndustryView forecast) {
        MetalView resources = forecast.metal();
        int planned = resources == null ? allocation.ships() : resources.plannedShips();
        int yield = resources == null ? 0 : resources.yieldPerPoint();
        long metal = resources == null ? 0 : resources.extraction();
        boolean limited = resources != null && allocation.mining() > 0 && metal < (long) allocation.mining() * yield;
        int next = Math.min(forecast.expansionCost(), forecast.expansionProgress() + allocation.expansion());
        return new ProductionOutcome(allocation.ships(), planned, forecast.shipbuilding(),
                allocation.mining(), yield, metal, limited,
                allocation.expansion(), forecast.expansionProgress(), next, forecast.expansionCost(), forecast.atMaximum());
    }

    /** Metal shortage builds fewer ships than the points request. */
    public boolean shipsShort() { return ships < plannedShips; }
}
