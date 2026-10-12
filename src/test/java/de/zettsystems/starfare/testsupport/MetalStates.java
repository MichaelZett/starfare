package de.zettsystems.starfare.testsupport;

import de.zettsystems.starfare.economy.domain.MetalDeposit;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.HashMap;

public final class MetalStates {
    private MetalStates() { }
    public static GameState ready(long stock, long remaining) {
        return ColonyStates.ready(RulesetRef.SPACEWARD_METALS, original -> {
            var stocks = new HashMap<>(original.stocks());
            stocks.put(1, stock);
            var deposits = new HashMap<>(original.deposits());
            deposits.put(1, new MetalDeposit(remaining, 3));
            return new MetalEconomy(deposits, stocks, original.mining());
        });
    }
    public static GameState running(long stock, long remaining) {
        var state = ready(stock, remaining); state.start(); return state;
    }
}
