package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.ai.values.AiStrategy;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * How strong the computer empires of a game are: their planning profile and their start compared with the
 * people. Both rules are calibrated with the strategy bench (docs/STRATEGY-EXPERIMENTS.md, 25.11.2026): a person
 * playing like the baseline wins about three to four of five duels against easy computers, half or a bit more against
 * normal ones and one in five or fewer against hard ones.
 * <p>
 * Classic normal computers play the calm profile ({@link AiStrategy#CAUTIOUS}, K7d) with the same start: the baseline
 * sends a fleet every round, which people found restless and hard to read. On 1,200 control games (maps 1001-1200,
 * medium and large, two to four empires) the reference won 64/50 % of the duels, 32/35 % with three and 33/29 % with
 * four empires (baseline computers: 52/42, 29/30, 24/27 %), while the computers attacked another empire about 0.4
 * instead of 0.75 times per round, with larger fleets.
 * <p>
 * With the extended rules the cheap first industry levels let a smaller
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
     * profile of the bench, the others mostly fight each other; there the start share alone sets the strength of easy
     * and hard computers. Normal classic computers play the calm profile in every game size. Under
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
            // The baseline was fair but restless; the calm profile attacks about half as often with the same start.
            case NORMAL -> AiStrategy.CAUTIOUS;
            case HARD -> duel ? pick(HARD_CLASSIC_DUEL_PROFILES, random) : AiStrategy.BASELINE;
        };
    }

    private static AiStrategy pick(List<AiStrategy> profiles, RandomGenerator random) {
        return profiles.get(random.nextInt(profiles.size()));
    }
}
