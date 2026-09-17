package de.zettsystems.starfare.social.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.social.application.InvitationService;
import de.zettsystems.starfare.social.application.SocialBroadcaster;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Inbox for pending game-seat invitations addressed to the current user. Auto-hides when empty.
 */
public class InvitationsPanel extends VerticalLayout {

    private final GameService games;
    private final InvitationService invitations;
    private final SocialBroadcaster broadcaster;
    private final PlayerDirectory players;
    private final Div list = new Div();
    private final Span empty = new Span(I18n.t(UiTexts.INVITATIONS_EMPTY));
    private @Nullable Subscription subscription;

    public InvitationsPanel(GameService games, InvitationService invitations, SocialBroadcaster broadcaster,
                            PlayerDirectory players) {
        this.games = games;
        this.invitations = invitations;
        this.broadcaster = broadcaster;
        this.players = players;
        addClassName("invitations-panel");
        setPadding(false);
        setSpacing(false);
        H3 title = new H3(I18n.t(UiTexts.INVITATIONS_TITLE));
        title.addClassName("invitations-title");
        empty.addClassName("invitations-empty");
        list.addClassName("invitations-list");
        add(title, list, empty);
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        subscription = broadcaster.subscribe(_ -> ui.access(this::refresh));
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (subscription != null) {
            subscription.remove();
            subscription = null;
        }
    }

    private void refresh() {
        String viewer = UserContext.currentPlayerId().orElse(null);
        list.removeAll();
        if (viewer == null) {
            setVisible(false);
            return;
        }
        List<PendingInvite> inbox = collect(viewer);
        boolean any = !inbox.isEmpty();
        empty.setVisible(!any);
        list.setVisible(any);
        setVisible(any);
        for (PendingInvite inv : inbox) {
            list.add(renderRow(viewer, inv));
        }
    }

    private List<PendingInvite> collect(String viewer) {
        return games.visibleGamesFor(viewer, GameListScope.LOBBY).stream()
                .filter(summary -> summary.invitedSeats().containsKey(viewer))
                .map(summary -> new PendingInvite(summary.gameId(), summary.name(), summary.hostPlayerId())).toList();
    }

    private Div renderRow(String viewer, PendingInvite invite) {
        Div row = new Div();
        row.addClassName("invitations-row");
        String hostName = invite.host() == null ? "" : players.displayName(invite.host());
        Span label = new Span(I18n.t(UiTexts.INVITATIONS_ROW, invite.gameName(), hostName));
        label.addClassName("invitations-row-label");
        Button accept = new Button(I18n.t(UiTexts.INVITATIONS_ACCEPT), _ -> {
            if (!invitations.acceptInvite(invite.gameId(), viewer)) {
                Notification.show(I18n.t(UiTexts.INVITATIONS_ACCEPT_FAILED));
            } else {
                Notification.show(I18n.t(UiTexts.INVITATIONS_ACCEPTED));
            }
        });
        accept.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        Button decline = new Button(I18n.t(UiTexts.INVITATIONS_DECLINE),
                _ -> invitations.declineInvite(invite.gameId(), viewer));
        decline.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        row.add(label, accept, decline);
        return row;
    }

    private record PendingInvite(GameId gameId, String gameName, @Nullable String host) {
    }
}
