package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
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
    void presetsTrimRelocationsAndSystemNavigationPreservesOnlyCurrentInputs() {
        openGame(EconomyUiStates.directProduction(), "Direct production Browserless");
        assertThat(text("production-functions")).contains("10 Punkte → 10 Schiffe/Runde");
        click("production-preset-autark");
        assertThat(mining(1)).isEqualTo(5);
        assertThat(text("production-save-status")).contains("Verlegung nach Beta: 6 → 3/Runde", "Verlegung nach Enemy: 4 → 2/Runde");
        assertThat(routes()).containsEntry(2, 3).containsEntry(3, 2);
        assertThat(text("production-functions")).contains("5 Punkte → 5 Schiffe/Runde");
        click("production-plan-open");
        assertThat(field("industry-mining").getValue()).isEqualTo(5);
        enter("industry-mining", 3);
        assertThat(routes()).containsEntry(2, 3).containsEntry(3, 2);
        test(find(Checkbox.class).withId("industry-expansion-pin").single()).click();
        select("Beta");
        click("production-preset-metal");
        assertThat(mining(2)).isEqualTo(4);
        select("Alpha");
        assertThat(find(Checkbox.class).withId("industry-expansion-pin").single().getValue()).isTrue();
        assertThat(field("industry-mining").getValue()).isEqualTo(3);
        assertThat(games.allocateProduction(id, 1, 1, 2, 1, 0)).isTrue();
        runPendingUiAccess();
        select("Beta");
        assertThat(field("industry-mining").getValue()).isZero();
        click("production-preset-ships");
        reloadMap();
        click("production-plan-open");
        assertThat(field("industry-mining").getValue()).isEqualTo(3);
        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        assertThat(viewText()).contains("Abgegeben");
        assertThat(mining(1)).isEqualTo(3);
        assertThat(find(IntegerField.class).withId("industry-mining").all()).isEmpty();
        assertThat(find(Component.class).withId("production-submit-warning").all()).isEmpty();
    }

    private Map<Integer, Integer> routes() {
        return registry.readState(id, state -> state.standingOrders().get(1).stream()
                .collect(Collectors.toMap(StandingOrder::toSystemId, StandingOrder::ships)));
    }
}
