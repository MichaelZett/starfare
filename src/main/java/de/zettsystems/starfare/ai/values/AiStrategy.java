package de.zettsystems.starfare.ai.values;

import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.Arrays;
import java.util.List;

/** Selectable planning profiles; BASELINE preserves the historical rule-specific AI. */
public enum AiStrategy {
    BASELINE, RUSH, EXPANSION, CONCENTRATION, DEFENSE, INDUSTRY, INDUSTRY_LIGHT, INDUSTRY_ADAPTIVE;

    public boolean requiresIndustry() {
        return this == INDUSTRY || this == INDUSTRY_LIGHT || this == INDUSTRY_ADAPTIVE;
    }

    public boolean supports(RulesetRef rules) { return !requiresIndustry() || rules.spaceward(); }

    public static List<AiStrategy> forRules(RulesetRef rules) {
        return Arrays.stream(values()).filter(strategy -> strategy.supports(rules)).toList();
    }
}
