package de.zettsystems.starfare.fleet.domain;

import de.zettsystems.starfare.game.domain.GameState;
import org.jspecify.annotations.Nullable;

/** A conquest claim never transfers ships, and cannot nominate an enemy. */
public final class ConquestOrders {
    private ConquestOrders() { }
    public static boolean allowed(GameState state, int player, @Nullable Integer beneficiary) {
        return beneficiary == null || beneficiary == player || state.allied(player, beneficiary);
    }
}
