package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.economy.values.IndustrialProduction;
import de.zettsystems.starfare.game.domain.GameState;
import java.util.List;
import java.util.Map;

public interface EconomyService {
    boolean allocateExpansion(GameState state, int playerId, int systemId, int points);
    List<IndustrialProduction> produce(GameState state, Map<Integer, Integer> routedShips);
}
