package de.zettsystems.starfare.game.ui;

import de.zettsystems.gamesplatformapi.PlatformSettings;
import de.zettsystems.starfare.game.values.DisplaySetting;
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
}
