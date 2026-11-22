package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import com.vaadin.flow.component.notification.Notification;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.legal.ui.ImprintView;
import de.zettsystems.starfare.testsupport.DomEvents;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Header actions (leaving) and clicks on the empty map; the platform shows the chat around the map. */
class MapHeaderActionsJourneyTest extends AbstractMapJourneyTest {
    private static final String CHAT = "Partie-Chat";
    private static final String LEAVE = "Partie verlassen";
    private static final String RESIGN = "Aufgeben";

    @Test
    void theMapHasNoChatOfItsOwnBecauseThePlatformShowsIt() {
        createReviewGame();

        assertThat(find(Button.class).all()).noneMatch(button -> CHAT.equals(button.getText()));
    }

    @Test
    void leavingHandsTheSeatToTheAiAndReturnsToTheLobby() {
        createSharedGame();

        leave();

        assertThat(notifications()).containsExactly(LEAVE);
        assertThat(getCurrentView()).isInstanceOf(LobbyView.class);
        assertThat(registry.readState(id, state -> state.joinedHumanPlayerIds().contains(player)).booleanValue()).isFalse();
    }

    @Test
    void leavingFailsWhenTheSeatWasGivenUpInAnotherSession() {
        createSharedGame();
        // Given up behind the view's back: no event, so the menu still offers to leave.
        registry.writeState(id, state -> state.joinedHumanPlayerIds().remove(player));

        leave();

        assertThat(notifications()).containsExactly("Verlassen nicht möglich");
        assertThat(getCurrentView()).isInstanceOf(MainView.class);
    }

    @Test
    void leavingFailsWhenTheAccountLostItsSeatInAnotherSession() {
        createSharedGame();
        registry.writeState(id, state -> state.seatByUser().remove(account));

        leave();

        assertThat(notifications()).containsExactly("Verlassen nicht möglich");
        assertThat(registry.readState(id, state -> state.joinedHumanPlayerIds().contains(player)).booleanValue())
                .as("nobody left the game").isTrue();
    }

    @Test
    void resigningNeedsAConfirmationAndHandsTheSeatToTheComputerForGood() {
        createSharedGame();
        registry.writeState(id, state -> { state.configureLobby(true, true); return null; });
        reloadMap();

        menuClick(RESIGN);
        ConfirmDialog confirm = find(ConfirmDialog.class).withId("resign-confirm").single();
        assertThat(test(confirm).getText()).contains("nicht zurückkehren");
        test(confirm).cancel();
        assertThat(games.hasActiveGame(id)).isTrue();
        assertThat(registry.readState(id, state -> state.resignedSeats().isEmpty()).booleanValue()).isTrue();

        menuClick(RESIGN);
        test(find(ConfirmDialog.class).withId("resign-confirm").single()).confirm();
        runPendingUiAccess();

        assertThat(notifications()).containsExactly("Du hast aufgegeben; der Computer übernimmt dein Reich.");
        assertThat(getCurrentView()).isInstanceOf(LobbyView.class);
        assertThat(registry.readState(id, state -> state.resignedSeats().contains(player)).booleanValue()).isTrue();
        assertThat(games.joinGame(id, account, "Shared Host", "Host Empire")).as("no way back").isEmpty();
    }

    @Test
    void observersCannotResign() {
        createSharedGame();
        navigate(ImprintView.class);
        loginAsNewPlayer("Map Observer");
        assertThat(games.observeGame(id, UserContext.currentPlayerId().orElseThrow())).isTrue();
        openMap();

        assertThat(menuEntries()).contains("Beobachtung beenden").doesNotContain(RESIGN);
    }

