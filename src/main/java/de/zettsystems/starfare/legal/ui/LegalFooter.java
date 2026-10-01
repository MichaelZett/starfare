package de.zettsystems.starfare.legal.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLink;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;

public final class LegalFooter extends Div {
    public LegalFooter() {
        addClassName("legal-footer");
        add(new RouterLink(I18n.t(UiTexts.LEGAL_IMPRINT), ImprintView.class),
                new RouterLink(I18n.t(UiTexts.LEGAL_PRIVACY), PrivacyView.class));
    }
}
