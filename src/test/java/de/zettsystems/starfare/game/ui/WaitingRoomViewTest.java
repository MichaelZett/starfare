package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.legal.ui.ImprintView;
import de.zettsystems.starfare.game.application.InvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaitingRoomViewTest extends AbstractBrowserlessTest {

    @Autowired
    private InvitationService invitations;
    @Autowired
    private PlayerDirectory players;

    @Test
    @SuppressWarnings("unchecked") // rationale: the wizard's radio group is typed, the finder is not.
    void creatingAnOpenMultiplayerGameSeatsTheCreatorAndOpensTheWaitingRoom() {
        loginAsNewPlayer("Wizard Host");
        navigate(LobbyView.class);
        test(find(Button.class).withText("Neues Spiel").atIndex(1)).click();
        var dialog = openedDialog();
        assertThat(dialog.isCloseOnOutsideClick()).as("a stray click keeps the wizard").isFalse();
        find(IntegerField.class, dialog).withLabel("Menschliche Spieler").single().setValue(2);
        find(IntegerField.class, dialog).withLabel("KI-Spieler").single().setValue(0);
        find(RadioButtonGroup.class, dialog).withId("wizard-visibility").single().setValue(GameVisibility.PUBLIC);
        assertThat(find(Checkbox.class, dialog).withId("host-only").single().getValue()).isFalse();
        test(find(Button.class, dialog).withText("Zusammenfassung prüfen").single()).click();
        assertThat(elementText(dialog.getElement())).contains("Wizard Host (du)", "Öffentlich", "Mensch 2");
        test(find(Button.class, dialog).withText("Spiel anlegen").single()).click();
        runPendingUiAccess();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(getCurrentView()).isInstanceOf(WaitingRoomView.class);
        GameId id = games.listGames().getLast();
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PUBLIC);
        assertThat(viewText()).contains("Wizard Host Empire (Wizard Host)", "Freier Platz",
                "Wartet auf Mitspieler: 1 von 2 Plätzen belegt");
        assertThat(button("Starten").isEnabled()).isFalse();
    }

    @Test
    void aGuestJoinsInTheWaitingRoomAndOnlyTheHostStarts() {
        String hostEmail = loginAsNewPlayer("Room Host");
        String host = account();
        GameId id = twoSeatGame(host);
        assertThat(games.changeVisibility(id, host, GameVisibility.PUBLIC)).isTrue();

        navigate(ImprintView.class);
        loginAsNewPlayer("Room Guest");
        navigate("starfare/match/" + id.value(), WaitingRoomView.class);
        test(button("Beitreten")).click();
        test(find(Button.class, openedDialog()).withText("Beitreten").single()).click();

        assertThat(viewText()).contains("Room Guest Empire (Room Guest)",
                "Wartet auf den Start durch den Gastgeber");
        assertThat(((WaitingRoomView) getCurrentView()).shownMatchId()).as("the platform's chat follows the room")
                .contains(id.value());
        assertThat(find(Button.class).withText("Starten").all()).as("only the host starts").isEmpty();

        navigate(ImprintView.class);
        loginAs(hostEmail);
        navigate("starfare/match/" + id.value(), WaitingRoomView.class);
        assertThat(viewText()).contains("Du kannst die Partie starten");
        test(button("Starten")).click();
        runPendingUiAccess();

        assertThat(games.hasStartedGame(id)).isTrue();
        assertThat(getCurrentView()).isInstanceOf(MainView.class);
        assertThat(((MainView) getCurrentView()).shownMatchId()).contains(id.value());
    }

    @Test
    void anInviteeAcceptsInTheWaitingRoomWithName() {
        loginAsNewPlayer("Inviting Host");
        String host = account();
        GameId id = twoSeatGame(host);
        navigate(ImprintView.class);
        loginAsNewPlayer("Invited Friend");
        String guest = account();
        assertThat(invitations.inviteUser(id, host, guest)).isPresent();

        navigate("starfare/match/" + id.value(), WaitingRoomView.class);
        assertThat(viewText()).contains("Eingeladen: Invited Friend");
        test(button("Annehmen")).click();

        assertThat(viewText()).contains("Invited Friend Empire (Invited Friend)");
        assertThat(games.seatFor(id, guest)).isPresent();
    }

    @Test
    void anInvitationAppearsInTheInviteesOpenLobbyWithoutReload() {
        loginAsNewPlayer("Pushing Host");
        String host = account();
        GameId id = twoSeatGame(host);
        navigate(ImprintView.class);
        loginAsNewPlayer("Waiting Friend");
        String guest = account();
        navigate(LobbyView.class);
        assertThat(find(com.vaadin.flow.component.grid.Grid.class).all()).as("the private game is hidden before the invitation").isEmpty();

        assertThat(invitations.inviteUser(id, host, guest)).isPresent();
        runPendingUiAccess();

        // The invitee accepts in the waiting room, as inside a platform host.
        @SuppressWarnings("unchecked") // rationale: the lobby's row type is private; its rendered cells are public components.
        var grid = (com.vaadin.flow.component.grid.Grid<Object>) find(com.vaadin.flow.component.grid.Grid.class).single();
        test(find(Button.class, test(grid).getCellComponent(0, 0)).withText("Warteraum").single()).click();
        assertThat(viewText()).contains("Waiting test", "Annehmen");
    }

    @Test
    void theLobbyCardOfAWaitingGameLeadsToItsWaitingRoom() {
        loginAsNewPlayer("Card Host");
        twoSeatGame(account());
        navigate(LobbyView.class);

        @SuppressWarnings("unchecked") // rationale: the lobby's row type is private; its rendered cells are public components.
        var cell = test((com.vaadin.flow.component.grid.Grid<Object>) find(com.vaadin.flow.component.grid.Grid.class).single())
                .getCellComponent(0, 0);
        test(find(Button.class, cell).withText("Warteraum").single()).click();

        assertThat(getCurrentView()).isInstanceOf(WaitingRoomView.class);
        assertThat(viewText()).contains("Waiting test", "Freier Platz");
    }

    @Test
    void theMapOfAGameThatHasNotStartedLeadsToTheWaitingRoom() {
        loginAsNewPlayer("Early Bird");
        GameId id = twoSeatGame(account());

        navigate("starfare/map/" + id.value(), WaitingRoomView.class);

        assertThat(viewText()).contains("Early Bird Empire");
    }

    @Test
    void strangersDoNotSeeAPrivateWaitingRoom() {
        loginAsNewPlayer("Secret Host");
        GameId id = twoSeatGame(account());
        navigate(ImprintView.class);
        loginAsNewPlayer("Stranger");

        navigate("starfare/match/" + id.value(), LobbyView.class);
    }

    /** Two seats for people, the host on the first; new games are private. */
    private GameId twoSeatGame(String host) {
        GameSetup setup = new GameSetup(20, 2, 0, List.of(5, 5), 2, 5, 6, false, false, List.of(), null, null);
        GameId id = games.newGame(setup, host, "Waiting test");
        String name = players.displayName(host);
        assertThat(games.joinGame(id, host, name, name + " Empire")).isPresent();
        return id;
    }

    private String account() {
        return UserContext.currentPlayerId().orElseThrow();
    }
}
