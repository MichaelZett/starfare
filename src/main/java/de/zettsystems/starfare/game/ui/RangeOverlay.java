package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.navigation.ui.RouteKeys;
import de.zettsystems.starfare.navigation.values.RangeView;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.Locale;

/**
 * Direct range during target selection: a quiet circle in map coordinates without own click
 * behaviour, and a reach state on each target that never relies on colour alone.
 */
final class RangeOverlay {
    private RangeOverlay() { }

    static Div circle(VisibleSystem source, RangeView range) {
        Div circle = new Div();
        circle.addClassName("map-range-circle");
        circle.getElement().setAttribute("aria-hidden", "true");
        double radius = range.range();
        circle.getStyle().set("position", "absolute").set("pointer-events", "none").set("z-index", "1")
                .set(CssProperties.LEFT, (source.x() - radius) + "px").set(CssProperties.TOP, (source.y() - radius) + "px")
                .set(CssProperties.WIDTH, (2 * radius) + "px").set(CssProperties.HEIGHT, (2 * radius) + "px")
                .set("border-radius", "50%").set("box-sizing", "border-box")
                .set("border", "2px dashed rgba(128, 200, 232, .55)").set("background", "rgba(128, 200, 232, .05)");
        return circle;
    }

    /** Marks a target by reach and returns the accessible description for its tooltip. */
    static String markTarget(Div dot, RangeView.Target target) {
        dot.getElement().setAttribute("data-reach", target.reach().name().toLowerCase(Locale.ROOT));
        switch (target.reach()) {
            case DIRECT -> dot.getStyle().set("outline", "2px solid rgba(128, 208, 255, .85)").set("outline-offset", "3px");
            case STATIONS -> dot.getStyle().set("outline", "2px dashed #e0b84a").set("outline-offset", "3px");
            case NONE -> dot.getStyle().set("opacity", ".45");
        }
        String description = describe(target);
        dot.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, dot.getElement().getText() + " — " + description);
        return description;
    }

    static String describe(RangeView.Target target) {
        String key = switch (target.reach()) {
            case DIRECT -> RouteKeys.REACH_DIRECT;
            case STATIONS -> RouteKeys.REACH_STATIONS;
            case NONE -> RouteKeys.REACH_NONE;
        };
        return I18n.t(key, target.percent());
    }
}
