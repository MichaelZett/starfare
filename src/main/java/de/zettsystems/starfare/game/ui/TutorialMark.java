package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;

/**
 * The platform's mark of a tutorial game ({@code gp-badge gp-tutorial}), shown wherever the game is named: the
 * wizard's summary, the waiting room and the map header. Every seat sees it.
 */
final class TutorialMark {
    static final String CLASS_NAME = "tutorial-mark";

    private TutorialMark() {
    }

    static Span create() {
        Span mark = new Span(I18n.t(UiTexts.TUTORIAL_MARK));
        mark.addClassNames("gp-badge", "gp-tutorial", CLASS_NAME);
        mark.getElement().setProperty(HtmlAttributes.TITLE, I18n.t(UiTexts.TUTORIAL_MARK_HINT));
        return mark;
    }
}
