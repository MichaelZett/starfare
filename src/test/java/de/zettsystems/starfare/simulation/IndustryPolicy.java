package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.economy.values.IndustryView;

final class IndustryPolicy {
    private IndustryPolicy() { }
    static int allocation(Strategy strategy, IndustryView industry, int enemyRounds, int availableShips) {
        return de.zettsystems.starfare.ai.domain.IndustryPolicy.allocation(
                AiStrategy.valueOf(strategy.name()), industry, enemyRounds, availableShips);
    }
}
