package de.zettsystems.starfare.legal.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import jakarta.annotation.security.PermitAll;

/** Concise, public explanation of the data processed by the game. */
@Route("privacy")
@PermitAll
public class PrivacyView extends VerticalLayout {
    public PrivacyView() {
        setMaxWidth("58rem");
        add(new de.zettsystems.starfare.game.ui.AccountNavigation(), new H1(I18n.t(UiTexts.LEGAL_PRIVACY)),
                new Paragraph(I18n.t(UiTexts.PRIVACY_DATA)),
                new H2(I18n.t(UiTexts.PRIVACY_PURPOSE_TITLE)),
                new Paragraph(I18n.t(UiTexts.PRIVACY_PURPOSE)),
                new H2(I18n.t(UiTexts.PRIVACY_RIGHTS_TITLE)),
                new Paragraph(I18n.t(UiTexts.PRIVACY_RIGHTS)),
                new H2(I18n.t(UiTexts.LEGAL_IMPRINT_CONTACT)),
                new Paragraph(I18n.t(UiTexts.LEGAL_PRIVACY_CONTACT)), new LegalFooter());
    }
}
