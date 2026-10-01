package de.zettsystems.starfare.legal.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.legal.values.LegalContact;
import jakarta.annotation.security.PermitAll;

/** Public imprint with operator and contact data supplied by the deployment. */
@Route("imprint")
@PermitAll
public class ImprintView extends VerticalLayout {
    public ImprintView(LegalContact contact) {
        setMaxWidth("58rem");
        add(new de.zettsystems.starfare.game.ui.AccountNavigation(), new H1(I18n.t(UiTexts.LEGAL_IMPRINT_TITLE)), provider(contact), contact(contact), new LegalFooter());
    }

    private static VerticalLayout provider(LegalContact contact) {
        VerticalLayout provider = new VerticalLayout();
        provider.setPadding(false);
        provider.setSpacing(false);
        provider.add(new H2(I18n.t(UiTexts.LEGAL_IMPRINT_PROVIDER)), new Paragraph(contact.operatorName()));
        if (contact.hasPostalAddress()) {
            provider.add(new Paragraph(contact.postalAddress()));
        } else {
            provider.add(new Paragraph(I18n.t(UiTexts.LEGAL_IMPRINT_ADDRESS_MISSING)));
        }
        return provider;
    }

    private static VerticalLayout contact(LegalContact contact) {
        VerticalLayout details = new VerticalLayout();
        details.setPadding(false);
        details.setSpacing(false);
        details.add(new H2(I18n.t(UiTexts.LEGAL_IMPRINT_CONTACT)));
        if (contact.hasContactEmail()) {
            details.add(new Anchor("mailto:" + contact.contactEmail(), contact.contactEmail()));
        }
        return details;
    }
}
