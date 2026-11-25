package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.ai.values.AiStrategy;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * How strong the computer empires of a game are: their planning profile and their start compared with the
 * people. The values are starting points; the strategy bench calibrates them (K4).
 */
public enum ComputerStrength {
    EASY(ComputerStrength.EASY_START_SHARE),
    NORMAL(ComputerStrength.NORMAL_START_SHARE),
    HARD(ComputerStrength.HARD_START_SHARE);

    public static final ComputerStrength DEFAULT = NORMAL;
    /** Start production and garrison of a computer empire relative to a person's. */
    static final double EASY_START_SHARE = 0.75;
    static final double NORMAL_START_SHARE = 1.0;
    static final double HARD_START_SHARE = 1.25;
    /** Normal computers each draw one of these, hidden from the players. */
    static final List<AiStrategy> NORMAL_PROFILES =
            List.of(AiStrategy.RUSH, AiStrategy.EXPANSION, AiStrategy.CONCENTRATION, AiStrategy.DEFENSE);
    /** Best classic profiles of the strategy bench in a duel; with more empires the baseline won most. */
    static final List<AiStrategy> HARD_CLASSIC_DUEL_PROFILES = List.of(AiStrategy.RUSH, AiStrategy.EXPANSION);
    /** Best extended profiles of the bench: adaptive industry led duels, with seven empires it was level with the baseline. */
    static final List<AiStrategy> HARD_EXTENDED_PROFILES = List.of(AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.BASELINE);

    private final double startShare;

    ComputerStrength(double startShare) {
        this.startShare = startShare;
    }

    /** A computer's start value derived from a person's, never below one. */
    public int computerStart(int humanValue) {
        return Math.max(1, (int) Math.round(humanValue * startShare));
    }

    /**
     * Planning profile of one computer empire.
     *
     * @param players every empire of the game, people included; a duel has two
     */
    public AiStrategy strategy(RulesetRef rules, int players, RandomGenerator random) {
        return switch (this) {
            case EASY -> AiStrategy.BASELINE;
            case NORMAL -> pick(NORMAL_PROFILES, random);
            case HARD -> hard(rules, players, random);
        };
    }

    private static AiStrategy hard(RulesetRef rules, int players, RandomGenerator random) {
        boolean duel = players <= 2;
        if (rules.isSpaceward()) {
            return duel ? AiStrategy.INDUSTRY_ADAPTIVE : pick(HARD_EXTENDED_PROFILES, random);
        }
        return duel ? pick(HARD_CLASSIC_DUEL_PROFILES, random) : AiStrategy.BASELINE;
    }

    private static AiStrategy pick(List<AiStrategy> profiles, RandomGenerator random) {
        return profiles.get(random.nextInt(profiles.size()));
    }
}
