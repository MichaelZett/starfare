package de.zettsystems.starfare.standalone.social.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.game.ui.LobbyExtras;
import de.zettsystems.starfare.game.ui.StarfareRoutes;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import jakarta.annotation.security.PermitAll;

/**
 * Starfare's friends, invitations, who is online and chat on a page of their own, in Starfare's own
 * start only (plan M13); in a host the platform has its friends page.
 */
@Route(StarfareRoutes.SOCIAL)
@CssImport("./styles/starfare.css")
@PermitAll
public class SocialView extends VerticalLayout {

    public SocialView(StandaloneSocialPanels panels) {
        addClassName("social-page");
        LobbyExtras.Parts parts = panels.create();
        parts.sidebar().addClassName("social-page-panels");
        Div heading = new Div(new H1(I18n.t(UiTexts.SOCIAL_TITLE)), parts.toolbar());
        heading.addClassName("social-page-heading");
        add(heading, parts.sidebar());
    }
}
