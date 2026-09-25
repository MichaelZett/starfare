package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.game.values.GameOutcomeStatistics;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;

/** Final result, deliberately opened only after the current report's battles are acknowledged. */
final class GameOutcomeDialog {

    private GameOutcomeDialog() {}

    static void open(boolean victory, String winnerName, String winnerColor,
                     GameOutcomeStatistics statistics, Runnable onReview, Runnable onLobby) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.addClassName("game-outcome-dialog");
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.addClassName("game-outcome-content");
        H2 title = new H2(I18n.t(victory ? UiTexts.GAME_OUTCOME_VICTORY : UiTexts.GAME_OUTCOME_DEFEAT, winnerName));
        title.addClassName(victory ? "game-outcome-victory" : "game-outcome-defeat");
        Span winner = new Span(winnerName);
        winner.addClassName("game-outcome-winner");
        if (winnerColor != null && !winnerColor.isBlank()) {
            winner.getStyle().set(CssProperties.BORDER_COLOR, winnerColor);
        }
        Span personalResult = new Span(I18n.t(victory
                ? UiTexts.GAME_OUTCOME_PERSONAL_VICTORY : UiTexts.GAME_OUTCOME_PERSONAL_DEFEAT));
        personalResult.addClassName("game-outcome-personal-result");
        Div metrics = new Div(metric("◷", UiTexts.GAME_OUTCOME_ROUNDS, statistics.rounds()),
                metric("✦", UiTexts.GAME_OUTCOME_SYSTEMS, statistics.systems()),
                metric("⬆", UiTexts.GAME_OUTCOME_BUILT, statistics.shipsBuilt()),
                metric("⚔", UiTexts.GAME_OUTCOME_DESTROYED, statistics.shipsDestroyed()),
                metric("✕", UiTexts.GAME_OUTCOME_LOST, statistics.shipsLost()));
        metrics.addClassName("game-outcome-metrics");
        content.add(title, winner, personalResult, metrics);
        Button review = new Button(I18n.t(UiTexts.GAME_OUTCOME_REVIEW), _ -> {
            dialog.close();
            onReview.run();
        });
        review.addClassName("game-outcome-review");
        review.addThemeVariants(ButtonVariant.PRIMARY);
        Button lobby = new Button(I18n.t(UiTexts.GAME_OUTCOME_LOBBY), _ -> {
            dialog.close();
            onLobby.run();
        });
        lobby.addClassName("game-outcome-lobby");
        dialog.add(content);
        dialog.getFooter().add(lobby, review);
        dialog.open();
    }

    private static Div metric(String icon, String key, int value) {
        Span iconSpan = new Span(icon);
        iconSpan.addClassName("game-outcome-metric-icon");
        Span number = new Span(String.valueOf(value));
        number.addClassName("game-outcome-metric-value");
        Span label = new Span(I18n.t(key, value));
        label.addClassName("game-outcome-metric-label");
        Div metric = new Div(iconSpan, number, label);
        metric.getElement().setAttribute("aria-label", I18n.t(key, value));
        metric.getElement().setAttribute("title", I18n.t(key, value));
        return metric;
    }
}
