package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.legal.ui.ImprintView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A guest opens the join dialog from the waiting room of a public game with one free seat. */
class JoinGameDialogTest extends AbstractBrowserlessTest {

    @Autowired
    private PlayerDirectory players;

    private GameId id = GameId.of("unset");
    private String guest = "";

    @BeforeEach
    void openTheJoinDialog() {
        loginAsNewPlayer("Join Host");
        String host = UserContext.currentPlayerId().orElseThrow();
        GameSetup setup = new GameSetup(20, 2, 0, List.of(5, 5), 2, 5, 6, false, false, List.of(), null, null);
        id = games.newGame(setup, host, "Join test");
        String hostName = players.displayName(host);
        assertThat(games.joinGame(id, host, hostName, hostName + " Empire")).isPresent();
        assertThat(games.changeVisibility(id, host, GameVisibility.PUBLIC)).isTrue();

        navigate(ImprintView.class);
        loginAsNewPlayer("Join Guest");
        guest = UserContext.currentPlayerId().orElseThrow();
        navigate("starfare/match/" + id.value(), WaitingRoomView.class);
        test(button("Beitreten")).click();
    }

    @Test
    void showsTheGameItsFreeSeatAndSuggestsAnEmpireName() {
        Dialog dialog = openedDialog();
        String freeSeat = games.summaryOf(id).players().get(1).label();

        assertThat(elementText(dialog.getElement()))
                .contains("Dein Imperium", "Partie: Join test", "Freie Sitze: " + freeSeat);
        assertThat(empireName(dialog).getValue()).isEqualTo("Join Guest Empire");
    }

    @Test
    void aBlankEmpireNameIsRejectedWithoutJoining() {
        Dialog dialog = openedDialog();
        test(empireName(dialog)).setValue("   ");
        test(confirm()).click();

        assertThat(empireName(dialog).isInvalid()).isTrue();
        assertThat(empireName(dialog).getErrorMessage()).isEqualTo("Bitte vergib einen Imperiumsnamen.");
        assertThat(dialog.isOpened()).isTrue();
        assertThat(games.seatFor(id, guest)).isEmpty();
    }

    @Test
    void theChosenEmpireNameNamesTheNewSeat() {
        Dialog dialog = openedDialog();
        test(empireName(dialog)).setValue("  Sternenbund ");
        test(confirm()).click();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(notifications()).contains("Du bist beigetreten");
        assertThat(games.seatFor(id, guest)).isPresent();
        assertThat(viewText()).contains("Sternenbund (Join Guest)");
    }

    @Test
    void aSeatTakenMeanwhileKeepsTheDialogOpenAndSaysSo() {
        String lateEmail = "browserless-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(lateEmail, "Quick Guest");
        String quick = String.valueOf(accounts.findByEmail(lateEmail).orElseThrow().id());
        assertThat(games.joinGame(id, quick, "Quick Guest", "Quick Empire")).isPresent();

        Dialog dialog = openedDialog();
        test(confirm()).click();

        assertThat(notifications()).contains("Beitritt nicht möglich");
        assertThat(dialog.isOpened()).isTrue();
        assertThat(games.seatFor(id, guest)).isEmpty();
    }

    private TextField empireName(Dialog dialog) {
        return find(TextField.class, dialog).withLabel("Name des Imperiums").single();
    }

    private Button confirm() {
        return find(Button.class).withId("join-game-confirm").single();
    }

    private List<String> notifications() {
        return find(Notification.class).all().stream().map(notification -> test(notification).getText()).toList();
    }
}
