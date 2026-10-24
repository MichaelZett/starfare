package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameChatService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.i18n.I18n;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameChatPanelTest {
    @Test
    void closingAndReopeningKeepsTheUnsentDraft() {
        var closed = new AtomicInteger();
        var panel = panel(mock(GameChatService.class), closed);
        TextArea input = descendants(panel).filter(TextArea.class::isInstance).map(TextArea.class::cast).findFirst().orElseThrow();
        panel.open();
        input.setValue("Noch nicht senden");
        panel.close();
        assertThat(panel.isVisible()).isFalse();
        assertThat(closed.get()).isEqualTo(1);
        panel.open();
        assertThat(input.getValue()).isEqualTo("Noch nicht senden");
        assertThat(panel.isVisible()).isTrue();
    }

    @Test
    void rejectedSendKeepsTheDraftAndOnlyAnAcceptedSendClearsIt() {
        var chat = mock(GameChatService.class);
        var panel = panel(chat, new AtomicInteger());
        TextArea input = descendants(panel).filter(TextArea.class::isInstance).map(TextArea.class::cast).findFirst().orElseThrow();
        Button send = descendants(panel).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getText().equals(I18n.t(UiTexts.CHAT_SEND))).findFirst().orElseThrow();
        input.setValue("Hallo");
        send.click();
        assertThat(input.getValue()).isEqualTo("Hallo");
        assertThat(descendants(panel).anyMatch(component -> component.getElement().getText()
                .equals(I18n.t(UiTexts.CHAT_SEND_FAILED)))).isTrue();
        when(chat.send(GameId.of("chat-test"), "", "Hallo")).thenReturn(true);
        send.click();
        assertThat(input.getValue()).isEmpty();
    }

    private static GameChatPanel panel(GameChatService chat, AtomicInteger closed) {
        return new GameChatPanel(chat, mock(PlayerDirectory.class), GameId.of("chat-test"), mock(Broadcaster.class),
                closed::incrementAndGet);
    }

    private static Stream<Component> descendants(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }
}
