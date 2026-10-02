package de.zettsystems.starfare.diplomacy.values;

import de.zettsystems.starfare.game.values.Player;
import java.util.List;

public record DiplomacyView(int turn, int player, boolean canAct, List<Player> players, DiplomacyState treaties) {
    public DiplomacyView { players = List.copyOf(players); }
}
