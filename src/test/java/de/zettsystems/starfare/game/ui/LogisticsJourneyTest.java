package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsJourneyTest extends AbstractMapJourneyTest {
    @Test
    void relocationCardEditingKeepsTheSelectedTab() {
        createReviewGame();
        openSend("Alpha", "Beta", true);
        dialogClick("Verlegung speichern");
        selectTab("Verlegungen");
        cardAction("fleet-travel-card-relocation", "Alpha → Beta", "Bearbeiten");
        enterAmount(2);
        dialogClick("Verlegung speichern");
        assertTab("Verlegungen");
        assertThat(view().standingOrders()).singleElement().satisfies(route -> assertThat(route.ships()).isEqualTo(2));
    }

    @Test
    void relocationAmountCanExceedGarrisonAndIncomingFlowRaisesTheLimit() {
        createReviewGame();
        openSend("Alpha", "Beta", true);
        enterAmount(20);
        assertAmount(20, 20);
        dialogClick("Hälfte");
        assertAmount(10, 20);
        assertThat(find(Button.class, openedDialog()).all()).extracting(Button::getText)
                .contains("Alle", "Alle außer Produktion").doesNotContain("Doppelt");
        toggleMode();
        assertAmount(5, 5);
        dialogClick("Alle");
        assertAmount(5, 5);
        toggleMode();
        dialogClick("Alle");
        assertAmount(20, 20);
        dialogClick("Verlegung speichern");
        assertThat(view().standingOrders()).singleElement().satisfies(route -> assertThat(route.ships()).isEqualTo(20));
        selectTab("Verlegungen");
        assertThat(text("fleet-travel-card-relocation")).contains("Alpha → Beta", "20");
        assertIncomingFlowAndEditing();
    }

    private void assertIncomingFlowAndEditing() {
        openSend("Beta", "Alpha", true);
        assertThat(text("send-fleet-production")).contains("2 (+20/Runde)");
        enterAmount(22);
        assertAmount(22, 22);
        dialogClick("Abbrechen");
        select("Beta");
        assertThat(text("system-metric-rate")).contains("2 (+20/Runde)");
        assertThat(text("system-details-owner")).contains("Host Empire").doesNotContain("Review Host");
        int fleets = view().ownFleets().size();
        cardAction("system-transfer-row", "Alpha → Beta", "Bearbeiten");
        enterAmount(19);
        dialogClick("Abbrechen");
        assertThat(view().standingOrders().getFirst().ships()).isEqualTo(20);
        cardAction("system-transfer-row", "Alpha → Beta", "Bearbeiten");
        enterAmount(19);
        dialogClick("Verlegung speichern");
        assertThat(view().standingOrders().getFirst().ships()).isEqualTo(19);
        select("Beta");
        cardAction("system-transfer-row", "Alpha → Beta", "Aufheben");
        assertThat(view().standingOrders()).isEmpty();
        assertThat(view().ownFleets()).hasSize(fleets);
        assertThat(text("system-metric-rate")).contains("2 (+0/Runde)");
        assertTab("Details");
    }

    @Test
    void aNormalGameDoesNotPointAtFreeShips() {
        createReviewGame();
        assertThat(idleShipsHint("Beta")).isEmpty();
        assertThat(find(Component.class).withClassName(TutorialMark.CLASS_NAME).all()).as("no visible mark").isEmpty();
    }

    @Test
    void tutorialPointsAtShipsBeyondOneRoundOfProduction() {
        createReviewGame(true);
        assertThat(find(Component.class).withClassName(TutorialMark.CLASS_NAME).all()).hasSize(1);
        assertThat(idleShipsHint("Beta")).contains("3");
        assertThat(idleShipsHint("Alpha")).isEmpty();
    }

    private java.util.Optional<String> idleShipsHint(String system) {
        return mapSystem(system).getChildren().filter(Div.class::isInstance).map(Div.class::cast)
                .filter(child -> child.hasClassName("sys-idle-ships")).map(Div::getText).findFirst();
    }

    @Test
    void systemDetailsSurviveSelectionInBothLanguages() {
        createReviewGame();
        select("Alpha");
        assertThat(text("app-header-empire")).isEqualTo("Host Empire");
        assertThat(text("system-metric-sendable")).contains("Versendbare Schiffe");
        changeLanguage(Locale.ENGLISH);
        select("Alpha");
        assertThat(text("system-metric-sendable")).contains("Sendable ships");
        changeLanguage(Locale.GERMAN);
        select("Alpha");
        assertThat(details("system-operations").isOpened()).isFalse();
        assertThat(details("system-background").isOpened()).isFalse();
        details("system-operations").setOpened(true);
        details("system-background").setOpened(true);
        select("Beta");
        assertThat(details("system-operations").isOpened()).isTrue();
        assertThat(details("system-background").isOpened()).isTrue();
    }

    private void toggleMode() {
        test(find(Checkbox.class, openedDialog()).single()).click();
        roundTrip();
    }

    private Details details(String className) { return find(Details.class).withClassName(className).single(); }

    @SuppressWarnings("unchecked") // rationale: the language switcher contains a ComboBox<Locale>.
    private void changeLanguage(Locale locale) {
        ComboBox<Locale> combo = find(ComboBox.class, find(Component.class).withClassName("lang-switcher").single()).single();
        test(combo).selectItem(locale.equals(Locale.ENGLISH) ? "Englisch" : "German");
        reloadMap();
    }
}
