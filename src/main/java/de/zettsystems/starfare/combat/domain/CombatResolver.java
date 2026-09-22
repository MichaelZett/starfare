package de.zettsystems.starfare.combat.domain;

import de.zettsystems.starfare.game.values.GameConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stateless combat resolution helper with a configurable random strength roll.
 */
public final class CombatResolver {
    private CombatResolver() {}

    public static Result resolve(int attacking, int defending) {
        return resolve(attacking, defending, GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT);
    }

    public static Result resolve(int attacking, int defending, int randomnessPercent) {
        return resolve(attacking, defending, randomnessPercent,
                ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble());
    }

    static Result resolve(int attacking, int defending, int randomnessPercent,
                          double attackerRoll, double defenderRoll) {
        if (attacking <= 0) {
            return new Result(0, Math.max(0, defending), false, 0, Math.max(0, defending));
        }
        if (defending <= 0) {
            return new Result(attacking, 0, true, attacking, 0);
        }

        int variation = Math.clamp(randomnessPercent, GameConfig.MIN_COMBAT_RANDOMNESS_PERCENT,
                GameConfig.MAX_COMBAT_RANDOMNESS_PERCENT);
        int attackerStrength = rolledStrength(attacking, variation, attackerRoll);
        int defenderStrength = rolledStrength(defending, variation, defenderRoll);
        double aEff = attackerStrength;
        double dEff = defenderStrength;

        if (aEff > dEff) {
            // Anteil der Angriffs-"Stärke", die überlebt
            double lossRatio = dEff / aEff;                    // 0.. <1
            int attRemain = Math.clamp(attacking - Math.round(attacking * lossRatio), 0, attacking);
            return new Result(attRemain, 0, true, attackerStrength, defenderStrength);
        } else {
            double lossRatio = aEff / dEff;
            int defRemain = Math.clamp(defending - Math.round(defending * lossRatio), 0, defending);
            return new Result(0, defRemain, false, attackerStrength, defenderStrength);
        }
    }

    private static int rolledStrength(int ships, int variationPercent, double roll) {
        double boundedRoll = Math.clamp(roll, 0.0, Math.nextDown(1.0));
        double factor = 1.0 - variationPercent / 100.0 + boundedRoll * (2.0 * variationPercent / 100.0);
        return Math.max(1, (int) Math.round(ships * factor));
    }

    public record Result(int attackersLeft, int defendersLeft, boolean attackerWon,
                         int attackerStrength, int defenderStrength) {}
}
