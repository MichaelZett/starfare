package de.zettsystems.starfare.economy.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class IndustryTest {
    @Test void tenCapacityWithFourExpansionPointsGrowsOnlyAfterFiveTurns() {
        Industry industry = Industry.establish(10).allocateExpansion(4);
        for (int turn = 1; turn <= 4; turn++) {
            industry = industry.expandForOneTurn();
            assertThat(industry.capacity()).isEqualTo(10);
            assertThat(industry.expansionProgress()).isEqualTo(turn * 4);
        }
        industry = industry.expandForOneTurn();
        assertThat(industry).isEqualTo(new Industry(11, 4, 0));
        assertThat(industry.expansionCost()).isEqualTo(22);
    }
    @Test void switchingAndConquestRetainPartialProgress() {
        Industry industry = new Industry(10, 4, 8);
        assertThat(industry.allocateExpansion(0).expandForOneTurn()).isEqualTo(new Industry(10, 0, 8));
        assertThat(industry.allocateExpansion(7).captured()).isEqualTo(new Industry(10, 0, 8));
        assertThat(industry.allocateExpansion(7).expandForOneTurn()).isEqualTo(new Industry(10, 7, 15));
    }
    @Test void unusedProgressCarriesOverAndMaximumReturnsAllOutputToShips() {
        assertThat(new Industry(10, 9, 18).expandForOneTurn()).isEqualTo(new Industry(11, 9, 7));
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
}
