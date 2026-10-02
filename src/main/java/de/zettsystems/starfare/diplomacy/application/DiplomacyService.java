package de.zettsystems.starfare.diplomacy.application;

import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.game.domain.GameState;

public interface DiplomacyService {
    boolean act(GameState state, int player, DiplomacyOrder order);
    void beginRound(GameState state, int turn);
    void answerAiProposals(GameState state);
}
