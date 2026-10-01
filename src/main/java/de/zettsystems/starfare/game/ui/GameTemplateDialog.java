package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameTemplateService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.i18n.I18n;

final class GameTemplateDialog {
    private GameTemplateDialog() { }

    static Button button(GameTemplateService templates, GameId source, String sourceName, Runnable created) {
        Button button = new Button(I18n.t(UiTexts.PLAN_TEMPLATE), _ -> open(templates, source, sourceName, created));
        button.setEnabled(templates.template(source, UserContext.currentPlayerId().orElse("")).isPresent());
        if (!button.isEnabled()) { button.setTooltipText(I18n.t(UiTexts.PLAN_TEMPLATE_MISSING)); }
        return button;
    }

    private static void open(GameTemplateService templates, GameId source, String sourceName, Runnable created) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.PLAN_TEMPLATE));
        TextField name = new TextField(I18n.t(UiTexts.PLAN_GAME_NAME));
        name.setMaxLength(100);
        name.setValue(sourceName);
        Checkbox rematch = new Checkbox(I18n.t(UiTexts.PLAN_REMATCH));
        dialog.add(name, rematch, new Paragraph(I18n.t(UiTexts.PLAN_TEMPLATE_HINT)));
        Button create = new Button(I18n.t(UiTexts.LOBBY_NEW_GAME), _ -> {
            var result = templates.create(source, UserContext.currentPlayerId().orElse(""), name.getValue(), rematch.getValue());
            if (result.isEmpty()) { Notification.show(I18n.t(UiTexts.PLAN_REJECTED)); return; }
            var attachedUi = dialog.getUI();
            created.run();
            dialog.close();
            attachedUi.ifPresent(ui -> ui.navigate(LobbyView.class));
        });
        dialog.getFooter().add(new Button(I18n.t(UiTexts.ACCOUNT_DELETE_CANCEL), _ -> dialog.close()), create);
        dialog.open();
    }
}
