package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import de.zettsystems.starfare.i18n.I18n;

final class MapControls extends Div {
    MapControls(MapCanvas canvas) {
        addClassName("map-controls");
        add(action(UiTexts.MAP_ZOOM_IN, () -> canvas.zoom(1.2)),
                action(UiTexts.MAP_ZOOM_OUT, () -> canvas.zoom(1 / 1.2)),
                action(UiTexts.MAP_RESET, canvas::resetViewport),
                action(UiTexts.MAP_FIT_GALAXY, () -> canvas.fit(false)),
                action(UiTexts.MAP_FIT_EMPIRE, () -> canvas.fit(true)),
                new Details(I18n.t(UiTexts.MAP_LEGEND), new Paragraph(I18n.t(UiTexts.MAP_LEGEND_TEXT))));
    }

    private static Button action(String key, Runnable action) {
        return new Button(I18n.t(key), _ -> action.run());
    }
}
