package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The pages a host links instead of Starfare's lobby: a new game, and friends and chat. */
class HostPagesTest extends AbstractBrowserlessTest {

    @Test
    void theNewGamePageOpensTheWizardAndGivingUpLeadsToTheOverview() {
        loginAsNewPlayer("New Page");
        navigate(NewMatchView.class);
        var dialog = openedDialog();
        assertThat(dialog.getHeaderTitle()).isEqualTo("Spiel anlegen");

        test(find(Button.class, dialog).withText("Abbrechen").single()).click();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(getCurrentView()).isInstanceOf(LobbyView.class);
    }

    @Test
    void theSocialPageShowsWhoIsOnlineAndChat() {
        loginAsNewPlayer("Social Page");

        navigate(SocialView.class);

        assertThat(viewText()).contains("Freunde und Chat", "Online", "Chat");
    }
}
