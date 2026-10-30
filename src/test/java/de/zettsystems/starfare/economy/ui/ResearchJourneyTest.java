package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchJourneyTest extends AbstractEconomyUiTest {
    @Test
    void savedBudgetLocksAfterSubmissionAndNewTechnologyProducesMixedCohorts() {
        openGame(EconomyUiStates.research(), "Research Browserless");
        openResearch();
        assertThat(elementText(openedDialog().getElement())).contains("Fortschritt 98/100");
        saveBudget(50);
        reloadMap();
        openResearch();
        assertThat(field("research-budget").getValue()).isEqualTo(50);
        assertThat(viewText()).doesNotContain("Arbeitskräfte fehlen");
        saveBudget(25);
        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        assertThat(viewText()).contains("Abgegeben");
        openResearch();
        assertThat(find(Button.class).withId("research-save").single().isEnabled()).isFalse();
        test(button("Schließen")).click();
        assertThat(games.submitTurn(id, 2)).isTrue();
        runPendingUiAccess();
        assertThat(viewText()).contains("Schiffsforschung: Technikstufe 1 erreicht");
        int technology = registry.readState(id, state -> state.shipTechnology(1));
        assertThat(technology).isEqualTo(1);
        openResearch();
        assertThat(elementText(openedDialog().getElement())).contains("Technikstufe 1");
        saveBudget(0);
        nextRound();
        reloadMap();
        var cohorts = registry.readState(id, state -> state.getSystem(1).composition().levels());
        assertThat(cohorts).containsKeys(0, 1);
        assertThat(text("industry-panel")).contains("Stufe 0:", "Stufe 1:", "Kampfstärke");
    }

    private void openResearch() {
        MenuBarTester<MenuBar> menu = test(find(MenuBar.class).withClassName("app-header-menu").single());
        menu.clickItem(0, 0);
        assertThat(openedDialog().getHeaderTitle()).isEqualTo("Schiffsforschung");
    }

    private void saveBudget(int percent) {
        enter("research-budget", percent);
        click("research-save");
        assertThat(find(Button.class).withId("research-save").all()).isEmpty();
        int budget = registry.readState(id, state -> state.metals().orElseThrow().research().empires().get(1).percent());
        assertThat(budget).isEqualTo(percent);
    }
}
