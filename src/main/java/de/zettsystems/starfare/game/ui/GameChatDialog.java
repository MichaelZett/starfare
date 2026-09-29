package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameChatService;
import de.zettsystems.starfare.game.values.GameChatMessage;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.i18n.I18n;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Modal chat for all participants of the current game. */
final class GameChatDialog {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private final GameChatService chat;
    private final PlayerDirectory players;
    private final GameId gameId;
    private final Dialog dialog = new Dialog();
    private final Div messages = new Div();
    private final TextArea input = new TextArea();

    GameChatDialog(GameChatService chat, PlayerDirectory players, GameId gameId) {
        this.chat = chat;
        this.players = players;
        this.gameId = gameId;
        dialog.setHeaderTitle(I18n.t(UiTexts.GAME_CHAT_TITLE));
        dialog.setWidth("min(42rem, 94vw)");
        messages.addClassName("game-chat-messages");
        input.setPlaceholder(I18n.t(UiTexts.CHAT_INPUT_PLACEHOLDER));
        input.setMaxLength(GameChatMessage.MAX_LENGTH);
        input.setWidthFull();
        Button send = new Button(I18n.t(UiTexts.CHAT_SEND), _ -> send());
        send.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout footer = new HorizontalLayout(input, send);
        footer.setWidthFull();
        footer.setFlexGrow(1, input);
        dialog.add(new H2(I18n.t(UiTexts.GAME_CHAT_HINT)), messages, footer);
        dialog.getFooter().add(new Button(I18n.t(UiTexts.MANAGE_CLOSE), _ -> dialog.close()));
    }

    void open() {
        reload();
        dialog.open();
        input.focus();
    }

    private void reload() {
        String account = UserContext.currentPlayerId().orElse("");
        messages.removeAll();
        for (GameChatMessage message : chat.messagesFor(gameId, account)) {
            Div line = new Div();
            Span meta = new Span(players.displayName(message.senderPlayerId()) + " · " + TIME.format(message.sentAt()));
            Span body = new Span(message.text());
            line.add(meta, body);
            messages.add(line);
        }
        messages.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }

    private void send() {
        if (chat.send(gameId, UserContext.currentPlayerId().orElse(""), input.getValue())) {
            input.clear();
            reload();
        }
    }
}
