package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.EmpireNameGenerator;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;

/** Collects the per-game empire name before a registered account claims a human seat. */
final class JoinGameDialog {
    private JoinGameDialog() {
    }

    static void open(GameService game, GameSummary summary, String accountId, String playerName, Runnable onJoined) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.LOBBY_JOIN_TITLE));

        Div gameDetails = new Div();
        gameDetails.addClassName("join-game-details");
        gameDetails.add(new Span(I18n.t(UiTexts.LOBBY_JOIN_GAME, summary.name())),
                new Span(I18n.t(UiTexts.LOBBY_JOIN_SEATS, availableSeats(summary))));

        TextField empireName = new TextField(I18n.t(UiTexts.LOBBY_FIELD_EMPIRE_NAME));
        empireName.setValue(EmpireNameGenerator.forHuman(playerName));
        empireName.setRequiredIndicatorVisible(true);
        empireName.setWidthFull();
        dialog.add(gameDetails, empireName);

        Button cancel = new Button(I18n.t(UiTexts.LOBBY_WIZARD_CANCEL), _ -> dialog.close());
        Button join = new Button(I18n.t(UiTexts.LOBBY_ACTION_JOIN), _ -> {
            String selectedEmpireName = empireName.getValue().trim();
            if (selectedEmpireName.isEmpty()) {
                empireName.setInvalid(true);
                empireName.setErrorMessage(I18n.t(UiTexts.LOBBY_EMPIRE_NAME_REQUIRED));
                return;
            }
            if (game.joinGame(summary.gameId(), accountId, playerName, selectedEmpireName).isEmpty()) {
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

    private static String availableSeats(GameSummary summary) {
        return summary.players().stream()
                .filter(player -> !player.ai())
                .filter(player -> !summary.joinedHumanSeats().contains(player.id()))
                .filter(player -> !summary.invitedSeats().containsValue(player.id()))
                .map(Player::label)
                .reduce((left, right) -> left + ", " + right)
                .orElse(I18n.t(UiTexts.LOBBY_JOIN_NO_SEATS));
    }
}
