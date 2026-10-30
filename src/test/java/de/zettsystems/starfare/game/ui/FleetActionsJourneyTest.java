package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.testsupport.DomEvents;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FleetActionsJourneyTest extends AbstractMapJourneyTest {
    @Test
    void cancellationPreservesSourceAndSuccessfulDispatchKeepsDetails() {
        createReviewGame();
        openSend("Alpha", "Beta", false);
        dialogClick("Abbrechen");
        assertTab("Details");
        assertThat(text("sys-selected")).contains("Alpha");
        assertThat(view().plannedOrders()).isEmpty();
        select("Beta");
        dialogClick("Flotte senden");
        assertTab("Details");
        assertThat(text("system-details-heading")).contains("Alpha");
        openSend("Alpha", "Beta", false);
        dialogClick("Flotte senden");
        assertThat(view().plannedOrders()).hasSize(2);
        assertTab("Details");
        selectTab("Befehle");
        test(find(Button.class).withText("Stornieren").atIndex(1)).click();
        runPendingUiAccess();
        assertThat(view().plannedOrders()).hasSize(1);
        click("Letzten Befehlschritt zurücknehmen");
        assertThat(view().plannedOrders()).hasSize(2);
        assertDirectActions();
    }

    private void assertDirectActions() {
        selectTab("Flotten");
        click("Warten");
        assertThat(view().waitingFleetIds()).hasSize(1);
        assertTab("Flotten");
        selectCard("fleet-travel-card", "Alpha");
        assertTab("Details");
        assertThat(find(Button.class).withText("Wartet").single().isEnabled()).isFalse();
        click("Auflösen");
        assertThat(view().plannedOrders()).hasSize(4);
        assertThat(view().ownFleets()).hasSize(1);
        assertTab("Details");
        click("Letzten Befehlschritt zurücknehmen");
        assertThat(view().plannedOrders()).hasSize(3);
    }

    @Test
    void systemAndFleetSelectionReplaceDetailsAndContextClickSelectsSource() {
        createReviewGame();
        select("Alpha");
        assertThat(text("system-details-heading")).contains("Alpha");
        selectTab("Flotten");
        selectCard("fleet-travel-card", "Alpha");
        assertThat(viewText()).contains("Reisedetails");
        assertThat(find(com.vaadin.flow.component.Component.class).withClassName("system-metric").all()).isEmpty();
        select("Alpha");
        test(find(com.vaadin.flow.component.html.Div.class).withClassName("fleet-badge").single()).click();
        runPendingUiAccess();
        assertThat(viewText()).contains("Reisedetails");
        DomEvents.fire(mapSystem("Alpha"), "contextmenu");
        runPendingUiAccess();
        assertThat(text("sys-selected")).contains("Alpha");
        assertTab("Details");
    }
}
