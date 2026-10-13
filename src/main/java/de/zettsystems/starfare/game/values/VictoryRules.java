package de.zettsystems.starfare.game.values;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Fixed victory and diplomacy choices for one game. {@code aiAlliancesAllowed} decides whether
 * AI empires may newly found or join alliances; it only takes effect while alliances are allowed.
 */
public record VictoryRules(int individualSystemPercent, boolean alliancesAllowed,
                           boolean allianceVictoryAllowed, int allianceSystemPercent, boolean aiAlliancesAllowed) {
    public VictoryRules {
        individualSystemPercent = Math.clamp(individualSystemPercent, GameConfig.MIN_VICTORY_SYSTEM_PERCENT, 100);
        allianceSystemPercent = Math.clamp(allianceSystemPercent, GameConfig.MIN_VICTORY_SYSTEM_PERCENT, 100);
        allianceVictoryAllowed = alliancesAllowed && allianceVictoryAllowed;
        aiAlliancesAllowed = alliancesAllowed && aiAlliancesAllowed;
    }
    /** Previous behaviour: AI may take part whenever alliances are allowed. */
    public VictoryRules(int individualSystemPercent, boolean alliancesAllowed,
                        boolean allianceVictoryAllowed, int allianceSystemPercent) {
        this(individualSystemPercent, alliancesAllowed, allianceVictoryAllowed, allianceSystemPercent, true);
    }

    /** Stored games without the AI choice keep their previous permitted AI participation. */
    @JsonCreator
    public static VictoryRules of(@JsonProperty("individualSystemPercent") int individualSystemPercent,
                                  @JsonProperty("alliancesAllowed") boolean alliancesAllowed,
                                  @JsonProperty("allianceVictoryAllowed") boolean allianceVictoryAllowed,
                                  @JsonProperty("allianceSystemPercent") int allianceSystemPercent,
                                  @JsonProperty("aiAlliancesAllowed") @Nullable Boolean aiAlliancesAllowed) {
        return new VictoryRules(individualSystemPercent, alliancesAllowed, allianceVictoryAllowed, allianceSystemPercent,
                aiAlliancesAllowed == null || aiAlliancesAllowed);
    }

    public static VictoryRules defaults(RulesetRef rules, int individualPercent) {
        return new VictoryRules(individualPercent, rules.isSpaceward(), false, GameConfig.VICTORY_SYSTEM_PERCENT);
    }
    public VictoryRules forRuleset(RulesetRef rules) {
        if (!rules.isSpaceward()) { return defaults(rules, individualSystemPercent); }
        if (rules.equals(RulesetRef.SPACEWARD) && !equals(defaults(rules, individualSystemPercent))) {
            throw new IllegalArgumentException("Custom diplomacy requires Spaceward 1.1.0");
        }
        return this;
    }
    public VictoryRules forFullConquest() {
        return new VictoryRules(100, alliancesAllowed, allianceVictoryAllowed, 100, aiAlliancesAllowed);
    }
    public VictoryRules withIndividualPercent(int percent) {
        return new VictoryRules(percent, alliancesAllowed, allianceVictoryAllowed, allianceSystemPercent, aiAlliancesAllowed);
    }
}
