package de.zettsystems.starfare.testsupport;

import de.zettsystems.starfare.economy.domain.MetalDeposit;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.HashMap;

public final class RecyclingStates {
    private RecyclingStates() { }
    public static GameState ready() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_RECYCLING, original -> {
            var deposits = new HashMap<>(original.deposits());
            deposits.replaceAll((_, deposit) -> new MetalDeposit(0, deposit.yieldPerPoint()));
            var stocks = new HashMap<>(original.stocks()); stocks.replaceAll((_, _) -> 0L);
            return new MetalEconomy(deposits, stocks, original.mining());
        });
        state.updateSystem(1, system -> system.reserveGarrison(0));
        return state;
    }
    public static GameState running() { var state = ready(); state.start(); return state; }
}
