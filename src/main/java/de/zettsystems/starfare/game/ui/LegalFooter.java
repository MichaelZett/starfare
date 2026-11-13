package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;

/**
 * Links to imprint and privacy in Starfare's own start. The pages belong to the starting
 * application, so the links use paths, not view classes: a host serves its own pages there.
 */
public final class LegalFooter extends Div {
    public LegalFooter() {
        addClassName("legal-footer");
        add(link(UiTexts.LEGAL_IMPRINT, "/imprint"), link(UiTexts.LEGAL_PRIVACY, "/privacy"));
    }

    private static Anchor link(String key, String path) {
        Anchor link = new Anchor(path, I18n.t(key));
        link.getElement().setAttribute(HtmlAttributes.TARGET, "_blank");
        link.getElement().setAttribute(HtmlAttributes.REL, "noopener noreferrer");
        link.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, I18n.t(UiTexts.UX_NEW_TAB, I18n.t(key)));
        return link;
    }
}
