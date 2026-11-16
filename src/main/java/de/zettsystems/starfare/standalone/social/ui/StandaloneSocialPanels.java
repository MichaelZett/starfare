package de.zettsystems.starfare.standalone.social.ui;

import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.InvitationService;
import de.zettsystems.starfare.game.ui.LobbyExtras;
import de.zettsystems.starfare.standalone.social.application.FriendshipService;
import de.zettsystems.starfare.standalone.social.application.MessageService;
import de.zettsystems.starfare.standalone.social.application.PresenceTracker;
import de.zettsystems.starfare.standalone.social.application.SocialBroadcaster;
import de.zettsystems.starfare.standalone.social.application.UserPreferencesService;
import de.zettsystems.starfare.standalone.social.application.VisibilityFilter;
import org.springframework.stereotype.Component;

/**
 * Friend requests, invitations, who is online, the chat and the visibility menu for Starfare's own
 * start: beside the lobby's matches and on the social page. A host has its own (plan M13).
 */
@Component
public class StandaloneSocialPanels implements LobbyExtras {
    private final GameService games;
    private final Broadcaster gameBroadcaster;
    private final PresenceTracker presence;
    private final SocialBroadcaster broadcaster;
    private final VisibilityFilter visibility;
    private final FriendshipService friendships;
    private final UserPreferencesService preferences;
    private final PlayerDirectory players;
    private final MessageService messages;
    private final InvitationService invitations;

    @SuppressWarnings("java:S107") // Spring constructor: the panels bring the social services together.
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected repositories and collaborators are shared Spring beans") // rationale: dependency injection shares collaborators.
    public StandaloneSocialPanels(GameService games, Broadcaster gameBroadcaster, PresenceTracker presence,
                                  SocialBroadcaster broadcaster, VisibilityFilter visibility,
                                  FriendshipService friendships, UserPreferencesService preferences,
                                  PlayerDirectory players, MessageService messages, InvitationService invitations) {
        this.games = games;
        this.gameBroadcaster = gameBroadcaster;
        this.presence = presence;
        this.broadcaster = broadcaster;
        this.visibility = visibility;
        this.friendships = friendships;
        this.preferences = preferences;
        this.players = players;
        this.messages = messages;
        this.invitations = invitations;
    }

    @Override
    public Parts create() {
        ChatDrawer chat = new ChatDrawer(messages, broadcaster, players);
        OnlineUsersPanel online = new OnlineUsersPanel(presence, broadcaster, visibility, friendships, players,
                chat::openChatWith);
        Div sidebar = new Div(new FriendRequestsPanel(friendships, broadcaster, players),
                new InvitationsPanel(games, invitations, gameBroadcaster, players), online, chat);
        sidebar.addClassName("social-panels");
        return new Parts(new VisibilityMenu(preferences, online::refresh), sidebar);
    }
}
