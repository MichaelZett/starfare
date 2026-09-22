package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionFlowTest {
    @Test
    void sumsTransferAmountsRatherThanSourceProduction() {
        var orders = List.of(new StandingOrderView(1, 1, 2, "A", "B", 9, 3),
                new StandingOrderView(2, 3, 2, "C", "B", 8, 2),
                new StandingOrderView(3, 2, 4, "B", "D", 7, 4));
        assertThat(ProductionFlow.at(2, orders)).isEqualTo(new ProductionFlow(5, 4));
        assertThat(ProductionFlow.at(5, orders)).isEqualTo(new ProductionFlow(0, 0));
    }
}
