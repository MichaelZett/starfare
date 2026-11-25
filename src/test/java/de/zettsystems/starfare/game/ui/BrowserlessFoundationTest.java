package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.legal.ui.ImprintView;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserlessFoundationTest extends AbstractBrowserlessTest {

    @Test
    void switchingAccountsReevaluatesPrivateLobbyAccess() {
        String host = loginAsNewPlayer("Private Host");
        reviewGames.create(host, null);
        navigate(LobbyView.class);
        assertLobbyRows(1);

        navigate(ImprintView.class);
        loginAsNewPlayer("Private Guest");
        navigate(LobbyView.class);
        assertLobbyRows(0);

        navigate(ImprintView.class);
        loginAs(host);
        navigate(LobbyView.class);
        assertLobbyRows(1);
    }

    @SuppressWarnings("unchecked") // rationale: the lobby's row type is private; its rendered cells are public components.
    private void assertLobbyRows(int expected) {
        if (expected == 0) {
            assertThat(find(Grid.class).all()).isEmpty();
            return;
        }
        GridTester<Grid<Object>, Object> grid = test(find(Grid.class).single());
        assertThat(grid.size()).isEqualTo(expected);
        assertThat(elementText(grid.getCellComponent(0, 0).getElement())).contains("Review regression");
    }

    @Test
    void mapClickSendsAnOrderAndProcessesTheRoundBroadcast() {
        String email = loginAsNewPlayer("Map Tester");
        var game = reviewGames.create(email, null);
        navigate(MainView.class, Map.of("gameId", game.id().value()));
        test(mapSystem("Alpha")).click();
        test(button("Flotte senden")).click();
        test(mapSystem("Beta")).click();
        var dialog = openedDialog();
        test(find(Button.class, dialog).withText("Flotte senden").single()).click();
        runPendingUiAccess();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(games.viewFor(game.id(), game.hostSeat()).plannedOrders()).hasSize(1);
        selectTab("Flotten");
        assertThat(viewText()).contains("Alpha", "Beta");

        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        assertThat(games.viewFor(game.id(), game.hostSeat()).turn()).isEqualTo(2);
        assertThat(viewText()).contains("Runde 2");
    }

    @Test
    void creatingASinglePlayerGameNavigatesToItsMap() {
        loginAsNewPlayer("Wizard Tester");
        navigate(LobbyView.class);
        test(find(Button.class).withText("Neues Spiel").atIndex(1)).click();
        var dialog = openedDialog();
        test(find(ComboBox.class, dialog).withId("wizard-players").single()).selectItem("2");
        find(TextField.class, dialog).withLabel("Name des Imperiums").single().setValue("Test Empire");
        find(RadioButtonGroup.class, dialog).withId("randomness").single().setValue(RandomnessLevel.HIGH);
        test(find(Button.class, dialog).withText("Spiel anlegen").single()).click();
        runPendingUiAccess();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(getCurrentView()).isInstanceOf(MainView.class);
        assertThat(viewText()).contains("Runde 1", "Test Empire");
        int randomness = registry.readState(games.listGames().getLast(),
                state -> state.originalSetup().orElseThrow().combatRandomnessPercent());
        assertThat(randomness).isEqualTo(20);
    }
}
