package de.zettsystems.starfare.game.values;

/** Fixed victory and diplomacy choices for one game. */
public record VictoryRules(int individualSystemPercent, boolean alliancesAllowed,
                           boolean allianceVictoryAllowed, int allianceSystemPercent) {
    public VictoryRules {
        individualSystemPercent = Math.clamp(individualSystemPercent, GameConfig.MIN_VICTORY_SYSTEM_PERCENT, 100);
        allianceSystemPercent = Math.clamp(allianceSystemPercent, GameConfig.MIN_VICTORY_SYSTEM_PERCENT, 100);
        allianceVictoryAllowed = alliancesAllowed && allianceVictoryAllowed;
    }
    public static VictoryRules defaults(RulesetRef rules, int individualPercent) {
        return new VictoryRules(individualPercent, rules.spaceward(), false, GameConfig.VICTORY_SYSTEM_PERCENT);
    }
    public VictoryRules forRuleset(RulesetRef rules) {
        if (!rules.spaceward()) { return defaults(rules, individualSystemPercent); }
        if (rules.equals(RulesetRef.SPACEWARD) && !equals(defaults(rules, individualSystemPercent))) {
            throw new IllegalArgumentException("Custom diplomacy requires Spaceward 1.1.0");
        }
        return this;
    }
    public VictoryRules forFullConquest() {
        return new VictoryRules(100, alliancesAllowed, allianceVictoryAllowed, 100);
    }
}
