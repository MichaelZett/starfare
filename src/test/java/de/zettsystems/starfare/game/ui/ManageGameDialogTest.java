package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.testsupport.TestPlatformSocial;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ManageGameDialogTest extends AbstractBrowserlessTest {
    private static final String FRIEND_NAME = "Invited Friend";

    @Autowired
    private PlayerDirectory players;
    @Autowired
    private TestPlatformSocial social;

    private String host = "";
    private String friend = "";
    private GameId id = GameId.of("unset");

    @BeforeEach
    void openTheManageDialog() {
        String friendEmail = "browserless-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(friendEmail, FRIEND_NAME);
        friend = String.valueOf(accounts.findByEmail(friendEmail).orElseThrow().id());
        loginAsNewPlayer("Manage Host");
        host = UserContext.currentPlayerId().orElseThrow();
        social.befriend(AccountId.of(Long.parseLong(host)), AccountId.of(Long.parseLong(friend)));
        GameSetup setup = GameSetup.defaults().seatPlayers(2, 0).chooseGalaxy(20, de.zettsystems.starfare.game.values.GalaxyLayout.EVEN);
        id = games.newGame(setup, host, "Managed game");
        String name = players.displayName(host);
        assertThat(games.joinGame(id, host, name, name + " Empire")).isPresent();
        navigate("starfare/match/" + id.value(), WaitingRoomView.class);
        test(button("Verwalten")).click();
    }

    @AfterEach
    void forgetFriendships() {
        social.reset();
    }

    @Test
    void theHostSeesHumanSeatsAndCannotKickThemselves() {
        Dialog dialog = openedDialog();
        List<Div> rows = find(Div.class, dialog).withClassName("manage-row").all();
        assertThat(rows).hasSize(2);

        assertThat(label(rows.get(0))).isEqualTo("Manage Host");
        assertThat(kick(rows.get(0)).isEnabled()).isFalse();
        String openSeat = games.summaryOf(id).players().get(1).label();
        assertThat(label(rows.get(1))).isEqualTo(openSeat);
        assertThat(kick(rows.get(1)).isEnabled()).isTrue();
    }

    @Test
    void aJoinedGuestIsListedByNameAndCanBeKicked() {
        assertThat(games.changeVisibility(id, host, GameVisibility.PUBLIC)).isTrue();
        String guestEmail = "browserless-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(guestEmail, "Kicked Guest");
        String guest = String.valueOf(accounts.findByEmail(guestEmail).orElseThrow().id());
        assertThat(games.joinGame(id, guest, "Kicked Guest", "Guest Empire")).isPresent();
        runPendingUiAccess();

        Div guestRow = find(Div.class, openedDialog()).withClassName("manage-row").all().get(1);
        assertThat(label(guestRow)).isEqualTo("Kicked Guest");
        test(kick(guestRow)).click();
        runPendingUiAccess();

        assertThat(games.summaryOf(id).seatByPlayer()).doesNotContainKey(guest);
        assertThat(notifications()).isEmpty();
    }

    @Test
    void invitingWithoutAChoiceFails() {
        invite();

        assertThat(notifications()).containsExactly("Einladung nicht möglich");
        assertThat(games.summaryOf(id).invitedSeats()).isEmpty();
    }

    @Test
    void invitingAFriendReservesASeatAndClearsTheChoice() {
        ComboBox<String> combo = inviteCombo();
        test(combo).selectItem(FRIEND_NAME);

        invite();

        assertThat(notifications()).containsExactly("Eingeladen: " + FRIEND_NAME);
        assertThat(games.summaryOf(id).invitedSeats()).containsOnlyKeys(friend);
        assertThat(combo.getValue()).isNull();
        runPendingUiAccess();
        assertThat(elementText(openedDialog().getElement())).contains(FRIEND_NAME, "Zurücknehmen");
        assertThat(test(combo).getSuggestionItems()).doesNotContain(friend);
    }

    @Test
    void invitingAFriendWhoHidesFromTheHostFails() {
        test(inviteCombo()).selectItem(FRIEND_NAME);
        social.hide(AccountId.of(Long.parseLong(friend)), AccountId.of(Long.parseLong(host)));

        invite();

        assertThat(notifications()).containsExactly("Einladung nicht möglich");
        assertThat(games.summaryOf(id).invitedSeats()).isEmpty();
        assertThat(inviteCombo().getValue()).as("the choice stays").isEqualTo(friend);
    }

    @Test
    void aFriendWhoHidesFromTheHostIsNotOffered() {
        assertThat(test(inviteCombo()).getSuggestionItems()).containsExactly(friend);
        social.hide(AccountId.of(Long.parseLong(friend)), AccountId.of(Long.parseLong(host)));
        openedDialog().close();

        test(button("Verwalten")).click();

        assertThat(test(inviteCombo()).getSuggestionItems()).isEmpty();
    }

    @Test
    void theDialogClosesWhenTheGameIsAborted() {
        Dialog dialog = openedDialog();

        assertThat(games.abortGame(id, host)).isTrue();
        runPendingUiAccess();

        assertThat(dialog.isOpened()).isFalse();
    }

    private void invite() {
        test(find(Button.class, openedDialog()).withText("Einladen").single()).click();
    }

    @SuppressWarnings("unchecked") // rationale: the dialog's combo box holds account ids; the finder is untyped.
    private ComboBox<String> inviteCombo() {
        return find(ComboBox.class, openedDialog()).withLabel("User einladen").single();
    }

    private String label(Div row) {
        return find(Span.class, row).withClassName("manage-row-label").single().getText();
    }

    private Button kick(Div row) {
        return find(Button.class, row).withText("Rauswerfen").single();
    }

    private List<String> notifications() {
        return find(Notification.class).all().stream().map(notification -> test(notification).getText()).toList();
    }
}
