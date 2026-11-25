package de.zettsystems.starfare.economy.values;

/** What each share turns its points into next round, read from an authorized forecast. */
public record ProductionOutcome(int shipPoints, int ships, int expansionPoints, int progress, int nextProgress,
                                int cost, boolean atMaximum) {

    public static ProductionOutcome of(AllocationSplit allocation, IndustryView forecast) {
        int next = Math.min(forecast.expansionCost(), forecast.expansionProgress() + allocation.expansion());
        return new ProductionOutcome(allocation.ships(), forecast.shipbuilding(), allocation.expansion(),
                forecast.expansionProgress(), next, forecast.expansionCost(), forecast.atMaximum());
    }
}
