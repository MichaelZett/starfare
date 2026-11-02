package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.navigation.ui.RouteKeys;
import de.zettsystems.starfare.navigation.values.RangeView;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.List;
import java.util.Locale;

/**
 * Direct range during target selection: a quiet circle in map coordinates without own click
 * behaviour, and a reach state on each target that never relies on colour alone.
 */
final class RangeOverlay {
    /** Room for the dashed marker around a target: outline width plus offset, and one pixel of air. */
    private static final double TARGET_CLEARANCE = 6;

    private RangeOverlay() { }

    static Div circle(VisibleSystem source, RangeView range, List<VisibleSystem> systems) {
        Div circle = new Div();
        circle.addClassName("map-range-circle");
        circle.getElement().setAttribute("aria-hidden", "true");
        double radius = drawnRadius(source, range, systems);
        circle.getStyle().set("position", "absolute").set("pointer-events", "none").set("z-index", "1")
                .set(CssProperties.LEFT, (source.x() - radius) + "px").set(CssProperties.TOP, (source.y() - radius) + "px")
                .set(CssProperties.WIDTH, (2 * radius) + "px").set(CssProperties.HEIGHT, (2 * radius) + "px")
                .set("border-radius", "50%").set("box-sizing", "border-box")
                .set("border", "2px dashed rgba(128, 200, 232, .55)").set("background", "rgba(128, 200, 232, .05)");
        return circle;
    }

    /**
     * Range is decided between system centres, yet a system is a large disc: one just out of range
     * would reach into the circle and look reachable. The circle therefore ends before every system
     * that cannot be flown to directly. The marker on each target stays the exact answer.
     */
    static double drawnRadius(VisibleSystem source, RangeView range, List<VisibleSystem> systems) {
        double radius = range.range();
        for (VisibleSystem system : systems) {
            if (range.targets().containsKey(system.id()) && range.target(system.id()).reach() != RangeView.Reach.DIRECT) {
                double distance = Math.hypot(system.x() - source.x(), system.y() - source.y());
                // A pending battle draws the system at the unknown size, so never count less than that.
                double edge = Math.max(SystemDisplaySize.pixelsFor(system), SystemDisplaySize.UNKNOWN_SIZE) / 2.0;
                radius = Math.min(radius, distance - edge - TARGET_CLEARANCE);
            }
        }
        return Math.max(radius, 0);
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
