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
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.social.application.FriendshipService;
import de.zettsystems.starfare.social.application.SocialBroadcaster;
import de.zettsystems.starfare.social.values.Friendship;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Inbox for incoming friendship requests (PENDING rows where the viewer is not the initiator).
 * Refreshes on every social event so accept/decline/new-request updates are immediate.
 */
public class FriendRequestsPanel extends VerticalLayout {

    private final FriendshipService friendships;
    private final SocialBroadcaster broadcaster;
    private final Div list = new Div();
    private final Span empty = new Span(I18n.t(UiTexts.FRIEND_INBOX_EMPTY));
    private @Nullable Subscription subscription;

    public FriendRequestsPanel(FriendshipService friendships, SocialBroadcaster broadcaster) {
        this.friendships = friendships;
        this.broadcaster = broadcaster;
        addClassName("friend-inbox");
        setPadding(false);
        setSpacing(false);
        H3 title = new H3(I18n.t(UiTexts.FRIEND_INBOX_TITLE));
        title.addClassName("friend-inbox-title");
        empty.addClassName("friend-inbox-empty");
        list.addClassName("friend-inbox-list");
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
        String viewer = UserContext.currentUsername().orElse(null);
        list.removeAll();
        if (viewer == null) {
            empty.setVisible(true);
            list.setVisible(false);
            setVisible(false);
            return;
        }
        List<Friendship> pending = friendships.incomingRequests(viewer);
        boolean any = !pending.isEmpty();
        empty.setVisible(!any);
        list.setVisible(any);
        setVisible(any);
        for (Friendship f : pending) {
            list.add(renderRow(viewer, f));
        }
    }

    private Div renderRow(String viewer, Friendship f) {
        Div row = new Div();
        row.addClassName("friend-inbox-row");
        String other = f.otherSide(viewer).orElse(null);
        Span from = new Span(other == null ? "" : other);
        from.addClassName("friend-inbox-from");
        Button accept = new Button(I18n.t(UiTexts.FRIEND_INBOX_ACCEPT), _ -> {
            if (other == null || !friendships.accept(viewer, other)) {
                Notification.show(I18n.t(UiTexts.PRESENCE_ACTION_FAILED));
            }
        });
        accept.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        Button decline = new Button(I18n.t(UiTexts.FRIEND_INBOX_DECLINE), _ -> {
            if (other == null || !friendships.decline(viewer, other)) {
                Notification.show(I18n.t(UiTexts.PRESENCE_ACTION_FAILED));
            }
        });
        decline.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        row.add(from, accept, decline);
        return row;
    }
}
