package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
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
        dialog.addClassName("game-outcome-dialog");
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.addClassName("game-outcome-content");
        H2 title = new H2(I18n.t(victory ? UiTexts.GAME_OUTCOME_VICTORY : UiTexts.GAME_OUTCOME_DEFEAT, winnerName));
        title.addClassName(victory ? "game-outcome-victory" : "game-outcome-defeat");
        Div metrics = new Div(metric("◷", UiTexts.GAME_OUTCOME_ROUNDS, statistics.rounds()),
                metric("✦", UiTexts.GAME_OUTCOME_SYSTEMS, statistics.systems()),
                metric("⬆", UiTexts.GAME_OUTCOME_BUILT, statistics.shipsBuilt()),
                metric("⚔", UiTexts.GAME_OUTCOME_DESTROYED, statistics.shipsDestroyed()),
                metric("✕", UiTexts.GAME_OUTCOME_LOST, statistics.shipsLost()));
        metrics.addClassName("game-outcome-metrics");
        content.add(title, metrics);
        Button close = new Button(I18n.t(UiTexts.GAME_OUTCOME_CONTINUE), _ -> {
            dialog.close();
            onClose.run();
        });
        dialog.add(content);
        dialog.getFooter().add(close);
        dialog.open();
    }

    private static Div metric(String icon, String key, int value) {
        Span iconSpan = new Span(icon);
        iconSpan.addClassName("game-outcome-metric-icon");
        Span number = new Span(String.valueOf(value));
        number.addClassName("game-outcome-metric-value");
        Div metric = new Div(iconSpan, number);
        metric.getElement().setAttribute("aria-label", I18n.t(key, value));
        metric.getElement().setAttribute("title", I18n.t(key, value));
        return metric;
    }
}
