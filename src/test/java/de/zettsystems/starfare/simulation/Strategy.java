package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.List;

enum Strategy {
    BASELINE, RUSH, EXPANSION, CONCENTRATION, DEFENSE, INDUSTRY, INDUSTRY_LIGHT, INDUSTRY_ADAPTIVE, CAUTIOUS;

    static List<Strategy> forRules(RulesetRef rules) {
        return List.of(BASELINE, RUSH, EXPANSION, CONCENTRATION, DEFENSE, rules.isSpaceward() ? INDUSTRY : CAUTIOUS);
    }

    boolean requiresIndustry() {
        return this == INDUSTRY || this == INDUSTRY_LIGHT || this == INDUSTRY_ADAPTIVE;
    }
}
