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
    void jointAttackIsPlannedInTheSidebarAndStartsTheFasterSourceLater() {
        createReviewGame();
        int turn = view().turn();
        select("Gamma");
        assertThat(find(Div.class).withClassName("sys-arrival").all())
                .as("a badge with arrival round and free ships at each own system with ships")
                .hasSize(2)
                .anySatisfy(badge -> assertThat(badge.getText()).isEqualTo("R" + (turn + 15) + " · 5"));
        Div plan = find(Div.class).withClassName("joint-plan").single();
        assertThat(elementText(plan.getElement())).as("the plan opens with the click on the target")
                .contains("Angriff auf Gamma", "Mögliche Quellen", "Quellen wählen");
        assertThat(button("Angriff planen").isEnabled()).as("nothing to plan without a source").isFalse();

        pickSource("Alpha");
        tick("Beta");
        assertThat(find(Div.class, mapSystem("Beta")).withClassName("sys-arrival-selected").all())
                .as("a tick in the plan picks the same source as its badge").hasSize(1);
        plan = find(Div.class).withClassName("joint-plan").single();
        assertThat(elementText(plan.getElement())).as("the faster source starts three rounds later")
                .contains("Gewählt")
                .contains("Start sofort · Ankunft Runde " + (turn + 15))
                .contains("Start in Runde " + (turn + 3) + " (+3) · Ankunft Runde " + (turn + 15))
                .contains("Gemeinsame Ankunft R" + (turn + 15) + " · 8 Schiffe");
        IntegerField beta = find(IntegerField.class, plan)
                .withCondition(field -> field.getAriaLabel().filter("Schiffe Beta"::equals).isPresent()).single();
        assertThat(beta.getValue()).as("all available ships by default").isEqualTo(3);
        test(beta).setValue(2);
        roundTrip();
        click("Angriff planen – 7 Schiffe, Ankunft R" + (turn + 15));

        assertThat(view().plannedOrders()).singleElement()
                .matches(order -> Integer.valueOf(1).equals(order.fromSystemId()) && Integer.valueOf(5).equals(order.ships()));
        assertThat(view().plannedDepartures()).singleElement()
                .isEqualTo(new PlannedDeparture(2, 3, "Beta", "Gamma", 2, turn + 3, turn + 15, null));
        assertThat(find(Div.class).withClassName("joint-plan").all()).as("the plan closes once planned").isEmpty();
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
    void cancellingTheJointAttackPlanDropsItsSources() {
        createReviewGame();
        select("Gamma");
        pickSource("Alpha");
        assertThat(find(Div.class).withClassName("sys-arrival-selected").all()).hasSize(1);

        click("Abbrechen");

        assertThat(find(Div.class).withClassName("joint-plan").all()).isEmpty();
        assertThat(find(Div.class).withClassName("sys-arrival").all()).as("no target, no badges").isEmpty();
        assertThat(view().plannedOrders()).isEmpty();
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

    /** Ticks a possible source in the joint attack plan. */
    private void tick(String system) {
        Div plan = find(Div.class).withClassName("joint-plan").single();
        test(find(Checkbox.class, plan).withCondition(box -> box.getLabel().startsWith(system + " ")).single()).click();
        runPendingUiAccess();
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