    @Test
    void anObserverEndsObservingAndReturnsToTheLobby() {
        createSharedGame();
        navigate(ImprintView.class);
        loginAsNewPlayer("Map Observer");
        String observer = UserContext.currentPlayerId().orElseThrow();
        assertThat(games.observeGame(id, observer)).isTrue();
        openMap();

        leave();

        assertThat(games.isObserver(id, observer)).isFalse();
        assertThat(getCurrentView()).isInstanceOf(LobbyView.class);
        assertThat(notifications()).isEmpty();
        assertThat(games.hasActiveGame(id)).as("the seated players keep playing").isTrue();
    }

    @Test
    void aBackgroundClickCancelsTheTargetSelection() {
        createReviewGame();
        select("Alpha");
        click("Flotte senden");
        assertThat(find(Component.class).withClassName("sys-selected").all()).as("source picked").isNotEmpty();

        clickBackground();

        assertThat(find(Component.class).withClassName("sys-selected").all()).as("no source").isEmpty();
        select("Beta");
        assertThat(find(Dialog.class).all().stream().filter(Dialog::isOpened)).as("no send dialog").isEmpty();
        assertThat(text("system-details-heading")).contains("Beta");
    }

    @Test
    void aBackgroundClickClearsTheHighlightedFleet() {
        createReviewGame();
        selectTab("Flotten");
        selectCard("fleet-travel-card", "Alpha");
        assertThat(find(Div.class).withClassName("fleet-lane-highlighted").all()).hasSize(1);

        clickBackground();

        assertThat(find(Div.class).withClassName("fleet-lane-highlighted").all()).isEmpty();
    }

    @Test
    void aBackgroundClickClearsTheHighlightedRelocation() {
        createReviewGame();
        assertThat(games.addStandingOrder(id, player, 1, 2, 2)).isTrue();
        reloadMap();
        selectTab("Verlegungen");
        selectCard("fleet-travel-card-relocation", "Alpha → Beta");
        assertThat(find(Div.class).withClassName("standing-order-badge-highlighted").all()).hasSize(1);

        clickBackground();

        assertThat(find(Div.class).withClassName("standing-order-badge-highlighted").all()).isEmpty();
        assertThat(find(Div.class).withClassName("standing-order-badge").all()).hasSize(1);
    }

    @Test
    void aBackgroundClickWithoutAnySelectionKeepsTheInspectedSystem() {
        createReviewGame();
        select("Alpha");

        clickBackground();

        assertThat(text("system-details-heading")).contains("Alpha");
    }

    /** Host and a second human, so leaving does not hand the whole game to the AI. */
    private void createSharedGame() {
        String guest = "browserless-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(guest, "Shared Guest");
        email = loginAsNewPlayer("Shared Host");
        account = UserContext.currentPlayerId().orElseThrow();
        var fixture = reviewGames.create(email, guest);
        id = fixture.id();
        player = fixture.hostSeat();
        openMap();
    }

    private void leave() {
        menuClick(isObserverView() ? "Beobachtung beenden" : LEAVE);
    }

    private List<String> menuEntries() {
        MenuBar bar = find(MenuBar.class).withClassName("app-header-menu").single();
        return bar.getItems().getFirst().getSubMenu().getItems().stream()
                .filter(MenuItem::isVisible).map(MenuItem::getText).toList();
    }

    /** The tester counts visible entries only; which ones are visible depends on the role. */
    private void menuClick(String entry) {
        List<String> entries = menuEntries();
        int index = entries.indexOf(entry);
        assertThat(index).as("%s in %s", entry, entries).isNotNegative();
        MenuBarTester<MenuBar> menu = test(find(MenuBar.class).withClassName("app-header-menu").single());
        menu.clickItem(0, index);
        runPendingUiAccess();
    }

    private boolean isObserverView() {
        return games.isObserver(id, UserContext.currentPlayerId().orElseThrow());
    }

    private void clickBackground() {
        DomEvents.fireFiltered(find(Div.class).withId("map").single(), "click", "event.target === event.currentTarget");
        runPendingUiAccess();
    }

    private List<String> notifications() {
        return find(Notification.class).all().stream().map(notification -> test(notification).getText()).toList();
    }
}
