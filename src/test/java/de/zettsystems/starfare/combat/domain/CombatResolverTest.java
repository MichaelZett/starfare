package de.zettsystems.starfare.combat.domain;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CombatResolverTest {

    @Test
    void resolveHandlesZeroAttackers() {
        CombatResolver.Result result = CombatResolver.resolve(0, 5);

        assertThat(result.attackersLeft()).isZero();
        assertThat(result.defendersLeft()).isEqualTo(5);
        assertThat(result.attackerWon()).isFalse();
    }

    @Test
    void resolveHandlesZeroDefenders() {
        CombatResolver.Result result = CombatResolver.resolve(7, 0);

        assertThat(result.attackersLeft()).isEqualTo(7);
        assertThat(result.defendersLeft()).isZero();
        assertThat(result.attackerWon()).isTrue();
    }

    @Test
    void zeroOnBothSidesIsANoOp() {
        CombatResolver.Result r = CombatResolver.resolve(0, 0);

        assertThat(r.attackerWon()).isFalse();
        assertThat(r.attackersLeft()).isZero();
        assertThat(r.defendersLeft()).isZero();
    }

    @Test
    void zeroRandomnessUsesExactStartingStrengths() {
        CombatResolver.Result result = CombatResolver.resolve(100, 100, 0, 0.0, 0.999);

        assertThat(result.attackerStrength()).isEqualTo(100);
        assertThat(result.defenderStrength()).isEqualTo(100);
        assertThat(result.attackerWon()).isFalse();
    }

    @Test
    void maximumRandomnessCanFlipEqualForces() {
        CombatResolver.Result attackerWins = CombatResolver.resolve(100, 100, 30, 0.999, 0.0);
        CombatResolver.Result defenderWins = CombatResolver.resolve(100, 100, 30, 0.0, 0.999);

        assertThat(attackerWins.attackerStrength()).isEqualTo(130);
        assertThat(attackerWins.defenderStrength()).isEqualTo(70);
        assertThat(attackerWins.attackerWon()).isTrue();
        assertThat(defenderWins.attackerWon()).isFalse();
    }

    @RepeatedTest(20)
    void overwhelmingAttackerAlwaysWinsWithMostShipsIntact() {
        // randomness range [0.9, 1.1) cannot flip a 100-vs-1 fight
        CombatResolver.Result r = CombatResolver.resolve(100, 1);

        assertThat(r.attackerWon()).isTrue();
        assertThat(r.defendersLeft()).isZero();
        assertThat(r.attackersLeft()).as("expected attacker to keep almost all ships, got " + r.attackersLeft())
                .isGreaterThanOrEqualTo(95);
    }

    @RepeatedTest(20)
    void overwhelmingDefenderAlwaysHoldsWithMostGarrisonIntact() {
        CombatResolver.Result r = CombatResolver.resolve(1, 100);

        assertThat(r.attackerWon()).isFalse();
        assertThat(r.attackersLeft()).isZero();
        assertThat(r.defendersLeft()).as("expected defender to keep almost all ships, got " + r.defendersLeft())
                .isGreaterThanOrEqualTo(95);
    }

    @RepeatedTest(20)
    void survivorCountIsNeverNegativeOrAboveStartingForce() {
        CombatResolver.Result r = CombatResolver.resolve(50, 30);

        assertThat(r.attackersLeft()).isGreaterThanOrEqualTo(0);

        assertThat(r.attackersLeft()).isLessThanOrEqualTo(50);
        assertThat(r.defendersLeft()).isGreaterThanOrEqualTo(0);
        assertThat(r.defendersLeft()).isLessThanOrEqualTo(30);
        // exactly one side wipes the other in this model
        assertThat((r.attackersLeft() == 0) ^ (r.defendersLeft() == 0)).isTrue();
    }
}
