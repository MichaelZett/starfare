package de.zettsystems.starfare.economy.domain;

import java.util.Objects;
import de.zettsystems.starfare.economy.values.MetalRules;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Explicit creation of resources from saved colony geology; never used during restoration. */
public final class MetalGenesis {
    private MetalGenesis() { }
    public static MetalEconomy forGalaxy(List<StarSystem> systems, List<Player> players,
                                         Map<Integer, ColonyEconomy> colonies) {
        Map<Integer, MetalDeposit> deposits = new HashMap<>();
        Map<Integer, Integer> mining = new HashMap<>();
        Map<Integer, Long> stocks = new HashMap<>();
        for (StarSystem system : systems) {
            int richness = Objects.requireNonNull(colonies.get(system.id())).quality().metalRichness();
            deposits.put(system.id(), new MetalDeposit((long) richness * MetalRules.DEPOSIT_PER_RICHNESS,
                    richness * MetalRules.YIELD_PER_RICHNESS));
            mining.put(system.id(), 0);
        }
        players.forEach(player -> stocks.put(player.id(), (long) MetalRules.START_STOCK));
        return new MetalEconomy(deposits, stocks, mining);
    }
}
