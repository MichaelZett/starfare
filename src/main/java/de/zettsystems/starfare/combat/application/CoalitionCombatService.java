package de.zettsystems.starfare.combat.application;

import de.zettsystems.starfare.game.domain.GameState;

public interface CoalitionCombatService {
    void resolveArrivals(GameState state);
}
