package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionPlanningJourneyTest extends AbstractEconomyUiTest {
    @Test
    void expansionTrimsRelocationsAndSurvivesNavigationUntilTheRoundIsSubmitted() {
        openGame(EconomyUiStates.directProduction(), "Direct production Browserless");
        assertThat(text("production-functions")).contains("10 Punkte → 10 Schiffe/Runde");
        click("production-plan-open");
        enter(IndustryPanel.ALLOCATION_ID, 4);
        assertThat(text("production-save-status")).contains("Verlegung nach Beta: 6 → 4/Runde", "Verlegung nach Enemy: 4 → 2/Runde");
        assertThat(routes()).containsEntry(2, 4).containsEntry(3, 2);
        assertThat(text("production-functions")).contains("6 Punkte → 6 Schiffe/Runde");
        select("Beta");
        select("Alpha");
        reloadMap();
        click("production-plan-open");
        assertThat(field(IndustryPanel.ALLOCATION_ID).getValue()).isEqualTo(4);
        assertThat(field("industry-shipbuilding").getValue()).isEqualTo(6);
        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        assertThat(viewText()).contains("Abgegeben");
        int expansion = registry.readState(id, state -> state.industries().get(1).expansionAllocation());
        assertThat(expansion).isEqualTo(4);
        assertThat(find(IntegerField.class).withId(IndustryPanel.ALLOCATION_ID).all()).isEmpty();
        assertThat(find(Component.class).withId("production-submit-warning").all()).isEmpty();
    }

    private Map<Integer, Integer> routes() {
        return registry.readState(id, state -> state.standingOrders().get(1).stream()
                .collect(Collectors.toMap(StandingOrder::toSystemId, StandingOrder::ships)));
    }
}
