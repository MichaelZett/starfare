package de.zettsystems.starfare.legal.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLink;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;

public final class LegalFooter extends Div {
    public LegalFooter() {
        addClassName("legal-footer");
        add(link(UiTexts.LEGAL_IMPRINT, ImprintView.class), link(UiTexts.LEGAL_PRIVACY, PrivacyView.class));
    }
    private static RouterLink link(String key, Class<? extends com.vaadin.flow.component.Component> view) {
        RouterLink link = new RouterLink(I18n.t(key), view);
        link.getElement().setAttribute(HtmlAttributes.TARGET, "_blank");
        link.getElement().setAttribute(HtmlAttributes.REL, "noopener noreferrer");
        link.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, I18n.t(UiTexts.UX_NEW_TAB, I18n.t(key)));
        return link;
    }
}
