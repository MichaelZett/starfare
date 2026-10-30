package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.testsupport.FleetUiStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationJourneyTest extends AbstractMapJourneyTest {
    @Test
    void routePreviewRefuellingAndStationLossPersistWithoutInventingAnArrival() {
        openGame(FleetUiStates.navigation(), "Navigation Browserless");
        int arrival = 1 + games.routeFor(id, player, 1, 4).orElseThrow().rounds();
        assertThat(text("app-header-empire")).isEqualTo("Navigation Empire");
        assertThat(games.addStandingOrder(id, player, 2, 1, 2)).isTrue();
        reloadMap();
        select("Alpha");
        assertThat(text("system-metric-rate")).contains("4 (+2/Runde)");
        assertThat(text("system-details-owner")).contains("Navigation Empire").doesNotContain("Navigator");
        cardAction("system-transfer-row", "Beta → Alpha", "Aufheben");
        assertThat(view().standingOrders()).singleElement().satisfies(route -> assertThat(route.toSystemId()).isEqualTo(4));
        assertThat(text("system-metric-rate")).contains("4 (+0/Runde)");
        assertPreview(arrival);
        openSend("Alpha", "Delta", false);
        enterAmount(10);
        dialogClick("Flotte senden");
        assertThat(view().plannedOrders()).hasSize(1);
        assertThat(find(Div.class).withClassName("fleet-lane-planned").all()).hasSize(3);
        assertThat(find(Div.class).withClassName("fleet-lane-standing").all()).hasSize(3);
        assertRefuellingAndBlock(arrival);
    }

    private void assertPreview(int arrival) {
        select("Alpha");
        click("Flotte senden");
        assertThat(find(Div.class).withClassName("map-range-circle").all()).hasSize(1);
        assertThat(mapSystem("Beta").getElement().getAttribute("data-reach")).isEqualTo("direct");
        assertThat(mapSystem("Delta").getElement().getAttribute("data-reach")).isEqualTo("stations");
        assertThat(mapSystem("Remote 5").getElement().getAttribute("data-reach")).isEqualTo("none");
        assertThat(mapSystem("Beta").getElement().getProperty("title")).contains("Direkt erreichbar · 100 % der direkten Reichweite");
        select("Delta");
        assertThat(find(Button.class, openedDialog()).withClassName("route-stop").all())
                .extracting(Button::getText).containsExactly("Alpha", "Beta", "Gamma", "Delta");
        int amount = find(IntegerField.class, openedDialog()).single().getValue();
        test(find(Button.class, openedDialog()).withAttribute("data-route-system", "2").single()).click();
        assertThat(find(IntegerField.class, openedDialog()).single().getValue()).isEqualTo(amount);
        assertThat(view().plannedOrders()).isEmpty();
        assertThat(text("send-fleet-duration")).contains("2 Aufenthalten", "Ankunft Runde " + arrival);
        assertThat(text("send-fleet-range")).contains("Nur über Stationen · 300 % der direkten Reichweite");
        dialogClick("Abbrechen");
        openSend("Alpha", "Remote 5", false);
        assertThat(text("send-fleet-duration")).contains("Kein erreichbarer Weg");
        assertThat(text("send-fleet-range")).contains("Kein Weg ·");
        assertThat(find(Button.class, openedDialog()).withText("Flotte senden").single().isEnabled()).isFalse();
        dialogClick("Abbrechen");
    }

    private void assertRefuellingAndBlock(int arrival) {
        advance();
        int stop = registry.readState(id, state -> state.fleets().getFirst().arrivalTurn());
        while (view().turn() < stop) { advance(); }
        reloadMap();
        selectTab("Flotten");
        assertThat(viewText()).contains("Auftanken in Beta", "Ankunftsrunde: " + arrival);
        find(Details.class).all().forEach(details -> details.setOpened(true));
        assertThat(elementText(card("fleet-travel-card", "✦ #1").getElement())).contains("Gamma");
        assertThat(registry.<Boolean>readState(id, state -> state.fleets().getFirst().inFlight())).isFalse();
        advance();
        assertThat(registry.<Boolean>readState(id, state -> state.fleets().getFirst().inFlight())).isFalse();
        registry.writeState(id, state -> { state.updateSystem(2, system -> system.captureBy(2, 1)); return null; });
        advance();
        reloadMap();
        selectTab("Flotten");
        assertThat(viewText()).contains("Weiterreise aus Beta unterbrochen", "Ankunft offen");
        selectCard("fleet-travel-card", "✦ #1");
        assertThat(viewText()).contains("Weiterreise aus Beta unterbrochen");
        assertThat(registry.<Boolean>readState(id, state -> state.fleets().getFirst().journey().blocked())).isTrue();
        selectTab("Verlegungen");
        assertThat(viewText()).contains("Verlegung pausiert");
        reloadMap();
        selectTab("Verlegungen");
        assertThat(viewText()).contains("Verlegung pausiert");
    }

    private void advance() {
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        runPendingUiAccess();
    }
}
