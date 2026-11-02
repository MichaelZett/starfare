package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Span;

import java.util.Locale;

/** Round section badges of the game wizard; the drawings live in styles/starfare-wizard.css. */
enum WizardEmblem {
    GALAXY, FLEET, VICTORY, ROUNDS, AI, TUNING, PLANET, COMBAT;

    /** Decorative only: a heading next to the badge always says the same. */
    Span create() {
        Span emblem = new Span();
        emblem.addClassNames("wizard-emblem", "wizard-emblem-" + name().toLowerCase(Locale.ROOT));
        emblem.getElement().setAttribute("aria-hidden", "true");
        return emblem;
    }
}
