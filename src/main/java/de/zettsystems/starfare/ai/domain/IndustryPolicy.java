package de.zettsystems.starfare.ai.domain;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.economy.values.IndustryView;

/** Industry allocations based only on the owning player's visible situation. */
public final class IndustryPolicy {
    private IndustryPolicy() { }

    public static int allocation(AiStrategy strategy, IndustryView industry, int enemyRounds, int availableShips) {
        if (industry.atMaximum() || enemyRounds <= 2) { return 0; }
        if (strategy == AiStrategy.INDUSTRY_LIGHT) { return industry.usableCapacity() / 4; }
        if (strategy != AiStrategy.INDUSTRY_ADAPTIVE) { throw new IllegalArgumentException("Expected an adaptive industry profile"); }
        if (availableShips < industry.usableCapacity() * 2 || industry.reserveShortfall() > 0) { return 0; }
        int points = enemyRounds <= 4 ? industry.usableCapacity() / 4 : industry.usableCapacity() / 2;
        return Math.min(points, industry.expansionCost() - industry.expansionProgress());
    }
}
