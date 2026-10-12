package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.economy.domain.*;
import de.zettsystems.starfare.economy.values.SystemQuality;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.SystemComposition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Paired maps: change geology alone, or suitability alone, without moving stars or homes. */
final class EconomyMaps {
    enum Environment { NATURAL, POOR, RICH, HARSH, FERTILE, EXHAUSTED }
    private EconomyMaps() { }

    static Map<Integer, ColonyEconomy> colonies(List<StarSystem> systems, long seed, Environment environment) {
        var natural = ColonyGenesis.forGalaxy(systems, seed);
        if (environment == Environment.NATURAL) { return natural; }
        var result = new HashMap<Integer, ColonyEconomy>();
        for (var system : systems) {
            int richness = switch (environment) { case POOR -> 1; case RICH -> 5; default -> 3; };
            var atmosphere = environment == Environment.HARSH ? SystemQuality.Atmosphere.NONE : SystemQuality.Atmosphere.TEMPERATE;
            var quality = new SystemQuality(SystemComposition.StarType.YELLOW, 1, 1,
                    SystemComposition.BodyType.ROCKY, false, atmosphere, richness);
            result.put(system.id(), new ColonyEconomy(quality, natural.get(system.id()).colony()));
        }
        return Map.copyOf(result);
    }

    static MetalEconomy resources(GameState state, Environment environment) {
        var original = MetalGenesis.forGalaxy(state.systems(), state.players(), state.colonies());
        if (environment != Environment.EXHAUSTED) { return original; }
        var deposits = new HashMap<>(original.deposits());
        deposits.replaceAll((_, deposit) -> new MetalDeposit(0, deposit.yieldPerPoint()));
        var stocks = new HashMap<>(original.stocks());
        stocks.replaceAll((_, _) -> 0L);
        return new MetalEconomy(deposits, stocks, original.mining());
    }
}
