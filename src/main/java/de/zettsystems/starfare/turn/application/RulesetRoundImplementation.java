package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.game.values.RulesetRef;

/** A registered round implementation for one immutable rule version. */
public interface RulesetRoundImplementation extends TurnEngine {
    RulesetRef ruleset();
}
