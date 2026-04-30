package de.zettsystems.starfare.social.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.social.application.MessageService;
import de.zettsystems.starfare.social.application.SocialBroadcaster;
import de.zettsystems.starfare.social.values.DirectMessage;
import de.zettsystems.starfare.social.values.SocialEvent;
import org.jspecify.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Ephemeral chat panel: keeps one conversation list per session (lost on reload). Incoming
 * {@link SocialEvent.DirectMessage} events that involve the current user open or update a
 * conversation and bump the unread counter if the conversation is not currently active.
 */
public class ChatDrawer extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private final MessageService messages;
    private final SocialBroadcaster broadcaster;

    private final Map<String, Conversation> conversations = new LinkedHashMap<>();
    private final Div conversationList = new Div();
    private final Div conversationArea = new Div();
    private final Div messagesView = new Div();
    private final TextField input = new TextField();
    private final Button sendButton = new Button(I18n.t(UiTexts.CHAT_SEND));
    private final Span emptyList = new Span(I18n.t(UiTexts.CHAT_EMPTY_LIST));
    private final Span activeHeader = new Span();

    private @Nullable Subscription subscription;
    private @Nullable String activeUser;

    public ChatDrawer(MessageService messages, SocialBroadcaster broadcaster) {
        this.messages = messages;
        this.broadcaster = broadcaster;
        addClassName("chat-drawer");
        setPadding(false);
        setSpacing(false);

        H4 title = new H4(I18n.t(UiTexts.CHAT_TITLE));
        title.addClassName("chat-title");

        conversationList.addClassName("chat-list");
        emptyList.addClassName("chat-empty-list");

        activeHeader.addClassName("chat-active-header");
        messagesView.addClassName("chat-messages");

        input.setPlaceholder(I18n.t(UiTexts.CHAT_INPUT_PLACEHOLDER));
        input.setClearButtonVisible(true);
        input.addClassName("chat-input");
        input.setWidthFull();

        sendButton.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        sendButton.addClickListener(_ -> sendActive());
        input.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, _ -> sendActive());

        HorizontalLayout inputRow = new HorizontalLayout(input, sendButton);
        inputRow.setWidthFull();
        inputRow.setFlexGrow(1, input);
        inputRow.addClassName("chat-input-row");

        conversationArea.addClassName("chat-active");
        conversationArea.add(activeHeader, messagesView, inputRow);
        conversationArea.setVisible(false);

        add(title, conversationList, emptyList, conversationArea);
        refreshList();
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        subscription = broadcaster.subscribe(event -> {
            if (event instanceof SocialEvent.DirectMessage dm) {
                ui.access(() -> handleIncoming(dm));
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (subscription != null) {
            subscription.remove();
            subscription = null;
        }
    }

    public void openChatWith(String otherUser) {
        String me = UserContext.currentUsername().orElse(null);
        if (me == null || otherUser == null || otherUser.equalsIgnoreCase(me)) {
            return;
        }
        String key = otherUser.toLowerCase(Locale.ROOT);
        conversations.computeIfAbsent(key, _ -> new Conversation(otherUser));
        setActive(key);
    }

    private void handleIncoming(SocialEvent.DirectMessage dm) {
        String me = UserContext.currentUsername().orElse(null);
        if (me == null) {
            return;
        }
        String meLc = me.toLowerCase(Locale.ROOT);
        if (!dm.from().equals(meLc) && !dm.to().equals(meLc)) {
            return;
        }
        String other = dm.from().equals(meLc) ? dm.to() : dm.from();
        Conversation conv = conversations.computeIfAbsent(other, _ -> new Conversation(other));
        conv.messages.add(new DirectMessage(dm.from(), dm.to(), dm.text(), dm.sentAt()));
        if (!other.equals(activeUser) && !dm.from().equals(meLc)) {
            conv.unread++;
        }
        if (other.equals(activeUser)) {
            renderMessages(conv);
        }
        refreshList();
    }

    private void sendActive() {
        if (activeUser == null) {
            return;
        }
        String me = UserContext.currentUsername().orElse(null);
        if (me == null) {
            return;
        }
        String text = input.getValue();
        MessageService.SendResult result = messages.send(me, activeUser, text);
        switch (result) {
            case DELIVERED -> input.clear();
            case EMPTY -> Notification.show(I18n.t(UiTexts.CHAT_SEND_EMPTY));
            case RECIPIENT_OFFLINE -> Notification.show(I18n.t(UiTexts.CHAT_SEND_OFFLINE));
            case NOT_VISIBLE -> Notification.show(I18n.t(UiTexts.CHAT_SEND_NOT_VISIBLE));
            case REJECTED -> Notification.show(I18n.t(UiTexts.CHAT_SEND_REJECTED));
        }
    }

    private void setActive(String key) {
        activeUser = key;
        Conversation conv = conversations.get(key);
        if (conv == null) {
            return;
        }
        conv.unread = 0;
        activeHeader.setText(conv.displayName);
        renderMessages(conv);
        conversationArea.setVisible(true);
        input.focus();
        refreshList();
    }

    private void renderMessages(Conversation conv) {
        String me = UserContext.currentUsername().orElse("").toLowerCase(Locale.ROOT);
        messagesView.removeAll();
        if (conv.messages.isEmpty()) {
            Span empty = new Span(I18n.t(UiTexts.CHAT_EMPTY_CONVERSATION));
            empty.addClassName("chat-empty-conversation");
            messagesView.add(empty);
            return;
        }
        for (DirectMessage m : conv.messages) {
            Div bubble = new Div();
            bubble.addClassName("chat-bubble");
            bubble.addClassName(m.from().equals(me) ? "chat-bubble-mine" : "chat-bubble-theirs");
            Span text = new Span(m.text());
            text.addClassName("chat-bubble-text");
            Span meta = new Span(TIME.format(m.sentAt()));
            meta.addClassName("chat-bubble-meta");
            bubble.add(text, meta);
            messagesView.add(bubble);
        }
        messagesView.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }

    private void refreshList() {
        conversationList.removeAll();
        boolean any = !conversations.isEmpty();
        emptyList.setVisible(!any);
        conversationList.setVisible(any);
        for (Conversation conv : conversations.values()) {
            conversationList.add(renderListRow(conv));
        }
    }

    private Component renderListRow(Conversation conv) {
        Div row = new Div();
        row.addClassName("chat-list-row");
        if (conv.key().equals(activeUser)) {
            row.addClassName("chat-list-row-active");
        }
        Span name = new Span(conv.displayName);
        name.addClassName("chat-list-name");
        row.add(name);
        if (conv.unread > 0) {
            Span badge = new Span(I18n.t(UiTexts.CHAT_UNREAD_BADGE, conv.unread));
            badge.addClassName("chat-unread-badge");
            row.add(badge);
        }
        row.getElement().addEventListener("click", _ -> setActive(conv.key()));
        return row;
    }

    private static final class Conversation {
        final String displayName;
        final List<DirectMessage> messages = new ArrayList<>();
        int unread;

        Conversation(String displayName) {
            this.displayName = displayName;
        }

        String key() {
            return displayName.toLowerCase(Locale.ROOT);
        }
    }
}
