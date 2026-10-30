package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.testsupport.DomEvents;
import de.zettsystems.starfare.testsupport.MetalStates;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MetalJourneyTest extends AbstractEconomyUiTest {
    @Test
    void previewLocksSavedAllocationsExhaustionAndHistoricalMetalRemainConsistent() {
        openGame(MetalStates.ready(2, 5), "Metal Browserless");
        assertThat(text("metal-details")).contains("6 Punkte → 1 von 6 Schiffen/Runde · Metall fehlt",
                "Metall fördern · 3 je Punkt");
        assertThat(text("app-header-stats")).contains("2 (−2)");
        changeLanguage(Locale.ENGLISH);
        assertThat(text("metal-details")).contains("6 points → 1 of 6 ships/round · metal missing");
        click("production-plan-open");
        assertThat(text("production-editor")).contains("Distribute 6 capacity points");
        changeLanguage(Locale.GERMAN);
        click("production-plan-open");
        previewAndPinAllocation();
        enter("industry-mining", 2);
        enter("industry-expansion", 1);
        assertThat(text("metal-details")).contains("3 Punkte → 3 Schiffe/Runde",
                "2 Punkte → 5 Metall/Runde · Vorkommen erschöpft");
        assertThat(text("production-save-status")).contains("Gespeichert");
        assertExhaustionAndHistory();
    }

    private void previewAndPinAllocation() {
        var before = registry.readState(id, GameState::toSnapshot);
        DomEvents.previewRange(miningSlider(), 6);
        assertThat(text("metal-details")).contains("0 Punkte → 0 Schiffe/Runde");
        assertThat(field("industry-shipbuilding").getValue()).isZero();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        DomEvents.changeRange(miningSlider(), 6);
        runPendingUiAccess();
        expandDetails();
        assertThat(mining(1)).isEqualTo(6);
        enter("industry-expansion", 2);
        assertThat(mining(1)).isEqualTo(4);
        test(find(Checkbox.class).withId("industry-expansion-pin").single()).click();
        enter("industry-mining", 6);
        assertThat(field("industry-expansion").getValue()).isEqualTo(2);
        assertThat(field("industry-mining").getValue()).isEqualTo(4);
        assertThat(mining(1)).isEqualTo(4);
        test(find(Checkbox.class).withId("industry-expansion-pin").single()).click();
        click("production-preset-ships");
        assertThat(mining(1)).isZero();
    }

    private void assertExhaustionAndHistory() {
        reloadMap();
        assertThat(text("metal-details")).contains("3 Punkte → 3 Schiffe/Runde");
        assertThat(text("app-header-stats")).contains("2 (−2)");
        nextRound();
        reloadMap();
        assertThat(text("system-background")).contains("Metallvorkommen: 0");
        assertThat(text("app-header-stats")).contains("0 (");
        assertThat(text("metal-details")).contains("→ 0 von 4 Schiffen/Runde · Metall fehlt");
        assertThat(garrison()).isEqualTo(15);
        assertThat(games.allocateProduction(id, 1, 2, 1, 1, 0)).isTrue();
        nextRound();
        assertThat(garrison()).isEqualTo(15);
        registry.writeState(id, state -> { state.endGame(1); return null; });
        reloadMap();
        assertThat(text("metal-details")).contains("0 Punkte × 3 → 0 Metall/Runde");
        reviewFirstRound();
        assertThat(text("system-background")).contains("Metallvorkommen: 0");
        assertThat(text("metal-details")).contains("2 Punkte → 0 Metall/Runde · Vorkommen erschöpft");
        assertThat(find(IntegerField.class).withId("industry-mining").all()).isEmpty();
        assertArchived("Metal Browserless");
    }

    private Input miningSlider() {
        return find(Input.class).withAriaLabel("Metall fördern").single();
    }

    @SuppressWarnings("unchecked") // rationale: the language switcher contains a ComboBox<Locale>.
    private void changeLanguage(Locale locale) {
        var switcher = find(Component.class).withClassName("lang-switcher").single();
        ComboBox<Locale> combo = find(ComboBox.class, switcher).single();
        test(combo).selectItem(locale.equals(Locale.ENGLISH) ? "Englisch" : "German");
        reloadMap();
    }
}
