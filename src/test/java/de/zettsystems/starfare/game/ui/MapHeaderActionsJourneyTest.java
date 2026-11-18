package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
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

/** Header actions (chat, leaving) and clicks on the empty map. */
class MapHeaderActionsJourneyTest extends AbstractMapJourneyTest {
    private static final String CHAT = "Partie-Chat";
    private static final String LEAVE = "Partie verlassen";

    @Test
    void theChatOpensBesideTheMapAndReopensTheSamePanel() {
        createReviewGame();
        assertThat(find(GameChatPanel.class).all()).isEmpty();

        click(CHAT);
        GameChatPanel chat = find(GameChatPanel.class).single();
        Component host = chat.getParent().orElseThrow();
        assertThat(host.hasClassName("map-chat-host")).isTrue();
        assertThat(host.isVisible()).isTrue();

        test(find(Button.class, chat).withText("Schließen").single()).click();
        assertThat(chat.isVisible()).isFalse();
        assertThat(host.isVisible()).isFalse();

        click(CHAT);
        assertThat(find(GameChatPanel.class).single()).isSameAs(chat);
        assertThat(chat.isVisible()).isTrue();
        assertThat(host.isVisible()).isTrue();
    }

    @Test
    void theChatStaysClosedWhenTheGameWasRemovedMeanwhile() {
        createReviewGame();
        // Removed behind the view's back: no event, so the chat button is still offered.
        registry.abortGame(id);

        test(button(CHAT)).click();

        assertThat(find(GameChatPanel.class).all()).isEmpty();
    }

    @Test
    void theChatStaysClosedWhenTheAccountBecameAnObserverMeanwhile() {
        createReviewGame();
        registry.writeState(id, state -> state.observers().add(account));

        test(button(CHAT)).click();

        assertThat(find(GameChatPanel.class).all()).isEmpty();
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
        assertThat(button(CHAT).isEnabled()).as("chat waits for the target").isFalse();

        clickBackground();

        assertThat(button(CHAT).isEnabled()).isTrue();
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
        assertThat(button(CHAT).isEnabled()).isTrue();
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

    /** The tester counts visible entries only; which ones are visible depends on the role. */
    private void leave() {
        MenuBar bar = find(MenuBar.class).withClassName("app-header-menu").single();
        List<String> entries = bar.getItems().getFirst().getSubMenu().getItems().stream()
                .filter(MenuItem::isVisible).map(MenuItem::getText).toList();
        int index = entries.indexOf(isObserverView() ? "Beobachtung beenden" : LEAVE);
        assertThat(index).as("leave entry in %s", entries).isNotNegative();
        MenuBarTester<MenuBar> menu = test(bar);
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
