package de.zettsystems.starfare.economy.ui;

import de.zettsystems.starfare.testsupport.ColonyStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColonyJourneyTest extends AbstractEconomyUiTest {
    @Test
    void populationLimitsAllocationAndHistoricalReviewKeepsTheRecordedColony() {
        openGame(ColonyStates.readyWithMoon(), "Colony Browserless");
        assertThat(text("colony-details")).contains("Nutzbare Industrie: 6 von 10", "Bevölkerung: 6,9",
                "Hauptkolonie auf einem Mond");
        click("production-plan-open");
        assertThat(field("industry-expansion").getMax()).isEqualTo(6);
        enter("industry-expansion", 3);
        assertThat(text("production-save-status")).contains("Gespeichert");
        nextRound();
        reloadMap();
        assertThat(text("colony-details")).contains("Nutzbare Industrie: 7 von 10", "Bevölkerung: 7,11");
        int production = registry.readState(id, state -> state.getSystem(1).productionPerTurn());
        assertThat(production).isEqualTo(4);
        nextRound();
        registry.writeState(id, state -> { state.endGame(1); return null; });
        reloadMap();
        assertThat(text("colony-details")).contains("Bevölkerung: 7,33");
        reviewFirstRound();
        assertThat(text("colony-details")).contains("Bevölkerung: 7,11");
        assertArchived("Colony Browserless");
    }
}
