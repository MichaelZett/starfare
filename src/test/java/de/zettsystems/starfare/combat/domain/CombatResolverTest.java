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

    @RepeatedTest(20)
    void overwhelmingAttackerAlwaysWinsWithMostShipsIntact() {
        // randomness range [0.9, 1.1) cannot flip a 100-vs-1 fight
        CombatResolver.Result r = CombatResolver.resolve(100, 1);

        assertThat(r.attackerWon()).isTrue();
        assertThat(r.defendersLeft()).isZero();
        assertThat(r.attackersLeft() >= 95).as("expected attacker to keep almost all ships, got " + r.attackersLeft()).isTrue();
    }

    @RepeatedTest(20)
    void overwhelmingDefenderAlwaysHoldsWithMostGarrisonIntact() {
        CombatResolver.Result r = CombatResolver.resolve(1, 100);

        assertThat(r.attackerWon()).isFalse();
        assertThat(r.attackersLeft()).isZero();
        assertThat(r.defendersLeft() >= 95).as("expected defender to keep almost all ships, got " + r.defendersLeft()).isTrue();
    }

    @RepeatedTest(20)
    void survivorCountIsNeverNegativeOrAboveStartingForce() {
        CombatResolver.Result r = CombatResolver.resolve(50, 30);

        assertThat(r.attackersLeft() >= 0 && r.attackersLeft() <= 50).isTrue();
        assertThat(r.defendersLeft() >= 0 && r.defendersLeft() <= 30).isTrue();
        // exactly one side wipes the other in this model
        assertThat((r.attackersLeft() == 0) ^ (r.defendersLeft() == 0)).isTrue();
    }
}
