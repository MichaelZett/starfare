package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.game.values.GameOutcomeStatistics;
import de.zettsystems.starfare.i18n.I18n;

/** Final result, deliberately opened only after the current report's battles are acknowledged. */
final class GameOutcomeDialog {

    private GameOutcomeDialog() {}

    static void open(boolean victory, String winnerName, GameOutcomeStatistics statistics, Runnable onClose) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.add(new H2(I18n.t(victory ? UiTexts.GAME_OUTCOME_VICTORY : UiTexts.GAME_OUTCOME_DEFEAT,
                        winnerName)),
                line(UiTexts.GAME_OUTCOME_ROUNDS, statistics.rounds()),
                line(UiTexts.GAME_OUTCOME_SYSTEMS, statistics.systems()),
                line(UiTexts.GAME_OUTCOME_BUILT, statistics.shipsBuilt()),
                line(UiTexts.GAME_OUTCOME_DESTROYED, statistics.shipsDestroyed()),
                line(UiTexts.GAME_OUTCOME_LOST, statistics.shipsLost()));
        Button close = new Button(I18n.t(UiTexts.GAME_OUTCOME_CONTINUE), _ -> {
            dialog.close();
            onClose.run();
        });
        dialog.add(content);
        dialog.getFooter().add(close);
        dialog.open();
    }

    private static Div line(String key, int value) {
        return new Div(I18n.t(key, value));
    }
}
