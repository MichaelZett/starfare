package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.legal.ui.ImprintView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A spectator picks whose view to follow and whether the fog of war applies; neither touches the game. */
class SpectatorJourneyTest extends AbstractMapJourneyTest {
    private static final String HOST = "Host Empire (Review Host)";
    private static final String GUEST = "Guest Empire (Review Guest)";

    private String spectator = "";

    @BeforeEach
    void aSpectatorWatchesASharedGame() {
        createSharedGame();
        navigate(ImprintView.class);
        loginAsNewPlayer("Spectator");
        spectator = UserContext.currentPlayerId().orElseThrow();
        assertThat(games.observeGame(id, spectator)).isTrue();
        openMap();
    }

    @Test
    void theSpectatorStartsWithTheFirstEmpireBehindTheFogOfWar() {
        assertThat(perspective().isVisible()).isTrue();
        assertThat(perspective().getValue()).extracting(Player::label).isEqualTo(HOST);
        assertThat(fog().getValue()).isTrue();

        assertThat(visible("Alpha")).as("the host's own system").isTrue();
        assertThat(visible("Gamma")).as("the guest's distant system").isFalse();
    }

    @Test
    void anotherPerspectiveShowsThatEmpiresView() {
        test(perspective()).selectItem(GUEST);
        runPendingUiAccess();

        assertThat(visible("Gamma")).isTrue();
        assertThat(visible("Alpha")).isFalse();
        assertThat(perspective().getValue()).extracting(Player::label).as("kept across the refresh").isEqualTo(GUEST);
        assertThat(games.isObserver(id, spectator)).isTrue();
        assertThat(games.seatFor(id, spectator)).as("watching takes no seat").isEmpty();
    }

    @Test
    void withoutTheFogOfWarEverySystemIsVisible() {
        test(fog()).click();
        runPendingUiAccess();

        assertThat(fog().getValue()).isFalse();
        assertThat(visible("Alpha")).isTrue();
        assertThat(visible("Gamma")).isTrue();
    }

    @Test
    void reopeningTheMapStartsAgainFromTheFirstEmpireWithFog() {
        test(perspective()).selectItem(GUEST);
        test(fog()).click();
        runPendingUiAccess();

        reloadMap();

        assertThat(perspective().getValue()).extracting(Player::label).isEqualTo(HOST);
        assertThat(fog().getValue()).isTrue();
        assertThat(visible("Gamma")).isFalse();
    }

    private boolean visible(String system) {
        return mapSystem(system).hasClassName("sys-own");
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed, the spectator's selection is.
    private ComboBox<Player> perspective() {
        return find(ComboBox.class).withId("spectator-perspective").single();
    }

    private Checkbox fog() {
        return find(Checkbox.class).withId("spectator-fog").single();
    }
}
