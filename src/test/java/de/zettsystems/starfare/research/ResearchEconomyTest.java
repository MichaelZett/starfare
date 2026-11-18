package de.zettsystems.starfare.research;

import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.research.domain.ResearchEconomy;
import de.zettsystems.starfare.research.values.ShipResearch;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResearchEconomyTest {
    private static final List<StarSystem> SYSTEMS = List.of(
            new StarSystem(1, "A", 0, 0, 1, 5, 4, false),
            new StarSystem(2, "B", 100, 0, 1, 5, 3, false),
            new StarSystem(3, "C", 200, 0, 2, 5, 9, false),
            new StarSystem(4, "D", 300, 0, null, 5, 5, true));

    @Test void empireBudgetIsRoundedOnceAndTheRemainderGoesToTheLargestFraction() {
        var research = new ResearchEconomy(Map.of(1, new ShipResearch(30, 0, 0),
                2, new ShipResearch(50, ShipResearch.MAX_LEVEL, 0)));
        var industries = Map.of(1, Industry.establish(4), 2, Industry.establish(3),
                3, Industry.establish(9), 4, Industry.establish(5));

        // 30 % of 4 + 3 capacity is 2.1 points: 1.2 and 0.9 floor to 1 and 0, the 0.9 share gets the second point.
        // A completed empire reserves nothing, neutral systems never do.
        assertThat(research.reservations(SYSTEMS, industries)).isEqualTo(Map.of(1, 1, 2, 1, 3, 0, 4, 0));
    }

    @Test void eachEmpireAdvancesOnlyWithItsOwnSystemsPoints() {
        var research = new ResearchEconomy(Map.of(1, new ShipResearch(30, 0, 0), 2, new ShipResearch(0, 0, 0)));

        var advanced = research.advance(SYSTEMS, Map.of(1, 30, 2, 40, 3, 20, 4, 15));

        assertThat(advanced.empires()).isEqualTo(Map.of(1, new ShipResearch(30, 0, 70), 2, new ShipResearch(0, 0, 20)));
    }
}
