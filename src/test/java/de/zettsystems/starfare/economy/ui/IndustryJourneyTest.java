package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndustryJourneyTest extends AbstractEconomyUiTest {
    @Test
    void allocationPersistsLocksAfterSubmissionAndGrowsWithoutPrematureShips() {
        openGame(EconomyUiStates.industry(), "Industry Browserless");
        assertThat(text("industry-panel")).contains("Kapazität 10 / 50");
        click("production-plan-open");
        enter("industry-expansion", 4);
        assertThat(text("production-save-status")).contains("Gespeichert");
        assertThat(text("industry-panel")).contains("6 Punkte → 6 Schiffe/Runde", "4 Punkte → Fortschritt 0/20 → 4/20");
        assertThat(text("production-plan-results")).contains("Verlegung: voraussichtlich 8 Schiffe");
        assertThat(text("system-metric-rate")).contains("6 (+0/Runde)");

        reloadMap();
        click("production-plan-open");
        assertThat(field("industry-expansion").getValue()).isEqualTo(4);
        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        assertThat(viewText()).contains("Abgegeben");
        assertThat(find(Button.class).withId("production-plan-open").all()).isEmpty();
        assertThat(games.allocateExpansion(id, 1, 1, 1, 0)).isFalse();
        assertThat(games.submitTurn(id, 2)).isTrue();
        for (int round = 2; round <= 5; round++) { nextRound(); }
        runPendingUiAccess();
        select("Alpha");
        assertThat(viewText()).contains("Runde 6");
        assertThat(text("industry-panel")).contains("Kapazität 11 / 50", "7 Punkte → 7 Schiffe/Runde",
                "4 Punkte → Fortschritt 0/22 → 4/22", "Ausbaufortschritt: 0 / 22");
        assertThat(garrison()).isEqualTo(10);

        registry.writeState(id, state -> { state.updateSystem(1, system -> system.afterDefense(1)); return null; });
        reloadMap();
        click("production-plan-open");
        assertThat(text("production-plan-results")).contains("Verlegung: voraussichtlich 0 Schiffe");
        enter("industry-expansion", 0);
        assertThat(text("industry-panel")).contains("11 Punkte → 11 Schiffe/Runde");
        assertThat(text("production-plan-results")).contains("Verlegung: voraussichtlich 2 Schiffe");
    }
}
