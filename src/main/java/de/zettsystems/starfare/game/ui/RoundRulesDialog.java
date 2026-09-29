package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.i18n.I18n;

import java.time.Duration;

/** Lets the host adjust the two deadlines while a game is running. */
final class RoundRulesDialog {
    private RoundRulesDialog() {}

    static void open(GameService game, GameId gameId, Runnable onSaved) {
        String account = UserContext.currentPlayerId().orElse("");
        RoundRules current = game.roundRulesFor(gameId, account).orElse(null);
        if (current == null) return;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.ROUND_RULES_TITLE));
        ComboBox<Duration> roundLimit = field(UiTexts.LOBBY_FIELD_ROUND_LIMIT,
                RoundRules.ROUND_LIMIT_CHOICES, current.roundLimit());
        ComboBox<Duration> stragglerLimit = field(UiTexts.LOBBY_FIELD_STRAGGLER_LIMIT,
                RoundRules.STRAGGLER_LIMIT_CHOICES, current.stragglerLimit());
        VerticalLayout form = new VerticalLayout(roundLimit, stragglerLimit);
        form.setPadding(false);
        dialog.add(form);
        Button save = new Button(I18n.t(UiTexts.ROUND_RULES_SAVE), _ -> {
            RoundRules updated = new RoundRules(roundLimit.getValue(), stragglerLimit.getValue(), current.attackOrder());
            if (game.updateRoundRules(gameId, account, updated)) {
                dialog.close();
                onSaved.run();
            }
        });
        save.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(new Button(I18n.t(UiTexts.MANAGE_CLOSE), _ -> dialog.close()), save);
        dialog.open();
    }

    private static ComboBox<Duration> field(String label, java.util.List<Duration> choices, Duration value) {
        ComboBox<Duration> field = new ComboBox<>(I18n.t(label));
        field.setItems(choices);
        field.setItemLabelGenerator(DurationLabels::label);
        field.setValue(value);
        return field;
    }
}
