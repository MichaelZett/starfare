package de.zettsystems.starfare.combat.domain;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stateless combat resolution helper with slight randomness.
 */
public final class CombatResolver {
    private CombatResolver() {}

    public static Result resolve(int attacking, int defending) {
        if (attacking <= 0) {
            return new Result(0, Math.max(0, defending), false);
        }
        if (defending <= 0) {
            return new Result(attacking, 0, true);
        }

        double aEff = attacking * rand();
        double dEff = defending * rand();

        if (aEff > dEff) {
            // Anteil der Angriffs-"Stärke", die überlebt
            double lossRatio = dEff / aEff;                    // 0.. <1
            int attRemain = Math.clamp(attacking - Math.round(attacking * lossRatio), 0, attacking);
            return new Result(attRemain, 0, true);
        } else {
            double lossRatio = aEff / dEff;
            int defRemain = Math.clamp(defending - Math.round(defending * lossRatio), 0, defending);
            return new Result(0, defRemain, false);
        }
    }

    private static double rand() { return 0.9 + ThreadLocalRandom.current().nextDouble(0.2); }

    public record Result(int attackersLeft, int defendersLeft, boolean attackerWon) {}
}
