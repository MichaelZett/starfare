package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.style.values.DeviceLayout;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The phone arrangement of the map: compact header without a chat of its own, bottom bar between map and overview. */
class MobileMapJourneyTest extends AbstractMapJourneyTest {

    @Test
    void theBottomBarSwitchesBetweenMapAndOverviewAndTheHeaderHasNoChat() {
        VaadinSession.getCurrent().setAttribute("starfare.deviceLayout", DeviceLayout.MOBILE);
        createReviewGame();

        var layout = find(MobileMapLayout.class).single();
        var sheet = find(Div.class, layout).withClassName("mobile-map-sheet");
        assertThat(sheet.exists()).as("the map first").isFalse();
        assertThat(find(Button.class).all()).noneMatch(button -> "Partie-Chat".equals(button.getText()));

        test(find(Button.class, layout).withText("Übersicht").single()).click();
        assertThat(find(Div.class, layout).withClassName("mobile-map-sheet").exists()).isTrue();
        assertThat(find(Button.class, layout).withText("Übersicht").single().getElement().getAttribute("aria-pressed"))
                .isEqualTo("true");

        test(find(Button.class, layout).withText("Karte").single()).click();
        assertThat(find(Div.class, layout).withClassName("mobile-map-sheet").exists()).isFalse();
    }
}
