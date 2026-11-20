package de.zettsystems.starfare.game.ui;

import de.zettsystems.gamesplatformapi.PlatformSettings;
import de.zettsystems.starfare.game.values.DisplaySetting;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayPreferencesTest extends AbstractMapJourneyTest {
    @Autowired private PlatformSettings settings;

    @Test
    void keepsChoicesWithThePlatformAndReadsThemWhenTheMapIsBuiltAgain() {
        createReviewGame();

        DisplayPreferences.choose(DisplaySetting.SPEED, "1.5");
        DisplayPreferences.choose(DisplaySetting.ROUTE_FILTER, "Alpha");
        DisplayPreferences.choose(DisplaySetting.ROUTE_FILTER, "");

        assertThat(settings.value("starfare", "SPEED")).hasValue("1.5");
        assertThat(settings.value("starfare", "ROUTE_FILTER")).as("a blank choice is the default again").isEmpty();
        reloadMap();
        assertThat(DisplayPreferences.number(DisplaySetting.SPEED, 1.0)).isEqualTo(1.5);
        assertThat(DisplayPreferences.get(DisplaySetting.ROUTE_FILTER, "")).isEmpty();
    }

    @Test
    void aChangeOnThisDeviceKeepsTheOtherFiltersAnotherDeviceChoseMeanwhile() {
        createReviewGame();
        settings.set("starfare", "EVENT_FILTERS", "COMBAT");
        settings.set("starfare", "LOGISTICS_FILTERS", "CAPTURES");

        selectTab("Flotten");
        test(find(TextField.class).single()).setValue("Gamma");
        runPendingUiAccess();

        assertThat(settings.value("starfare", "ROUTE_FILTER")).hasValue("Gamma");
        assertThat(settings.value("starfare", "EVENT_FILTERS")).as("this map still shows every category").hasValue("COMBAT");
        assertThat(settings.value("starfare", "LOGISTICS_FILTERS")).hasValue("CAPTURES");
        reloadMap();
        assertThat(DisplayPreferences.get(DisplaySetting.EVENT_FILTERS, "*")).as("the next map reads the other device's choice")
                .isEqualTo("COMBAT");
    }
}
