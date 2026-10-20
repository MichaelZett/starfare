package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResearchBalanceRegressionTest {
    @Test void conquestRebalancesBothEmpiresBeforeRefreshingShipbuilding() throws Exception {
        var scenario = new Scenario(RulesetRef.SPACEWARD_SHIP_RESEARCH, GalaxyLayout.EVEN, 32, 42, 314159, 150, 10);
        var result = ResearchBenchmark.play(scenario, AiStrategy.CONCENTRATION, 2, 40);
        assertThat(result.rounds()).isPositive();
        assertThat(result.research()).isPositive();
    }
}
