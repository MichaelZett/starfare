package de.zettsystems.starfare.diplomacy.values;

import de.zettsystems.starfare.game.values.Player;
import java.util.List;

/** {@code aiAlliancesAllowed} false: AI empires cannot be offered a new alliance in this game. */
public record DiplomacyView(int turn, int player, boolean canAct, List<Player> players, DiplomacyState treaties,
                            boolean aiAlliancesAllowed) {
    public DiplomacyView { players = List.copyOf(players); }
    public DiplomacyView(int turn, int player, boolean canAct, List<Player> players, DiplomacyState treaties) {
        this(turn, player, canAct, players, treaties, true);
    }
}
