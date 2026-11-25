package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.testsupport.DomEvents;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndustryJourneyTest extends AbstractEconomyUiTest {
    private int relocated() {
        return registry.readState(id, state -> state.standingOrders().get(1).stream()
                .mapToInt(de.zettsystems.starfare.game.values.StandingOrder::ships).sum());
    }

    private int savedExpansion() {
        return registry.readState(id, state -> state.industries().get(1).expansionAllocation());
    }

    private Input slider() {
        return find(Input.class).withAttribute("aria-label", "Ausbau").single();
    }

    @Test
    void allocationPersistsLocksAfterSubmissionAndGrowsWithoutPrematureShips() {
        openGame(EconomyUiStates.industry(), "Industry Browserless");
        assertThat(text("industry-panel")).contains("Kapazität 10 / 50");
        assertThat(text("production-result").strip()).isEqualTo("10 Schiffe/Runde · Ausbau 0/10");
        // Dragging only previews; nothing is saved until the slider is released.
        DomEvents.previewRange(slider(), 8);
        assertThat(text("production-result").strip()).isEqualTo("2 Schiffe/Runde · Ausbau 8/10 · fertig in 2 Runden");
        assertThat(savedExpansion()).isZero();

        enter("industry-expansion", 4);
        assertThat(text("production-save-status")).contains("Gespeichert");
        assertThat(text("production-result").strip()).isEqualTo("6 Schiffe/Runde · Ausbau 4/10 · fertig in 3 Runden");
        assertThat(savedExpansion()).isEqualTo(4);
        // Less shipbuilding shortens the relocations from Alpha to the six ships it still builds.
        assertThat(relocated()).isEqualTo(6);
        assertThat(text("system-metric-rate")).contains("6 (+0/Runde)");

        reloadMap();
        assertThat(field("industry-expansion").getValue()).isEqualTo(4);
        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        assertThat(viewText()).contains("Abgegeben");
        assertThat(find(IntegerField.class).withId("industry-expansion").all()).isEmpty();
        assertThat(text("production-result").strip()).isEqualTo("6 Schiffe/Runde · Ausbau 4/10 · fertig in 3 Runden");
        assertThat(games.allocateExpansion(id, 1, 1, 1, 0)).isFalse();
        assertThat(games.submitTurn(id, 2)).isTrue();
        for (int round = 2; round <= 3; round++) { nextRound(); }
        runPendingUiAccess();
        select("Alpha");
        assertThat(viewText()).contains("Runde 4");
        assertThat(text("industry-panel")).contains("Kapazität 11 / 50");
        assertThat(text("production-result").strip()).isEqualTo("7 Schiffe/Runde · Ausbau 6/13 · fertig in 3 Runden");
        assertThat(garrison()).isEqualTo(12);

        reloadMap();
        DomEvents.changeRange(slider(), 0);
        runPendingUiAccess();
        assertThat(savedExpansion()).isZero();
        assertThat(text("production-result").strip()).isEqualTo("11 Schiffe/Runde · Ausbau 2/13");
        // More shipbuilding never lengthens a shortened relocation again.
        assertThat(relocated()).isEqualTo(6);
    }
}
