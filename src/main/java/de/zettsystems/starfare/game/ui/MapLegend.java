package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import de.zettsystems.starfare.i18n.I18n;

/** The phone's map help: the legend as a dialog, since the phone layout has no room for the desktop's help panel. */
final class MapLegend {
    private MapLegend() { }

    static void open() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.MAP_HELP));
        dialog.add(new Paragraph(I18n.t(UiTexts.MAP_LEGEND_TEXT_TOUCH)));
        dialog.getFooter().add(new Button(I18n.t(UiTexts.MAP_MOBILE_CLOSE), _ -> dialog.close()));
        dialog.open();
    }
}
