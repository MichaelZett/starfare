package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.SystemComposition;
import de.zettsystems.starfare.game.values.SystemComposition.Body;
import de.zettsystems.starfare.game.values.SystemComposition.StarType;
import de.zettsystems.starfare.game.values.SystemDiagramLayout;
import de.zettsystems.starfare.game.values.PhysicalSystemSize;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.economy.values.SystemQuality;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared schematic system view; its celestial controls never issue game commands. */
final class DetailedSystemPanel extends Div {
    private final Div description = new Div();
    private final Div background = new Div();
    private Runnable onInspect = () -> { };
    private Runnable onColonyInspect = () -> { };
    private final List<Button> targets = new ArrayList<>();
    private final GameId game;
    private final int system;

    DetailedSystemPanel(SystemComposition composition, boolean shipyard, GameId game, int system) {
        this.game = game;
        this.system = system;
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
            addBody(diagram, body, composition);
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
        Div catalog = new Div();
        catalog.addClassName("system-body-catalog");
        composition.bodies().forEach(body -> catalog.add(catalogButton(body)));
        background.add(description, new Span(I18n.t(UiTexts.SYSTEM_CATALOG)), catalog);
        add(diagram, caption);
        selectStar(composition.stars().getFirst(), 0);
    }

    Div background() { return background; }

    void onInspect(Runnable action) { onInspect = action; }
    void onColonyInspect(Runnable action) { onColonyInspect = action; }

    private static void drawOrbits(Div diagram, SystemComposition composition) {
        for (Body body : composition.bodies()) {
            Div orbit = new Div();
            orbit.addClassName("system-orbit");
            if (body.type() == SystemComposition.BodyType.ASTEROIDS) {
                orbit.addClassName("system-asteroid-belt");
            }
            double radius = SystemDiagramLayout.orbitPosition(composition, body);
            orbit.getStyle().set(CssProperties.WIDTH, percent(radius * 2));
            orbit.getStyle().set(CssProperties.HEIGHT, "130%");
            diagram.add(orbit);
        }
    }

    private void addStar(Div diagram, StarType type, int index, int count) {
        Button star = new Button();
        star.addClassNames("system-body-target", "system-star", starClass(type));
        star.setAriaLabel(I18n.t(UiTexts.SYSTEM_STAR_NAME, index == 0 ? "A" : "B")
                + ": " + I18n.t(starKey(type)));
        star.getElement().setAttribute(HtmlAttributes.TITLE, star.getAriaLabel().orElse(""));
        position(star, 0, 50);
        int radius = PhysicalSystemSize.starRadius(game, system, type, index);
        star.getElement().setAttribute("data-model-radius-km", String.valueOf(radius));
        String diameter = PhysicalSystemSize.starDiameter(radius) + "px";
        star.getStyle().set(CssProperties.WIDTH, diameter).set(CssProperties.HEIGHT, diameter);
        star.getElement().setAttribute(HtmlAttributes.TITLE, star.getAriaLabel().orElse("") + " · " + radiusText(radius));
        star.addClickListener(_ -> selectStar(type, index));
        targets.add(star);
        Span name = new Span(star.getAriaLabel().orElse(""));
        name.addClassName("system-star-name");
        Div row = new Div(star, name);
        row.addClassName("system-star-row");
        row.getStyle().set(CssProperties.TOP, percent(100.0 * index / count))
                .set(CssProperties.HEIGHT, percent(100.0 / count));
        diagram.add(row);
    }

    private void addBody(Div diagram, Body body, SystemComposition composition) {
        Button target = bodyButton(body);
        target.addClassName("system-body-target");
        var quality = composition.colonyQuality();
        boolean planetColony = quality != null && !quality.moon();
        Span label = new Span(planetColony ? I18n.t(UiTexts.SYSTEM_COLONY_TARGET) : String.valueOf(body.orbit()));
        label.addClassName("system-body-label");
        target.getElement().appendChild(label.getElement());
        if (quality != null && !quality.moon()) {
            target.addClassName("system-colony-target");
            target.setAriaLabel(I18n.t(UiTexts.SYSTEM_COLONY_PLANET));
            applyEnvironment(target, quality);
        }
        for (int index = 0; index < (quality == null ? body.majorMoons() : 0); index++) {
            Span moon = new Span();
            moon.addClassNames("system-moon", "system-moon-" + index);
            int radius = PhysicalSystemSize.moonRadius(game, system, body.orbit(), index);
            String diameter = PhysicalSystemSize.bodyDiameter(radius) + "px";
            moon.getStyle().set(CssProperties.WIDTH, diameter).set(CssProperties.HEIGHT, diameter);
            moon.getElement().setAttribute(HtmlAttributes.TITLE, radiusText(radius));
            target.getElement().appendChild(moon.getElement());
        }
        double x = SystemDiagramLayout.orbitPosition(composition, body);
        position(target, x, SystemDiagramLayout.bodyHeight(composition, body));
        diagram.add(target);
        if (quality != null && quality.moon()) { addColonyMoon(diagram, quality, x); }
    }

