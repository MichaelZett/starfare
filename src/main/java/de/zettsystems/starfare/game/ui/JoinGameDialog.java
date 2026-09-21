package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.EmpireNameGenerator;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.i18n.I18n;

/** Collects the per-game empire name before a registered account claims a human seat. */
final class JoinGameDialog {
    private JoinGameDialog() {
    }

    static void open(GameService game, GameId gameId, String accountId, String playerName, Runnable onJoined) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.LOBBY_JOIN_TITLE));

        TextField empireName = new TextField(I18n.t(UiTexts.LOBBY_FIELD_EMPIRE_NAME));
        empireName.setValue(EmpireNameGenerator.forHuman(playerName));
        empireName.setRequiredIndicatorVisible(true);
        empireName.setWidthFull();
        dialog.add(empireName);

        Button cancel = new Button(I18n.t(UiTexts.LOBBY_WIZARD_CANCEL), _ -> dialog.close());
        Button join = new Button(I18n.t(UiTexts.LOBBY_ACTION_JOIN), _ -> {
            String selectedEmpireName = empireName.getValue().trim();
            if (selectedEmpireName.isEmpty()) {
                empireName.setInvalid(true);
                empireName.setErrorMessage(I18n.t(UiTexts.LOBBY_EMPIRE_NAME_REQUIRED));
                return;
            }
            if (game.joinGame(gameId, accountId, playerName, selectedEmpireName).isEmpty()) {
                Notification.show(I18n.t(UiTexts.LOBBY_JOIN_FAILED));
                return;
            }
            Notification.show(I18n.t(UiTexts.LOBBY_JOINED));
            onJoined.run();
            dialog.close();
        });
        join.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(cancel, join);
        dialog.open();
    }
}
