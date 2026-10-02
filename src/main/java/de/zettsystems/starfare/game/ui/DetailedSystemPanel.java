package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.SystemComposition;
import de.zettsystems.starfare.game.values.SystemComposition.Body;
import de.zettsystems.starfare.game.values.SystemComposition.StarType;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Decorative Classic system view; its controls never issue game commands. */
final class DetailedSystemPanel extends Div {
    private final Div description = new Div();
    private final Details properties = new Details();
    private final List<Button> targets = new ArrayList<>();

    DetailedSystemPanel(SystemComposition composition, boolean shipyard) {
        addClassName("detailed-system");
        Div diagram = new Div();
        diagram.addClassName("system-orrery");
        diagram.getElement().setAttribute(HtmlAttributes.ROLE, "group");
        diagram.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, I18n.t(UiTexts.SYSTEM_SCHEMATIC));
        drawOrbits(diagram, composition);
        for (int index = 0; index < composition.stars().size(); index++) {
            addStar(diagram, composition.stars().get(index), index, composition.stars().size());
        }
        for (Body body : composition.bodies()) {
            addBody(diagram, body, composition.bodies().size());
        }
        if (shipyard) {
            Span yard = new Span("⚒ " + I18n.t(UiTexts.SYSTEM_SHIPYARD));
            yard.addClassName("system-shipyard");
            yard.getElement().setAttribute(HtmlAttributes.TITLE, I18n.t(UiTexts.SYSTEM_SHIPYARD_HINT));
            diagram.add(yard);
        }
        Span caption = new Span(I18n.t(UiTexts.SYSTEM_SCHEMATIC));
        caption.addClassName("system-schematic-caption");
        description.addClassName("system-body-description");
        description.getElement().setAttribute(HtmlAttributes.ARIA_LIVE, "polite");
        properties.add(description);
        Div catalog = new Div();
        catalog.addClassName("system-body-catalog");
        composition.bodies().forEach(body -> catalog.add(catalogButton(body)));
        Details list = new Details(I18n.t(UiTexts.SYSTEM_CATALOG), catalog);
        add(diagram, caption, properties, list);
        selectStar(composition.stars().getFirst(), 0);
        properties.setOpened(false);
    }

    private static void drawOrbits(Div diagram, SystemComposition composition) {
        int count = composition.bodies().size();
        for (Body body : composition.bodies()) {
            Div orbit = new Div();
            orbit.addClassName("system-orbit");
            if (body.type() == SystemComposition.BodyType.ASTEROIDS) {
                orbit.addClassName("system-asteroid-belt");
            }
            double radius = radius(body.orbit(), count);
            orbit.getStyle().set(CssProperties.WIDTH, percent(radius * 2));
            orbit.getStyle().set(CssProperties.HEIGHT, percent(radius * 1.65));
            diagram.add(orbit);
        }
    }

    private void addStar(Div diagram, StarType type, int index, int count) {
        Button star = new Button();
        star.addClassNames("system-body-target", "system-star", starClass(type));
        star.getElement().appendChild(new Span(index == 0 ? "A" : "B").getElement());
        star.setAriaLabel(I18n.t(UiTexts.SYSTEM_STAR_NAME, index == 0 ? "A" : "B")
                + ": " + I18n.t(starKey(type)));
        star.getElement().setAttribute(HtmlAttributes.TITLE, star.getAriaLabel().orElse(""));
        position(star, count == 1 ? 50 : 43 + index * 14, 50);
        star.addClickListener(_ -> selectStar(type, index));
        targets.add(star);
        diagram.add(star);
    }

    private void addBody(Div diagram, Body body, int count) {
        Button target = bodyButton(body);
        target.addClassName("system-body-target");
        target.getElement().appendChild(new Span(String.valueOf(body.orbit())).getElement());
        for (int index = 0; index < body.majorMoons(); index++) {
            Span moon = new Span();
            moon.addClassNames("system-moon", "system-moon-" + index);
            target.getElement().appendChild(moon.getElement());
        }
        double angle = Math.toRadians(35 + body.orbit() * 137.5);
        double radius = radius(body.orbit(), count);
        position(target, 50 + Math.cos(angle) * radius, 50 + Math.sin(angle) * radius * 0.825);
        diagram.add(target);
    }

    private Button bodyButton(Body body) {
        Button target = new Button();
        target.addClassNames("system-world", "system-world-" + body.type().name().toLowerCase(Locale.ROOT),
                "system-surface-" + body.appearance());
        if (body.rings()) { target.addClassName("system-world-ringed"); }
        String name = I18n.t(UiTexts.SYSTEM_ORBIT, body.orbit());
        target.setAriaLabel(name + ": " + I18n.t(bodyKey(body)));
        target.getElement().setAttribute(HtmlAttributes.TITLE, target.getAriaLabel().orElse(""));
        target.addClickListener(_ -> selectBody(body));
        targets.add(target);
        return target;
    }

    private Button catalogButton(Body body) {
        Button button = new Button(I18n.t(UiTexts.SYSTEM_ORBIT, body.orbit()) + " · " + I18n.t(bodyKey(body)),
                _ -> selectBody(body));
        button.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        button.addClassName("system-catalog-button");
        return button;
    }

    private void selectStar(StarType type, int index) {
        highlight(index);
        description.removeAll();
        properties.setSummaryText(I18n.t(starKey(type)));
        properties.setOpened(true);
        description.add(new Span(I18n.t(starKey(type))), new Paragraph(I18n.t(starDescriptionKey(type))));
    }

    private void selectBody(Body body) {
        // Stars precede worlds in the target list.
        int starCount = (int) targets.stream().filter(target -> target.hasClassName("system-star")).count();
        highlight(starCount + body.orbit() - 1);
        description.removeAll();
        properties.setSummaryText(I18n.t(UiTexts.SYSTEM_ORBIT, body.orbit()) + " · " + I18n.t(bodyKey(body)));
        properties.setOpened(true);
        description.add(new Span(I18n.t(UiTexts.SYSTEM_ORBIT, body.orbit()) + " · " + I18n.t(bodyKey(body))),
                new Paragraph(I18n.t(bodyDescriptionKey(body))));
        if (body.type() != SystemComposition.BodyType.ASTEROIDS) {
            description.add(new Span(I18n.t(UiTexts.SYSTEM_MOONS, body.majorMoons())));
        }
    }

    private void highlight(int selected) {
        for (int index = 0; index < targets.size(); index++) {
            Button target = targets.get(index);
            target.getElement().setAttribute(HtmlAttributes.ARIA_PRESSED, index == selected ? "true" : "false");
        }
    }

    private static double radius(int orbit, int count) { return 10 + 25.0 * orbit / count; }
    private static String percent(double value) { return String.format(Locale.ROOT, "%.3f%%", value); }
    private static void position(Button button, double x, double y) {
        button.getStyle().set(CssProperties.LEFT, percent(x));
        button.getStyle().set(CssProperties.TOP, percent(y));
    }
    private static String starClass(StarType type) { return "system-star-" + type.name().toLowerCase(Locale.ROOT); }
    private static String starKey(StarType type) {
        return switch (type) {
            case RED -> UiTexts.SYSTEM_STAR_RED;
            case ORANGE -> UiTexts.SYSTEM_STAR_ORANGE;
            case YELLOW -> UiTexts.SYSTEM_STAR_YELLOW;
            case YELLOW_WHITE -> UiTexts.SYSTEM_STAR_YELLOW_WHITE;
            case WHITE -> UiTexts.SYSTEM_STAR_WHITE;
        };
    }
    private static String starDescriptionKey(StarType type) {
        return switch (type) {
            case RED -> UiTexts.SYSTEM_STAR_RED_DESCRIPTION;
            case ORANGE -> UiTexts.SYSTEM_STAR_ORANGE_DESCRIPTION;
            case YELLOW -> UiTexts.SYSTEM_STAR_YELLOW_DESCRIPTION;
            case YELLOW_WHITE -> UiTexts.SYSTEM_STAR_YELLOW_WHITE_DESCRIPTION;
            case WHITE -> UiTexts.SYSTEM_STAR_WHITE_DESCRIPTION;
        };
    }
    private static String bodyKey(Body body) {
        return switch (body.type()) {
            case ROCKY -> UiTexts.SYSTEM_BODY_ROCKY;
            case SUPER_EARTH -> UiTexts.SYSTEM_BODY_SUPER_EARTH;
            case GAS_GIANT -> UiTexts.SYSTEM_BODY_GAS_GIANT;
            case ICE_GIANT -> UiTexts.SYSTEM_BODY_ICE_GIANT;
            case DWARF -> UiTexts.SYSTEM_BODY_DWARF;
            case ASTEROIDS -> UiTexts.SYSTEM_BODY_ASTEROIDS;
        };
    }
    private static String bodyDescriptionKey(Body body) {
        return switch (body.type()) {
            case ROCKY -> UiTexts.SYSTEM_BODY_ROCKY_DESCRIPTION;
            case SUPER_EARTH -> UiTexts.SYSTEM_BODY_SUPER_EARTH_DESCRIPTION;
            case GAS_GIANT -> UiTexts.SYSTEM_BODY_GAS_GIANT_DESCRIPTION;
            case ICE_GIANT -> UiTexts.SYSTEM_BODY_ICE_GIANT_DESCRIPTION;
            case DWARF -> UiTexts.SYSTEM_BODY_DWARF_DESCRIPTION;
            case ASTEROIDS -> UiTexts.SYSTEM_BODY_ASTEROIDS_DESCRIPTION;
        };
    }
}
