package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.game.values.EconomyTotals;
import de.zettsystems.starfare.game.values.GameOutcomeStatistics;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;

/** Final result, deliberately opened only after the current report's battles are acknowledged. */
final class GameOutcomeDialog {

    private GameOutcomeDialog() {}

    /** {@code continuable}: offer continuing towards full conquest, false once it already is the goal. */
    static Dialog open(boolean victory, boolean continuable, String winnerName, String winnerColor,
                     GameOutcomeStatistics statistics, Runnable onContinue, Runnable onReview, Runnable onLobby) {
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
        metrics.setWidthFull();
        content.add(title, winner, personalResult, metrics);
        var economy = statistics.economy();
        if (economy != null) { content.add(economy(economy)); }
        Button review = new Button(I18n.t(UiTexts.GAME_OUTCOME_REVIEW), _ -> {
            dialog.close();
            onReview.run();
        });
        review.addClassName("game-outcome-review");
        review.addThemeVariants(ButtonVariant.PRIMARY);
        Button continueGame = new Button(I18n.t(UiTexts.GAME_OUTCOME_CONTINUE), _ -> {
            dialog.close();
            onContinue.run();
        });
        continueGame.setVisible(victory && continuable);
        Button lobby = new Button(I18n.t(UiTexts.GAME_OUTCOME_LOBBY), _ -> {
            dialog.close();
            onLobby.run();
        });
        lobby.addClassName("game-outcome-lobby");
        dialog.add(content);
        dialog.getFooter().add(lobby, continueGame, review);
        dialog.open();
        return dialog;
    }

    /** Spaceward only: recorded sums, never an invented zero for missing history. */
    private static Div economy(EconomyTotals economy) {
        Div section = new Div();
        section.addClassName("game-outcome-economy");
        if (!economy.recorded()) {
            section.add(new Span(I18n.t(UiTexts.GAME_OUTCOME_ECONOMY_NOT_RECORDED)));
            return section;
        }
        Div metrics = new Div();
        metrics.addClassName("game-outcome-metrics");
        metrics.setWidthFull();
        if (economy.metal()) {
            metrics.add(metric("⛏", UiTexts.GAME_OUTCOME_METAL_MINED, economy.metalMined()),
                    metric("⬇", UiTexts.GAME_OUTCOME_METAL_CONSUMED, economy.metalConsumed()));
        }
        metrics.add(metric("⚙", UiTexts.GAME_OUTCOME_INDUSTRY_BUILT, economy.industryBuilt()));
        section.add(metrics);
        switch (economy.coverage()) {
            case SINCE -> section.add(new Span(I18n.t(UiTexts.GAME_OUTCOME_ECONOMY_SINCE, economy.firstRound())));
            case PARTIAL -> section.add(new Span(I18n.t(UiTexts.GAME_OUTCOME_ECONOMY_PARTIAL)));
            case COMPLETE, NOT_RECORDED -> { }
        }
        return section;
    }

    private static Div metric(String icon, String key, long value) {
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
