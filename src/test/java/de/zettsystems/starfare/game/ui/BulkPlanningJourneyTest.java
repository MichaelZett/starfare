package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.legal.ui.ImprintView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulkPlanningJourneyTest extends AbstractMapJourneyTest {
    @Test
    void reservesBulkDispatchEditingAndUndoUseBothSelectedSystems() {
        createReviewGame();
        click("Reserven gesammelt setzen");
        selectSources();
        enterAmount(1);
        dialogClick("Speichern");
        assertThat(view().systems().stream().filter(s -> s.id() == 1 || s.id() == 2))
                .allMatch(s -> Integer.valueOf(1).equals(s.garrisonReserve()));
        click("Flotten gesammelt senden");
        selectSources();
        selectTarget("Gamma");
        dialogClick("Flotten gesammelt senden");
        assertTab("Befehle");
        assertThat(view().plannedOrders()).hasSize(2);
        cardAction("fleet-travel-card-planned", "Alpha → Gamma", "Befehl bearbeiten");
        selectTarget("Beta");
        enterAmount(2);
        dialogClick("Speichern");
        assertThat(view().plannedOrders().getFirst().ships()).isEqualTo(2);
        assertThat(viewText()).contains("Alpha → Beta");
        click("Letzten Befehlschritt zurücknehmen");
        assertThat(view().plannedOrders().getFirst().ships()).isEqualTo(4);
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

    @SuppressWarnings("unchecked") // rationale: the planning dialog contains a MultiSelectComboBox<VisibleSystem>.
    private void selectSources() {
        MultiSelectComboBox<VisibleSystem> sources = find(MultiSelectComboBox.class, openedDialog()).single();
        test(sources).selectItem("Alpha", "Beta");
        assertThat(sources.getValue()).hasSize(2);
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
