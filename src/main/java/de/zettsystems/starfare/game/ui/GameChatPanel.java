package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameChatService;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.values.GameChatMessage;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import org.jspecify.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Game-local chat; hiding it preserves the draft without retaining an event subscription. */
final class GameChatPanel extends Div {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private final GameChatService chat;
    private final PlayerDirectory players;
    private final GameId gameId;
    private final Broadcaster broadcaster;
    private final Runnable closed;
    private @Nullable Subscription subscription;
    private final Div messages = new Div();
    private final TextArea input = new TextArea();
    private final Button newMessages = new Button(I18n.t(UiTexts.CHAT_NEW_MESSAGES));
    private final Span feedback = new Span();
    private List<GameChatMessage> rendered = List.of();
    private boolean opened;

    GameChatPanel(GameChatService chat, PlayerDirectory players, GameId gameId, Broadcaster broadcaster, Runnable closed) {
        this.chat = chat;
        this.players = players;
        this.gameId = gameId;
        this.broadcaster = broadcaster;
        this.closed = closed;
        addClassName("game-chat-panel");
        getElement().setAttribute("role", "region");
        getElement().setAttribute("aria-label", I18n.t(UiTexts.GAME_CHAT_TITLE));
        Button close = new Button(I18n.t(UiTexts.MANAGE_CLOSE), _ -> close());
        close.addThemeVariants(ButtonVariant.TERTIARY);
        HorizontalLayout heading = new HorizontalLayout(new H2(I18n.t(UiTexts.GAME_CHAT_TITLE)), close);
        heading.setAlignItems(HorizontalLayout.Alignment.CENTER);
        heading.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        messages.addClassName("game-chat-messages");
        messages.getElement().setAttribute("role", "log");
        messages.getElement().setAttribute("aria-live", "polite");
        input.setPlaceholder(I18n.t(UiTexts.CHAT_INPUT_PLACEHOLDER));
        input.setAriaLabel(I18n.t(UiTexts.CHAT_INPUT_PLACEHOLDER));
        input.setMaxLength(GameChatMessage.MAX_LENGTH);
        input.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        input.setWidthFull();
        Button send = new Button(I18n.t(UiTexts.CHAT_SEND), _ -> send());
        send.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout footer = new HorizontalLayout(input, send);
        footer.setAlignItems(HorizontalLayout.Alignment.END);
        footer.setWidthFull();
        footer.setFlexGrow(1, input);
        feedback.getElement().setAttribute("role", "status");
        newMessages.getElement().setProperty("hidden", true);
        newMessages.addClickListener(_ -> followLatest());
        add(heading, new Span(I18n.t(UiTexts.GAME_CHAT_HINT)), messages, newMessages, feedback, footer);
        addAttachListener(_ -> {
            getElement().executeJs("""
                    if (!this._sfChatKeys) {
                      this._sfChatKeys = true;
                      this.addEventListener('keydown', event => {
                        if (event.key !== 'Tab' || !(matchMedia('(max-width: 1100px)').matches
                            || document.documentElement.classList.contains('starfare-mobile'))) return;
                        const items = [...this.querySelectorAll('vaadin-button:not([disabled]), vaadin-text-area')]
                          .filter(item => item.getClientRects().length && !item.hidden);
                        if (!items.length) return;
                        const first = items[0], last = items[items.length - 1];
                        if (event.shiftKey && document.activeElement === first) {
                          event.preventDefault(); last.focus();
                        } else if (!event.shiftKey && document.activeElement === last) {
                          event.preventDefault(); first.focus();
                        }
                      });
                    }
                    """);
            messages.getElement().executeJs("""
                    if (!this._sfChatScroll) {
                      this._sfChatScroll = true;
                      this.addEventListener('scroll', () => {
                        this._sfFollow = this.scrollHeight - this.clientHeight - this.scrollTop < 40;
                        if (this._sfFollow) $0.hidden = true;
                      });
                    }
                    """, newMessages.getElement());
            subscribe();
        });
        addDetachListener(_ -> unsubscribe());
        getElement().addEventListener("keydown", _ -> close()).setFilter("event.key === 'Escape'");
    }

    void open() {
        opened = true;
        setVisible(true);
        subscribe();
        reload(false);
        input.focus();
    }

    void close() {
        if (!opened) { return; }
        opened = false;
        unsubscribe();
        setVisible(false);
        closed.run();
    }

    @SuppressWarnings("FutureReturnValueIgnored") // Vaadin schedules access on the owning UI.
    private void subscribe() {
        if (subscription != null || !opened) { return; }
        getUI().ifPresent(ui -> subscription = broadcaster.subscribe(gameId, event -> {
            if (event instanceof GameEvent.ChatMessage) {
                ui.access(() -> { if (opened) { reload(false); } });
            }
        }));
    }

    private void unsubscribe() {
        Subscription current = subscription;
        subscription = null;
        if (current != null) { current.remove(); }
    }

    private void reload(boolean ownMessage) {
        List<GameChatMessage> latest = chat.messagesFor(gameId, UserContext.currentPlayerId().orElse(""));
        boolean appended = latest.size() >= rendered.size() && latest.subList(0, rendered.size()).equals(rendered);
        int start = appended ? rendered.size() : 0;
        if (!appended) { messages.removeAll(); }
        for (int index = start; index < latest.size(); index++) {
            GameChatMessage message = latest.get(index);
            Span meta = new Span(players.displayName(message.senderPlayerId()) + " · " + TIME.format(message.sentAt()));
            Span body = new Span(message.text());
            messages.add(new Div(meta, body));
        }
        boolean changed = !latest.equals(rendered);
        rendered = List.copyOf(latest);
        if (changed || ownMessage) {
            messages.getElement().executeJs("""
                    if ($0 || this._sfFollow !== false) {
                      this.scrollTop = this.scrollHeight;
                      this._sfFollow = true;
                      $1.hidden = true;
                    } else { $1.hidden = false; }
                    """, ownMessage, newMessages.getElement());
        }
    }

    private void followLatest() {
        messages.getElement().executeJs("this._sfFollow = true; this.scrollTop = this.scrollHeight;");
        newMessages.getElement().executeJs("this.hidden = true;");
    }

    private void send() {
        if (chat.send(gameId, UserContext.currentPlayerId().orElse(""), input.getValue())) {
            input.clear();
            feedback.setText("");
            reload(true);
        } else {
            feedback.setText(I18n.t(UiTexts.CHAT_SEND_FAILED));
        }
    }
}
