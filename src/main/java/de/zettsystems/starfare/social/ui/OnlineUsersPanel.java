package de.zettsystems.starfare.social.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
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
import de.zettsystems.starfare.social.application.PresenceTracker;
import de.zettsystems.starfare.social.application.SocialBroadcaster;
import de.zettsystems.starfare.social.application.VisibilityFilter;
import de.zettsystems.starfare.social.values.Friendship;
import de.zettsystems.starfare.social.values.UserPresence;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Right-hand sidebar listing online users that the current viewer is allowed to see. Per-row context
 * menu offers friendship actions depending on the current relationship state.
 */
public class OnlineUsersPanel extends VerticalLayout {

    private final PresenceTracker presence;
    private final SocialBroadcaster broadcaster;
    private final VisibilityFilter visibility;
    private final FriendshipService friendships;
    private final Consumer<String> openChatWith;
    private final Div list = new Div();
    private final Span empty = new Span(I18n.t(UiTexts.PRESENCE_EMPTY));
    private @Nullable Subscription subscription;

    public OnlineUsersPanel(PresenceTracker presence, SocialBroadcaster broadcaster,
                            VisibilityFilter visibility, FriendshipService friendships,
                            Consumer<String> openChatWith) {
        this.presence = presence;
        this.broadcaster = broadcaster;
        this.visibility = visibility;
        this.friendships = friendships;
        this.openChatWith = openChatWith;
        addClassName("presence-panel");
        setPadding(false);
        setSpacing(false);
        H3 title = new H3(I18n.t(UiTexts.PRESENCE_TITLE));
        title.addClassName("presence-title");
        empty.addClassName("presence-empty");
        list.addClassName("presence-list");
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

    public void refresh() {
        String viewer = UserContext.currentUsername().orElse(null);
        list.removeAll();
        if (viewer == null) {
            empty.setVisible(true);
            list.setVisible(false);
            return;
        }
        List<UserPresence> visibleUsers = presence.onlineUsers().stream()
                .filter(u -> visibility.canSee(viewer, u.username()))
                .toList();
        boolean any = !visibleUsers.isEmpty();
        empty.setVisible(!any);
        list.setVisible(any);
        for (UserPresence u : visibleUsers) {
            list.add(renderRow(viewer, u.username()));
        }
    }

    private Div renderRow(String viewer, String username) {
        Div row = new Div();
        row.addClassName("presence-row");
        Span dot = new Span();
        dot.addClassName("presence-dot");
        Span name = new Span(username);
        name.addClassName("presence-name");
        row.add(dot, name);

        if (viewer != null && !viewer.equalsIgnoreCase(username)) {
            Optional<Friendship> status = friendships.statusBetween(viewer, username);
            status.ifPresent(f -> row.add(statusBadge(f)));
            Button actions = new Button("\u22EF");
            actions.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
            actions.addClassName("presence-actions");
            ContextMenu menu = new ContextMenu(actions);
            menu.setOpenOnClick(true);
            buildMenu(menu, viewer, username, status.orElse(null));
            row.add(actions);
        }
        return row;
    }

    private Span statusBadge(Friendship f) {
        String key = switch (f.status()) {
            case ACCEPTED -> UiTexts.PRESENCE_STATUS_FRIEND;
            case PENDING -> UiTexts.PRESENCE_STATUS_PENDING;
            case BLOCKED -> UiTexts.PRESENCE_STATUS_BLOCKED;
        };
        Span badge = new Span(I18n.t(key));
        badge.addClassName("presence-status-badge");
        badge.addClassName("presence-status-" + f.status().name().toLowerCase(Locale.ROOT));
        return badge;
    }

    @SuppressWarnings("java:S6916")
    private void buildMenu(ContextMenu menu, String viewer, String target, @Nullable Friendship current) {
        if (openChatWith != null && visibility.canSee(viewer, target)) {
            menu.addItem(I18n.t(UiTexts.CHAT_ACTION_MESSAGE),
                    _ -> openChatWith.accept(target));
        }
        if (current == null) {
            menu.addItem(I18n.t(UiTexts.PRESENCE_ACTION_FRIEND_REQUEST),
                    _ -> notifyResult(friendships.request(viewer, target),
                            UiTexts.PRESENCE_REQUEST_SENT, UiTexts.PRESENCE_REQUEST_FAILED));
            menu.addItem(I18n.t(UiTexts.PRESENCE_ACTION_BLOCK),
                    _ -> runBlock(viewer, target));
            return;
        }
        switch (current.status()) {
            case PENDING -> {
                if (viewer.equalsIgnoreCase(current.requestedBy())) {
                    menu.addItem(I18n.t(UiTexts.PRESENCE_ACTION_CANCEL_REQUEST),
                            _ -> notifyResult(friendships.remove(viewer, target),
                                    null, UiTexts.PRESENCE_ACTION_FAILED));
                } else {
                    menu.addItem(I18n.t(UiTexts.FRIEND_INBOX_ACCEPT),
                            _ -> notifyResult(friendships.accept(viewer, target),
                                    null, UiTexts.PRESENCE_ACTION_FAILED));
                    menu.addItem(I18n.t(UiTexts.FRIEND_INBOX_DECLINE),
                            _ -> notifyResult(friendships.decline(viewer, target),
                                    null, UiTexts.PRESENCE_ACTION_FAILED));
                }
                menu.addItem(I18n.t(UiTexts.PRESENCE_ACTION_BLOCK),
                        _ -> runBlock(viewer, target));
            }
            case ACCEPTED -> {
                menu.addItem(I18n.t(UiTexts.PRESENCE_ACTION_REMOVE_FRIEND),
                        _ -> notifyResult(friendships.remove(viewer, target),
                                null, UiTexts.PRESENCE_ACTION_FAILED));
                menu.addItem(I18n.t(UiTexts.PRESENCE_ACTION_BLOCK),
                        _ -> runBlock(viewer, target));
            }
            case BLOCKED -> {
                if (viewer.equalsIgnoreCase(current.requestedBy())) {
                    menu.addItem(I18n.t(UiTexts.PRESENCE_ACTION_UNBLOCK),
                            _ -> notifyResult(friendships.unblock(viewer, target),
                                    null, UiTexts.PRESENCE_ACTION_FAILED));
                }
            }
        }
    }

    private void runBlock(String viewer, String target) {
        notifyResult(friendships.block(viewer, target), null, UiTexts.PRESENCE_ACTION_FAILED);
    }

    private void notifyResult(boolean ok, @Nullable String successKey, String failureKey) {
        if (ok) {
            if (successKey != null) {
                Notification.show(I18n.t(successKey));
            }
        } else {
            Notification.show(I18n.t(failureKey));
        }
    }
}
