package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.standalone.social.ui.SocialView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The pages a host links instead of Starfare's lobby: a new game; friends and chat only in Starfare's own start. */
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
