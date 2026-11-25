package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.EconomyRules;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class IndustryTest {
    @Test void tenCapacityWithFourExpansionPointsGrowsInTheThirdTurn() {
        Industry industry = Industry.establish(10).allocateExpansion(4);
        assertThat(industry.expansionCost()).isEqualTo(10);
        for (int turn = 1; turn <= 2; turn++) {
            industry = industry.expandForOneTurn();
            assertThat(industry.capacity()).isEqualTo(10);
            assertThat(industry.expansionProgress()).isEqualTo(turn * 4);
        }
        industry = industry.expandForOneTurn();
        assertThat(industry).isEqualTo(new Industry(11, 4, 2));
        assertThat(industry.expansionCost()).isEqualTo(13);
    }
    @Test void switchingAndConquestRetainPartialProgress() {
        Industry industry = new Industry(10, 4, 8);
        assertThat(industry.allocateExpansion(0).expandForOneTurn()).isEqualTo(new Industry(10, 0, 8));
        assertThat(industry.allocateExpansion(7).captured()).isEqualTo(new Industry(10, 0, 8));
        assertThat(industry.allocateExpansion(7).expandForOneTurn()).isEqualTo(new Industry(11, 7, 5));
    }
    @Test void unusedProgressCarriesOverAndMaximumReturnsAllOutputToShips() {
        assertThat(new Industry(10, 9, 9).expandForOneTurn()).isEqualTo(new Industry(11, 9, 8));
        Industry maximum = new Industry(49, 49, 97).expandForOneTurn();
        assertThat(maximum).isEqualTo(new Industry(50, 0, 48));
        assertThat(maximum.shipbuilding()).isEqualTo(50);
        assertThat(maximum.expandForOneTurn()).isEqualTo(maximum);
        assertThatThrownBy(() -> maximum.allocateExpansion(1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void invalidBudgetsAndProgressCannotBeConstructed() {
        assertThatThrownBy(() -> new Industry(0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Industry(51, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Industry(10, 11, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Industry(10, -1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Industry(10, 0, 20)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void cheapEarlyIndustryCostsATenthOfTheSquaredCapacityRoundedUpButAtLeastTwo() {
        assertThat(EconomyRules.expansionCost(1)).isEqualTo(2);
        assertThat(EconomyRules.expansionCost(5)).isEqualTo(3);
        assertThat(EconomyRules.expansionCost(14)).isEqualTo(20);
        assertThat(EconomyRules.expansionCost(25)).isEqualTo(50);
    }
}
