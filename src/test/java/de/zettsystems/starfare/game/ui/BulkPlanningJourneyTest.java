package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlannedDeparture;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.testsupport.DomEvents;
import de.zettsystems.starfare.legal.ui.ImprintView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulkPlanningJourneyTest extends AbstractMapJourneyTest {
    @Test
    void jointAttackPicksSourcesOnTheBadgesAndStartsTheFasterOneLater() {
        createReviewGame();
        int turn = view().turn();
        select("Gamma");
        assertThat(find(Div.class).withClassName("sys-arrival").all())
                .as("a badge with the arrival round at each own system with ships, without any arrival to meet")
                .hasSize(2);
        assertThat(viewText()).contains("Gemeinsam angreifen: Rundenabzeichen eigener Systeme anklicken");

        pickSource("Alpha");
        pickSource("Beta");
        assertThat(find(Div.class, mapSystem("Beta")).withClassName("sys-arrival-selected").all()).hasSize(1);
        click("Gemeinsam angreifen (Ankunft R" + (turn + 15) + ")");
        assertThat(elementText(openedDialog().getElement())).as("the faster source starts three rounds later")
                .contains("Start sofort · Ankunft Runde " + (turn + 15))
                .contains("Start in Runde " + (turn + 3) + " (+3) · Ankunft Runde " + (turn + 15));
        IntegerField beta = find(IntegerField.class, openedDialog()).withCondition(field -> "Beta".equals(field.getLabel())).single();
        assertThat(beta.getValue()).as("all available ships by default").isEqualTo(3);
        test(beta).setValue(2);
        roundTrip();
        dialogClick("Angriff planen");

        assertThat(view().plannedOrders()).singleElement()
                .matches(order -> Integer.valueOf(1).equals(order.fromSystemId()) && Integer.valueOf(5).equals(order.ships()));
        assertThat(view().plannedDepartures()).singleElement()
                .isEqualTo(new PlannedDeparture(2, 3, "Beta", "Gamma", 2, turn + 3, turn + 15, null));
        VisibleSystem reserved = view().systems().stream().filter(system -> system.id() == 2).findFirst().orElseThrow();
        assertThat(reserved.garrison()).as("reserved ships still defend").isEqualTo(3);
        assertThat(reserved.availableShips()).as("but cannot be sent twice").isEqualTo(1);
        assertTab("Flotten");
        assertThat(elementText(card("fleet-travel-card-departure", "Beta → Gamma").getElement()))
                .contains("Start: Runde " + (turn + 3) + " · Ankunft: Runde " + (turn + 15));

        cardAction("fleet-travel-card-departure", "Beta → Gamma", "Stornieren");
        assertThat(view().plannedDepartures()).isEmpty();
        click("Letzten Befehlschritt zurücknehmen");
        assertThat(view().plannedDepartures()).as("undo brings the cancelled start back").hasSize(1);
    }

    @Test
    void editingAndUndoOfAQueuedSendKeepTheTravelList() {
        createReviewGame();
        assertThat(games.sendFleet(id, player, 1, 3, 1)).isTrue();
        reloadMap();
        selectTab("Flotten");
        cardAction("fleet-travel-card-planned", "Alpha → Gamma", "Befehl bearbeiten");
        selectTarget("Beta");
        enterAmount(2);
        dialogClick("Speichern");
        assertThat(view().plannedOrders().getFirst().ships()).isEqualTo(2);
        assertThat(viewText()).contains("Alpha → Beta");
        click("Letzten Befehlschritt zurücknehmen");
        assertThat(view().plannedOrders().getFirst().ships()).isEqualTo(1);
        assertThat(viewText()).contains("Alpha → Gamma");
        selectTab("Flotten");
        assertThat(viewText()).contains("Gemeinsame Ankünfte");
    }

    @Test
    void routeFilterSurvivesNewAuthenticationAndRematchReservesTheHumanSeat() {
        createReviewGame();
        selectTab("Flotten");
        test(find(TextField.class).single()).setValue("Gamma");
        runPendingUiAccess();
        assertThat(find(com.vaadin.flow.component.html.Div.class).withClassName("fleet-travel-card").all()).isEmpty();
        navigate(ImprintView.class);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        loginAs(email);
        openMap();
        selectTab("Flotten");
        assertThat(find(TextField.class).single().getValue()).isEqualTo("Gamma");
        var before = registry.listIds();
        navigate(LobbyView.class);
        openTemplate();
        test(find(Checkbox.class, openedDialog()).single()).click();
        dialogClick("Neues Spiel");
        var copy = registry.listIds().stream().filter(candidate -> !before.contains(candidate)).findFirst().orElseThrow();
        int reservations = registry.readState(copy, state -> state.invitedSeats().size());
        boolean started = registry.readState(copy, GameState::started);
        assertThat(reservations).isEqualTo(1);
        assertThat(started).isFalse();
        store.delete(copy);
    }

    /** The round badge sits inside the system dot and has its own click target. */
    private void pickSource(String system) {
        DomEvents.fire(find(Div.class, mapSystem(system)).withClassName("sys-arrival").single(), "click");
        runPendingUiAccess();
    }

    @SuppressWarnings("unchecked") // rationale: the planning target is a ComboBox<VisibleSystem>.
    private void selectTarget(String name) {
        ComboBox<VisibleSystem> target = find(ComboBox.class, openedDialog()).single();
        test(target).selectItem(name);
    }

    @SuppressWarnings("unchecked") // rationale: lobby card renderers use a private row type.
    private void openTemplate() {
        GridTester<Grid<Object>, Object> grid = test(find(Grid.class).single());
        var row = grid.getCellComponent(0, 0);
        test(find(com.vaadin.flow.component.button.Button.class, row).withText("Als Vorlage / Revanche").single()).click();
    }
}
