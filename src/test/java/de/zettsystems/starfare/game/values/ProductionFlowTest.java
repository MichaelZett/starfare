package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionFlowTest {
    @Test void quickAmountPreservesNetProductionOnTopOfAnAlreadyDeductedReserve() {
        ProductionFlow flow = new ProductionFlow(6, 3);
        assertThat(flow.retainedShips(4)).isEqualTo(7);
        assertThat(flow.sendExceptProduction(20, 4)).isEqualTo(13);
        assertThat(flow.sendExceptProduction(7, 4)).isZero();
        assertThat(flow.sendExceptProduction(0, 4)).isZero();
    }
    @Test void outflowCannotCreateAnegativeReserveOrExceedTheModeMaximum() {
        assertThat(new ProductionFlow(0, 9).sendExceptProduction(20, 4)).isEqualTo(20);
        assertThat(new ProductionFlow(0, 0).sendExceptProduction(20, 4)).isEqualTo(16);
        assertThat(new ProductionFlow(6, 0).sendExceptProduction(3, 4)).isZero();
    }
}
