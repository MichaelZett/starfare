package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.testsupport.RecyclingStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecyclingJourneyTest extends AbstractEconomyUiTest {
    @Test
    void editingRecyclingPreservesFractionsAndCreditsEachOrderOnlyOnce() {
        openGame(RecyclingStates.ready(), "Recycling Browserless");
        recycle(1);
        assertThat(text("recycling-details")).contains("Vorgemerkt: 1 Schiffe");
        recycle(0);
        assertThat(text("recycling-details")).doesNotContain("Vorgemerkt:");
        recycle(1);
        assertThat(garrison()).isEqualTo(12);
        reloadMap();
        assertThat(text("recycling-details")).contains("Vorgemerkt: 1 Schiffe");
        nextRound();
        reloadMap();
        assertThat(text("recycling-details")).contains("Gespeicherter Metallrest: 0,7").doesNotContain("Vorgemerkt:");
        assertThat(garrison()).isEqualTo(11);
        recycle(9);
        assertThat(text("recycling-details")).contains("Vorgemerkt: 9 Schiffe");
        assertThat(text("metal-details")).contains("7 Punkte → 4 von 7 Schiffen/Runde · Metall fehlt");
        nextRound();
        reloadMap();
        assertThat(text("recycling-details")).contains("Gespeicherter Metallrest: 0,0");
        assertThat(garrison()).isEqualTo(6);
        nextRound();
        assertThat(garrison()).isEqualTo(6);
        finishAndReviewFirstRound();
        assertThat(text("recycling-details")).contains("Gespeicherter Metallrest: 0,7");
        assertThat(find(IntegerField.class).withId("recycling-ships").all()).isEmpty();
    }

    private void recycle(int ships) {
        expandDetails();
        click("recycling-open");
        enter("recycling-ships", ships);
        click("recycling-apply");
    }
}
