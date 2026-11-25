package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.navigation.ui.RouteKeys;
import java.util.function.BooleanSupplier;

final class MapControls extends Div {
    /** {@code routeRules}: whether the shown game limits flight range, so the help explains stations. */
    MapControls(MapCanvas canvas, BooleanSupplier routeRules) {
        addClassName("map-controls");
        Paragraph stations = new Paragraph(I18n.t(RouteKeys.RULES));
        stations.setVisible(false);
        Details help = new Details(I18n.t(UiTexts.MAP_HELP), new Div(
                action(UiTexts.MAP_RESET, canvas::resetViewport),
                new Paragraph(I18n.t(UiTexts.MAP_LEGEND_TEXT)), stations));
        help.addOpenedChangeListener(_ -> stations.setVisible(routeRules.getAsBoolean()));
        add(action(UiTexts.MAP_ZOOM_IN, () -> canvas.zoom(1.2)),
                action(UiTexts.MAP_ZOOM_OUT, () -> canvas.zoom(1 / 1.2)),
                action(UiTexts.MAP_FIT_GALAXY, () -> canvas.fit(false)),
                action(UiTexts.MAP_FIT_EMPIRE, () -> canvas.fit(true)),
                help);
    }

    private static Button action(String key, Runnable action) {
        return new Button(I18n.t(key), _ -> action.run());
    }
}
