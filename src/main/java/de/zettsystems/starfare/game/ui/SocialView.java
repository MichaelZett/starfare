package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.social.application.FriendshipService;
import de.zettsystems.starfare.social.application.InvitationService;
import de.zettsystems.starfare.social.application.MessageService;
import de.zettsystems.starfare.social.application.PresenceTracker;
import de.zettsystems.starfare.social.application.SocialBroadcaster;
import de.zettsystems.starfare.social.application.UserPreferencesService;
import de.zettsystems.starfare.social.application.VisibilityFilter;
import de.zettsystems.starfare.social.ui.ChatDrawer;
import de.zettsystems.starfare.social.ui.FriendRequestsPanel;
import de.zettsystems.starfare.social.ui.InvitationsPanel;
import de.zettsystems.starfare.social.ui.OnlineUsersPanel;
import de.zettsystems.starfare.social.ui.VisibilityMenu;
import jakarta.annotation.security.PermitAll;

/**
 * Starfare's friends, invitations, who is online and chat, on a page of their own. In a host the
 * platform lists the games; until the platform has a social area of its own (M13), this page keeps
 * these features reachable. Standalone, the lobby shows the same panels next to the games.
 */
@Route(StarfareRoutes.SOCIAL)
@CssImport("./styles/starfare.css")
@PermitAll
public class SocialView extends VerticalLayout {

    @SuppressWarnings("java:S107") // Spring-Konstruktor: die Seite bündelt die vier Bereiche des Sozialmoduls.
    public SocialView(GameService game, PresenceTracker presence, SocialBroadcaster socialBroadcaster,
                      VisibilityFilter visibilityFilter, FriendshipService friendshipService,
                      UserPreferencesService preferencesService, PlayerDirectory players,
                      MessageService messageService, InvitationService invitationService) {
        addClassName("social-page");
        ChatDrawer chat = new ChatDrawer(messageService, socialBroadcaster, players);
        OnlineUsersPanel online = new OnlineUsersPanel(presence, socialBroadcaster, visibilityFilter,
                friendshipService, players, chat::openChatWith);
        Div panels = new Div(new FriendRequestsPanel(friendshipService, socialBroadcaster, players),
                new InvitationsPanel(game, invitationService, socialBroadcaster, players), online, chat);
        panels.addClassName("social-page-panels");
        Div heading = new Div(new H1(I18n.t(UiTexts.SOCIAL_TITLE)), new VisibilityMenu(preferencesService, online::refresh));
        heading.addClassName("social-page-heading");
        add(heading, panels);
    }
}