    private void addColonyMoon(Div diagram, SystemQuality quality, double planetX) {
        Button moon = new Button();
        moon.addClassNames("system-body-target", "system-world", "system-colony-moon", "system-colony-target");
        int radius = PhysicalSystemSize.moonRadius(game, system, 1, 0);
        moon.getElement().setAttribute("data-model-radius-km", String.valueOf(radius));
        moon.getStyle().set(CssProperties.BODY_DIAMETER, PhysicalSystemSize.bodyDiameter(radius) + "px");
        Span sphere = new Span();
        sphere.addClassNames("system-body-sphere", "system-world-rocky");
        Span label = new Span(I18n.t(UiTexts.SYSTEM_COLONY_TARGET));
        label.addClassName("system-body-label");
        moon.getElement().appendChild(sphere.getElement(), label.getElement());
        moon.setAriaLabel(I18n.t(UiTexts.SYSTEM_COLONY_MOON));
        moon.getElement().setAttribute(HtmlAttributes.TITLE, I18n.t(UiTexts.SYSTEM_COLONY_MOON) + " · " + radiusText(radius));
        applyEnvironment(moon, quality);
        position(moon, Math.min(planetX + 8, 87), 76);
        moon.addClickListener(_ -> {
            highlight(targets.indexOf(moon));
            description.removeAll();
            description.add(new Span(I18n.t(UiTexts.SYSTEM_COLONY_MOON)), new Span(radiusText(radius)));
            onColonyInspect.run();
        });
        targets.add(moon);
        diagram.add(moon);
    }

    private static void applyEnvironment(Button target, SystemQuality quality) {
        target.addClassName("system-atmosphere-" + quality.atmosphere().name().toLowerCase(Locale.ROOT));
        target.addClassName("system-suitability-" + quality.suitability().name().toLowerCase(Locale.ROOT));
    }

    private Button bodyButton(Body body) {
        Button target = new Button();
        target.addClassNames("system-world", "system-surface-" + body.appearance());
        int diameter = body.type() == SystemComposition.BodyType.ASTEROIDS ? SystemDiagramLayout.bodyDiameter(body.type())
                : PhysicalSystemSize.bodyDiameter(PhysicalSystemSize.bodyRadius(game, system, body));
        target.getStyle().set(CssProperties.BODY_DIAMETER, diameter + "px");
        Span sphere = new Span();
        sphere.addClassNames("system-body-sphere", "system-world-" + body.type().name().toLowerCase(Locale.ROOT));
        if (body.rings()) { sphere.addClassName("system-world-ringed"); }
        target.getElement().appendChild(sphere.getElement());
        String name = I18n.t(UiTexts.SYSTEM_ORBIT, body.orbit());
        target.setAriaLabel(name + ": " + I18n.t(bodyKey(body)));
        target.getElement().setAttribute(HtmlAttributes.TITLE, target.getAriaLabel().orElse(""));
        if (body.type() != SystemComposition.BodyType.ASTEROIDS) {
            target.getElement().setAttribute("data-model-radius-km", String.valueOf(PhysicalSystemSize.bodyRadius(game, system, body)));
            target.getElement().setAttribute(HtmlAttributes.TITLE, target.getAriaLabel().orElse("") + " · "
                    + radiusText(PhysicalSystemSize.bodyRadius(game, system, body)));
        }
        target.addClickListener(_ -> {
            if (target.hasClassName("system-colony-target")) {
                highlight(targets.indexOf(target));
                description.removeAll();
                description.add(new Span(radiusText(PhysicalSystemSize.bodyRadius(game, system, body))));
                onColonyInspect.run();
            } else { selectBody(body); }
        });
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
        description.add(new Span(I18n.t(starKey(type))), new Paragraph(I18n.t(starDescriptionKey(type))));
        description.add(new Span(radiusText(PhysicalSystemSize.starRadius(game, system, type, index))));
        onInspect.run();
    }

    private void selectBody(Body body) {
        // Stars precede worlds in the target list.
        targets.stream().filter(target -> target.hasClassName("system-world")
                && !target.hasClassName("system-colony-moon"))
                .skip(body.orbit() - 1L).findFirst().ifPresent(target -> highlight(targets.indexOf(target)));
        description.removeAll();
        description.add(new Span(I18n.t(UiTexts.SYSTEM_ORBIT, body.orbit()) + " · " + I18n.t(bodyKey(body))),
                new Paragraph(I18n.t(bodyDescriptionKey(body))));
        if (body.type() != SystemComposition.BodyType.ASTEROIDS) {
            description.add(new Span(radiusText(PhysicalSystemSize.bodyRadius(game, system, body))));
            description.add(new Span(I18n.t(UiTexts.SYSTEM_MOONS, body.majorMoons())));
            for (int index = 0; index < body.majorMoons(); index++) {
                description.add(new Span(I18n.t(UiTexts.SYSTEM_MODEL_MOON_RADIUS, index + 1,
                        PhysicalSystemSize.moonRadius(game, system, body.orbit(), index))));
            }
        }
        onInspect.run();
    }

    private void highlight(int selected) {
        for (int index = 0; index < targets.size(); index++) {
            Button target = targets.get(index);
            target.getElement().setAttribute(HtmlAttributes.ARIA_PRESSED, index == selected ? "true" : "false");
        }
    }

    private static String radiusText(int radius) { return I18n.t(UiTexts.SYSTEM_MODEL_RADIUS, radius); }

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
