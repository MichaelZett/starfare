package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.List;

enum Strategy {
    BASELINE, RUSH, EXPANSION, CONCENTRATION, DEFENSE, INDUSTRY;

    static List<Strategy> forRules(RulesetRef rules) {
        return rules.spaceward() ? List.of(values())
                : List.of(BASELINE, RUSH, EXPANSION, CONCENTRATION, DEFENSE);
    }
}
