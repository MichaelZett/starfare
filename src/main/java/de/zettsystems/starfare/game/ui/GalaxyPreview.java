package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;

/** Illustrates a star distribution; it is a diagram, not the map the game will generate. */
final class GalaxyPreview extends Div {
    private static final double[][] EVEN_POSITIONS = {{12, 18}, {34, 16}, {57, 19}, {82, 14}, {22, 43}, {48, 42},
            {73, 44}, {12, 72}, {38, 68}, {62, 74}, {86, 69}, {50, 88}};
    private static final double[][] RANDOM_POSITIONS = {{8, 20}, {31, 12}, {67, 25}, {85, 11}, {17, 47}, {48, 36},
            {77, 53}, {29, 76}, {58, 66}, {91, 81}, {6, 88}, {48, 91}};
    private static final String EVEN_CLASS = "wizard-galaxy-even";
    private static final String RANDOM_CLASS = "wizard-galaxy-random";

    private final Div stars = new Div();

    GalaxyPreview(GalaxyLayout layout) {
        addClassName("wizard-galaxy-preview");
        stars.addClassName("wizard-galaxy-stars");
        add(new Span(I18n.t(UiTexts.LOBBY_SUMMARY_SCHEMATIC)), stars);
        show(layout);
    }

    void show(GalaxyLayout layout) {
        boolean even = layout == GalaxyLayout.EVEN;
        setClassName(EVEN_CLASS, even);
        setClassName(RANDOM_CLASS, !even);
        stars.removeAll();
        for (double[] position : even ? EVEN_POSITIONS : RANDOM_POSITIONS) {
            Span star = new Span();
            star.addClassName("wizard-galaxy-star");
            star.getStyle().set(CssProperties.LEFT, position[0] + "%");
            star.getStyle().set(CssProperties.TOP, position[1] + "%");
            stars.add(star);
        }
    }
}
