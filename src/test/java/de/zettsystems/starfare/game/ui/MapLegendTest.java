package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The phone layout shows the map legend as a dialog. */
class MapLegendTest extends AbstractBrowserlessTest {

    @Test
    void explainsTheMapSymbolsAndCloses() {
        MapLegend.open();
        Dialog dialog = openedDialog();

        assertThat(dialog.getHeaderTitle()).isEqualTo("Legende");
        assertThat(elementText(dialog.getElement())).contains("G: Garnison", "P: Produktion");

        test(find(Button.class).withText("Schließen").single()).click();
        assertThat(dialog.isOpened()).isFalse();
    }
}
