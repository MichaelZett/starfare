package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.ai.values.AiStrategy;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * How strong the computer empires of a game are: their planning profile and their start compared with the
 * people. Both rules are calibrated with the strategy bench (docs/STRATEGY-EXPERIMENTS.md, 25.11.2026): a person
 * playing like the baseline wins about three to four of five duels against easy computers, half against normal ones
 * and one in five or fewer against hard ones. With the extended rules the cheap first industry levels let a smaller
 * start catch up, so only half the start makes computers easier, and hard computers expand their industry instead
 * of starting larger.
 */
public enum ComputerStrength {
    EASY(ComputerStrength.EASY_SHARE, ComputerStrength.EASY_SHARE),
    NORMAL(ComputerStrength.NORMAL_SHARE, ComputerStrength.NORMAL_SHARE),
    HARD(ComputerStrength.HARD_CLASSIC_SHARE, ComputerStrength.NORMAL_SHARE);

    public static final ComputerStrength DEFAULT = NORMAL;
    /** Start production and garrison of a computer empire relative to a person's. */
    static final double EASY_SHARE = 0.5;
    static final double NORMAL_SHARE = 1.0;
    static final double HARD_CLASSIC_SHARE = 1.5;
    /**
     * Classic duels: rush alone leaves a person hardly a chance on a large map whatever the start; half of the hard
     * computers play the baseline with the larger start instead. Beyond a duel the baseline is the strongest profile.
     */
    static final List<AiStrategy> HARD_CLASSIC_DUEL_PROFILES = List.of(AiStrategy.BASELINE, AiStrategy.RUSH);

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
     * profile of the bench, the others mostly fight each other; there the start share alone sets the strength. Under
     * the extended rules the profiles without industry lose to the baseline in every size; hard computers play the
     * adaptive industry profile, the strongest of the bench.
     *
     * @param players every empire of the game, people included; a duel has two
     */
    public AiStrategy strategy(RulesetRef rules, int players, RandomGenerator random) {
        if (rules.isSpaceward()) {
            return this == HARD ? AiStrategy.INDUSTRY_ADAPTIVE : AiStrategy.BASELINE;
        }
        boolean duel = players <= 2;
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
