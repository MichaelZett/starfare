package de.zettsystems.starfare.combat.application;

import de.zettsystems.starfare.game.domain.GameState;

import java.util.List;

public interface CombatService {
    void resolveAttack(GameState state, int attackerId, int toSystemId, int ships, List<Integer> localNos);
}
