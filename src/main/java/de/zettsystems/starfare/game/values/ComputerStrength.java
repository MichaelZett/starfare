package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.ai.values.AiStrategy;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * How strong the computer empires of a game are: their planning profile and their start compared with the
 * people. The classic values are calibrated with the strategy bench (docs/STRATEGY-EXPERIMENTS.md, 25.11.2026):
 * a person playing like the baseline wins about three to four of five duels against easy computers, half against
 * normal ones and one in five against hard ones. The extended rules keep their first proposal until phase E.
 */
public enum ComputerStrength {
    EASY(ComputerStrength.EASY_CLASSIC_SHARE, ComputerStrength.EASY_EXTENDED_SHARE),
    NORMAL(ComputerStrength.NORMAL_SHARE, ComputerStrength.NORMAL_SHARE),
    HARD(ComputerStrength.HARD_CLASSIC_SHARE, ComputerStrength.HARD_EXTENDED_SHARE);

    public static final ComputerStrength DEFAULT = NORMAL;
    /** Start production and garrison of a computer empire relative to a person's. */
    static final double EASY_CLASSIC_SHARE = 0.5;
    static final double NORMAL_SHARE = 1.0;
    static final double HARD_CLASSIC_SHARE = 1.5;
    static final double EASY_EXTENDED_SHARE = 0.75;
    static final double HARD_EXTENDED_SHARE = 1.25;
    /**
     * Classic duels: rush alone leaves a person hardly a chance on a large map whatever the start; half of the hard
     * computers play the baseline with the larger start instead. Beyond a duel the baseline is the strongest profile.
     */
    static final List<AiStrategy> HARD_CLASSIC_DUEL_PROFILES = List.of(AiStrategy.BASELINE, AiStrategy.RUSH);
    /** Extended normal computers each draw one of these, hidden from the players. */
    static final List<AiStrategy> NORMAL_EXTENDED_PROFILES =
            List.of(AiStrategy.RUSH, AiStrategy.EXPANSION, AiStrategy.CONCENTRATION, AiStrategy.DEFENSE);
    /** Best extended profiles of the bench: adaptive industry led duels, with seven empires it was level with the baseline. */
    static final List<AiStrategy> HARD_EXTENDED_PROFILES = List.of(AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.BASELINE);

    private final double classicShare;
    private final double extendedShare;

    ComputerStrength(double classicShare, double extendedShare) {
        this.classicShare = classicShare;
        this.extendedShare = extendedShare;
    }

    /** Start production and garrison of a computer empire relative to a person's under these rules. */
    public double startShare(RulesetRef rules) {
        return rules.isSpaceward() ? extendedShare : classicShare;
    }

    /** A computer's start value under these rules derived from a person's, never below one. */
    public int computerStart(RulesetRef rules, int humanValue) {
        return computerStart(humanValue, startShare(rules));
    }

    /** A computer's start value at the given share of a person's, never below one. */
    public static int computerStart(int humanValue, double share) {
        return Math.max(1, (int) Math.round(humanValue * share));
    }

    /**
     * Planning profile of one computer empire. With more than two empires the classic baseline is the strongest
     * profile of the bench, the others mostly fight each other; there the start share alone sets the strength.
     *
     * @param players every empire of the game, people included; a duel has two
     */
    public AiStrategy strategy(RulesetRef rules, int players, RandomGenerator random) {
        boolean duel = players <= 2;
        if (rules.isSpaceward()) {
            return switch (this) {
                case EASY -> AiStrategy.BASELINE;
                case NORMAL -> pick(NORMAL_EXTENDED_PROFILES, random);
                case HARD -> duel ? AiStrategy.INDUSTRY_ADAPTIVE : pick(HARD_EXTENDED_PROFILES, random);
            };
        }
        return switch (this) {
            case EASY -> AiStrategy.BASELINE;
            // The other profiles beat the baseline in duels and lose to it in larger games; none is fair to a person.
            case NORMAL -> AiStrategy.BASELINE;
            case HARD -> duel ? pick(HARD_CLASSIC_DUEL_PROFILES, random) : AiStrategy.BASELINE;
        };
    }

    private static AiStrategy pick(List<AiStrategy> profiles, RandomGenerator random) {
        return profiles.get(random.nextInt(profiles.size()));
    }
}
