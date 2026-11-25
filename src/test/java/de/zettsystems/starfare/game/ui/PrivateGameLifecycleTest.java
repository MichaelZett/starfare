package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.standalone.ui.ArchiveView;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.application.InvitationService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.legal.ui.ImprintView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A private game the host only hosts: hidden from others until published or an invitation, then played to the
 * end and only readable in the archive, with switchable perspective and fog of war.
 */
class PrivateGameLifecycleTest extends AbstractBrowserlessTest {
    private static final String NAME = "Private lifecycle";

    @Autowired
    private InvitationService invitations;
    @Autowired
    private Broadcaster broadcaster;
    @Autowired
    private GameSessionStore store;

    private GameId id = GameId.of("unset");
    private String hostEmail = "";
    private String host = "";
    private String guestEmail = "";
    private String guest = "";

    @BeforeEach
    void aPrivateGameWithoutTheHost() {
        guestEmail = loginAsNewPlayer("Lobby Guest");
        guest = UserContext.currentPlayerId().orElseThrow();
        navigate(ImprintView.class);
        hostEmail = loginAsNewPlayer("Lobby Host");
        host = UserContext.currentPlayerId().orElseThrow();
        GameSetup setup = GameSetup.defaults().seatPlayers(1, 1).chooseGalaxy(20, de.zettsystems.starfare.game.values.GalaxyLayout.EVEN);
        id = games.newGame(setup, host, NAME);
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PRIVATE);
    }

    @AfterEach
    void removeTheGame() {
        store.delete(id);
    }

    @Test
    void othersSeeThePrivateGameOnlyOncePublished() {
        signIn(guestEmail);
        assertThat(lobbyRows()).isEmpty();
        navigate("starfare/map/" + id.value(), LobbyView.class);

        signIn(hostEmail);
        navigate("starfare/match/" + id.value(), WaitingRoomView.class);
        test(button("Verwalten")).click();
        Dialog manage = openedDialog();
        test(find(Checkbox.class, manage).single()).click();
        test(find(Button.class, manage).withText("Schließen").single()).click();
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PUBLIC);
        assertThat(viewText()).contains("Öffentlich");

        signIn(guestEmail);
        assertThat(lobbyRows()).anyMatch(row -> row.contains(NAME));
    }

    @Test
    void anInvitedGuestPlaysToTheEndAndReviewsTheGameReadOnly() {
        assertThat(invitations.inviteUser(id, host, guest)).isPresent();
        signIn(guestEmail);
        assertThat(lobbyRows()).anyMatch(row -> row.contains(NAME));
        navigate("starfare/match/" + id.value(), WaitingRoomView.class);
        test(button("Annehmen")).click();
        assertThat(games.seatFor(id, guest)).isPresent();

        assertThat(games.startGame(id)).isTrue();
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });
        broadcaster.publish(new GameEvent.GameFinished(id, 1));
        runPendingUiAccess();

        navigate(LobbyView.class);
        assertThat(lobbyRows()).noneMatch(row -> row.contains(NAME));
        openFromArchive();
        assertThat(getCurrentView()).isInstanceOf(MainView.class);
        assertThat(find(Button.class).withText("Runde abgeben").all()).isEmpty();
        assertThat(find(Button.class).withText("Verlegungen verwalten").all()).isEmpty();
        Button template = find(Button.class).withId("review-template").single();
        assertThat(template.getText()).as("the review offers a new game from this one").isEqualTo("Als Vorlage / Revanche");
        assertThat(template.isEnabled()).isTrue();

        switchPerspectiveAndFog();
        chooseSidebarSections();
    }

    private void openFromArchive() {
        navigate(ArchiveView.class);
        GridTester<Grid<Object>, Object> archive = archive();
        int row = java.util.stream.IntStream.range(0, archive.size())
                .filter(index -> elementText(archive.getCellComponent(index, 0).getElement()).contains(NAME))
                .findFirst().orElseThrow();
        test(find(Button.class, archive.getCellComponent(row, 0)).withText("Ansehen").single()).click();
        runPendingUiAccess();
    }

    private void switchPerspectiveAndFog() {
        Checkbox fog = find(Checkbox.class).withId("review-fog").single();
        test(fog).click();
        runPendingUiAccess();
        assertThat(find(Div.class).withClassName("sys-fog").all()).isNotEmpty();

        Player ai = games.reviewFor(id, guest).orElseThrow().players().stream()
                .filter(Player::ai).findFirst().orElseThrow();
        test(perspective()).selectItem(ai.label());
        runPendingUiAccess();
        assertThat(perspective().getValue().id()).isEqualTo(ai.id());
        String aiHome = games.reviewFor(id, guest, ai.id(), true).orElseThrow().systems().stream()
                .filter(system -> Objects.equals(system.ownerId(), ai.id())).findFirst().orElseThrow().name();
        assertThat(find(Div.class).withClassName("sys-own").all())
                .anyMatch(system -> system.getText().startsWith(aiHome));

        test(find(Checkbox.class).withId("review-fog").single()).click();
        runPendingUiAccess();
        assertThat(find(Div.class).withClassName("sys-fog").all()).isEmpty();
    }

    private void chooseSidebarSections() {
        selectTab("Details");
        assertThat(viewText()).contains("System oder Flotte auf der Karte auswählen.");
        selectTab("Flotten");
        assertThat(viewText()).contains("Eigene Flotten");
        selectTab("Verlegungen");
        assertThat(viewText()).contains("Produktionsverlegungen");
        selectTab("Bericht");
        assertThat(find(com.vaadin.flow.component.tabs.Tabs.class).withClassName("map-sidebar-tabs").single()
                .getSelectedTab().getLabel()).isEqualTo("Bericht");
    }

    private void signIn(String email) {
        navigate(ImprintView.class);
        loginAs(email);
        navigate(LobbyView.class);
    }

    @SuppressWarnings("unchecked") // rationale: the lobby's row type is private; its rendered cells are public components.
    private List<String> lobbyRows() {
        if (find(Grid.class).all().isEmpty()) {
            return List.of();
        }
        GridTester<Grid<Object>, Object> grid = test(find(Grid.class).single());
        return java.util.stream.IntStream.range(0, grid.size())
                .mapToObj(row -> elementText(grid.getCellComponent(row, 0).getElement())).toList();
    }

    @SuppressWarnings("unchecked") // rationale: archive cells are rendered components; their row type is immaterial here.
    private GridTester<Grid<Object>, Object> archive() {
        return test(find(Grid.class).single());
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed, the perspective holds players.
    private ComboBox<Player> perspective() {
        return find(ComboBox.class).withId("review-perspective").single();
    }
}
