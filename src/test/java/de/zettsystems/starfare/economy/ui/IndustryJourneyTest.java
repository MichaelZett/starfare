package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndustryJourneyTest extends AbstractEconomyUiTest {
    private int relocated() {
        return registry.readState(id, state -> state.standingOrders().get(1).stream()
                .mapToInt(de.zettsystems.starfare.game.values.StandingOrder::ships).sum());
    }

    @Test
    void allocationPersistsLocksAfterSubmissionAndGrowsWithoutPrematureShips() {
        openGame(EconomyUiStates.industry(), "Industry Browserless");
        assertThat(text("industry-panel")).contains("Kapazität 10 / 50");
        click("production-plan-open");
        enter("industry-expansion", 4);
        assertThat(text("production-save-status")).contains("Gespeichert");
        assertThat(text("industry-panel")).contains("6 Punkte → 6 Schiffe/Runde", "4 Punkte → Fortschritt 0/10 → 4/10");
        // Less shipbuilding shortens the relocations from Alpha to the six ships it still builds.
        assertThat(text("production-plan-results")).contains("Verlegung: voraussichtlich 6 Schiffe");
        assertThat(relocated()).isEqualTo(6);
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
        for (int round = 2; round <= 3; round++) { nextRound(); }
        runPendingUiAccess();
        select("Alpha");
        assertThat(viewText()).contains("Runde 4");
        assertThat(text("industry-panel")).contains("Kapazität 11 / 50", "7 Punkte → 7 Schiffe/Runde",
                "4 Punkte → Fortschritt 2/13 → 6/13", "Ausbaufortschritt: 2 / 13");
        assertThat(garrison()).isEqualTo(12);

        reloadMap();
        click("production-plan-open");
        enter("industry-expansion", 0);
        assertThat(text("industry-panel")).contains("11 Punkte → 11 Schiffe/Runde");
        // More shipbuilding never lengthens a shortened relocation again.
        assertThat(text("production-plan-results")).contains("Verlegung: voraussichtlich 6 Schiffe");
    }
}
