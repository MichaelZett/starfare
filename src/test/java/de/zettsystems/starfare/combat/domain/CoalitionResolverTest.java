package de.zettsystems.starfare.combat.domain;

import de.zettsystems.starfare.report.values.CoalitionSide;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CoalitionResolverTest {
    private static CoalitionSide side(int id, int ships) {
        return new CoalitionSide(id, 0, List.of(new CoalitionSide.Member(id, "P" + id, ships, ships, 0)));
    }
    @Test void twoSidesMatchExactStrengthDifferenceWithoutRandomness() {
        var result = CoalitionResolver.resolve(List.of(side(1, 100), side(2, 60)), 0, _ -> 0.5);
        assertThat(result).extracting(CoalitionSide::remaining).containsExactly(40, 0);
        assertThat(result.getFirst().members().getFirst().destroyed()).isEqualTo(60);
        assertThat(result.getLast().members().getFirst().destroyed()).isEqualTo(60);
    }
    @Test void alliedLossesAndKillsAreAllocatedWithoutDoubleCounting() {
        var coalition = new CoalitionSide(1, 0, List.of(new CoalitionSide.Member(1, "One", 50, 50, 0),
                new CoalitionSide.Member(2, "Two", 50, 50, 0)));
        var result = CoalitionResolver.resolve(List.of(coalition, side(3, 60)), 0, _ -> 0.5);
        assertThat(result.getFirst().members()).extracting(CoalitionSide.Member::remaining).containsExactly(20, 20);
        assertThat(result.getFirst().members()).extracting(CoalitionSide.Member::destroyed).containsExactly(30, 30);
        assertThat(result.getLast().remaining()).isZero();
    }
    @Test void equalSidesAndThreeWayDeadlockAnnihilateTogether() {
        var pair = CoalitionResolver.resolve(List.of(side(1, 20), side(2, 20)), 0, _ -> 0.5);
        var triple = CoalitionResolver.resolve(List.of(side(3, 20), side(1, 20), side(2, 20)), 0, _ -> 0.5);
        var deadlock = CoalitionResolver.resolve(List.of(side(1, 100), side(2, 100), side(3, 1)), 0, _ -> 0.5);
        assertThat(pair).allMatch(side -> side.remaining() == 0);
        assertThat(triple).allMatch(side -> side.remaining() == 0);
        assertThat(deadlock).allMatch(side -> side.remaining() == 0);
    }
    @Test void remainderTiesUsePlayerIdAndZeroForceNeverReceivesShips() {
        assertThat(ProportionalAllocation.distribute(3, Map.of(3, 0.0, 2, 2.0, 1, 2.0)))
                .containsEntry(1, 2).containsEntry(2, 1).containsEntry(3, 0);
        var empty = CoalitionResolver.resolve(List.of(side(0, 0), side(1, 8)), 30, _ -> 0.5);
        assertThat(empty).extracting(CoalitionSide::remaining).containsExactly(0, 8);
    }
    @Test void randomizedBattlesConserveLossesAndIgnoreListOrder() {
        Random random = new Random(71);
        for (int sample = 0; sample < 500; sample++) {
            List<CoalitionSide> input = new ArrayList<>();
            for (int id = 1; id <= 2 + sample % 8; id++) { input.add(side(id, random.nextInt(501))); }
            var result = CoalitionResolver.resolve(input, sample % 31, id -> (id * 13 % 101) / 101.0);
            Collections.shuffle(input, random);
            assertThat(CoalitionResolver.resolve(input, sample % 31, id -> (id * 13 % 101) / 101.0)).isEqualTo(result);
            assertThat(result.stream().filter(side -> side.remaining() > 0).count()).isLessThanOrEqualTo(1);
            assertThat(result).allSatisfy(side -> assertThat(side.remaining()).isBetween(0, side.ships()));
            int lost = result.stream().mapToInt(side -> side.ships() - side.remaining()).sum();
            assertThat(result.stream().flatMap(side -> side.members().stream()).mapToInt(CoalitionSide.Member::destroyed).sum()).isEqualTo(lost);
        }
    }
    @Test void rollIsAppliedToSideStrengthWithoutGrantingExtraShips() {
        var result = CoalitionResolver.resolve(List.of(side(1, 100), side(2, 100)), 30, id -> id == 1 ? 1 : 0);
        assertThat(result.getFirst().strength()).isCloseTo(130, within(1e-10));
        assertThat(result.getLast().strength()).isEqualTo(70);
        assertThat(result.getFirst().remaining()).isBetween(1, 100);
        assertThat(result.getLast().remaining()).isZero();
    }
    @Test void memberOrderIsCanonicalAndInvalidForcesFailBeforeCombat() {
        var one = new CoalitionSide.Member(1, "One", 3, 3, 0);
        var two = new CoalitionSide.Member(2, "Two", 3, 3, 0);
        var forward = new CoalitionSide(1, 0, List.of(one, two));
        var reversed = new CoalitionSide(1, 0, List.of(two, one));
        assertThat(CoalitionResolver.resolve(List.of(reversed, side(3, 3)), 0, _ -> .5))
                .isEqualTo(CoalitionResolver.resolve(List.of(forward, side(3, 3)), 0, _ -> .5));
        assertThatThrownBy(() -> CoalitionResolver.resolve(List.of(side(1, 3), side(1, 4)), 0, _ -> .5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CoalitionResolver.resolve(List.of(side(1, 3), side(2, 4)), 0, _ -> Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}
