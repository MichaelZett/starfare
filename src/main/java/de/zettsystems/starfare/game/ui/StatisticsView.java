package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameStatisticsService;
import de.zettsystems.starfare.game.values.CompletedGameOutcome;
import de.zettsystems.starfare.game.values.CompletedGameStatistics;
import de.zettsystems.starfare.game.values.OpponentStatistics;
import de.zettsystems.starfare.game.values.PlayerStatistics;
import de.zettsystems.starfare.i18n.I18n;
import jakarta.annotation.security.PermitAll;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;
import java.util.List;
import java.util.stream.Stream;

/** Personal overview of completed games, retained independently from archived sessions. */
@Route("statistics")
@PermitAll
@CssImport("./styles/starfare.css")
public class StatisticsView extends VerticalLayout {
    private final GameStatisticsService statistics;
    private final Span totals = new Span();
    private final Div summaryCards = new Div();
    private final Div opponentCards = new Div();
    private final Div completedGames = new Div();
    private final Paragraph empty = new Paragraph(I18n.t(UiTexts.STATISTICS_EMPTY));
    private final Paragraph noOpponents = new Paragraph(I18n.t(UiTexts.STATISTICS_OPPONENTS_EMPTY));
    private final PlayerDirectory players;

    public StatisticsView(GameStatisticsService statistics, PlayerDirectory players) {
        this.statistics = statistics;
        this.players = players;
        setSizeFull();
        addClassName("statistics-view");
        summaryCards.addClassName("statistics-summary-cards");
        opponentCards.addClassName("statistics-opponent-cards");
        completedGames.addClassName("statistics-history-cards");
        noOpponents.addClassName("statistics-empty");
        add(new H1(I18n.t(UiTexts.STATISTICS_TITLE)), summaryCards, totals,
                new HorizontalLayout(new Button(I18n.t(UiTexts.MAP_ACTION_LOBBY),
                        _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)))),
                new H2(I18n.t(UiTexts.STATISTICS_OPPONENTS)), opponentCards, noOpponents,
                new H2(I18n.t(UiTexts.STATISTICS_HISTORY)), completedGames, empty);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        refresh();
    }

    private void refresh() {
        PlayerStatistics view = statistics.statisticsFor(UserContext.currentPlayerId().orElse(""));
        int draws = Math.max(0, view.games() - view.wins() - view.losses());
        Map<String, String> displayNames = players.displayNames(Stream.concat(
                        view.opponents().stream().map(OpponentStatistics::opponentId),
                        view.completedGames().stream().flatMap(game -> game.opponentIds().stream()))
                .distinct().toList());
        totals.setText(I18n.t(UiTexts.STATISTICS_TOTALS, view.games(), view.wins(), view.losses(), draws));
        summaryCards.removeAll();
        summaryCards.add(statCard("◷", UiTexts.STATISTICS_GAMES, view.games()),
                statCard("🏆", UiTexts.STATISTICS_WINS, view.wins()),
                statCard("✕", UiTexts.STATISTICS_LOSSES, view.losses()),
                balanceCard(view));
        renderCompletedGames(view.completedGames(), displayNames);
        renderOpponents(view.opponents(), displayNames);
        empty.setVisible(view.games() == 0);
        noOpponents.setVisible(view.games() > 0 && view.opponents().isEmpty());
    }

    private static Div statCard(String icon, String key, int value) {
        Span iconSpan = new Span(icon);
        Span number = new Span(String.valueOf(value));
        number.addClassName("statistics-summary-value");
        Span label = new Span(I18n.t(key));
        Div card = new Div(iconSpan, number, label);
        card.addClassName("statistics-summary-card");
        return card;
    }

    private static Div balanceCard(PlayerStatistics view) {
        int games = view.games();
        int winPercent = games == 0 ? 0 : Math.round(100f * view.wins() / games);
        int draws = Math.max(0, games - view.wins() - view.losses());
        int lossPercent = games == 0 ? 0 : Math.round(100f * view.losses() / games);
        int drawPercent = Math.max(0, 100 - winPercent - lossPercent);
        Span label = new Span(I18n.t(UiTexts.STATISTICS_BALANCE, winPercent));
        Div track = new Div();
        track.addClassName("statistics-balance-track");
        track.getElement().setAttribute("role", "img");
        track.getElement().setAttribute("aria-label",
                I18n.t(UiTexts.STATISTICS_BALANCE_LABEL, view.wins(), view.losses(), draws));
        track.add(balanceSegment("statistics-balance-win", winPercent),
                balanceSegment("statistics-balance-loss", lossPercent),
                balanceSegment("statistics-balance-draw", drawPercent));
        Div legend = new Div(balanceLegend(UiTexts.STATISTICS_WINS, view.wins(), "statistics-legend-win"),
                balanceLegend(UiTexts.STATISTICS_LOSSES, view.losses(), "statistics-legend-loss"),
                balanceLegend(UiTexts.STATISTICS_DRAWS, draws, "statistics-legend-draw"));
        legend.addClassName("statistics-balance-legend");
        Div card = new Div(label, track, legend);
        card.addClassName("statistics-summary-card");
        return card;
    }

    private static Div balanceSegment(String style, int percent) {
        Div segment = new Div();
        segment.addClassName(style);
        segment.getStyle().set("width", percent + "%");
        return segment;
    }

    private static Div balanceLegend(String key, int count, String style) {
        Span swatch = new Span();
        swatch.addClassName("statistics-legend-swatch");
        swatch.addClassName(style);
        Span text = new Span(I18n.t(UiTexts.STATISTICS_BALANCE_LEGEND, I18n.t(key), count));
        Div item = new Div(swatch, text);
        item.addClassName("statistics-legend-item");
        return item;
    }

    private void renderOpponents(List<OpponentStatistics> opponents, Map<String, String> displayNames) {
        opponentCards.removeAll();
        for (OpponentStatistics opponent : opponents) {
            int draws = Math.max(0, opponent.games() - opponent.wins() - opponent.losses());
            String displayName = displayNames.getOrDefault(opponent.opponentId(), opponent.opponentId());
            Span name = new Span(displayName);
            name.addClassName("statistics-opponent-name");
            name.getElement().setAttribute("title", displayName);
            Div facts = new Div(
                    opponentFact(UiTexts.STATISTICS_GAMES, opponent.games()),
                    opponentFact(UiTexts.STATISTICS_WINS, opponent.wins()),
                    opponentFact(UiTexts.STATISTICS_LOSSES, opponent.losses()),
                    opponentFact(UiTexts.STATISTICS_DRAWS, draws));
            facts.addClassName("statistics-opponent-facts");
            Div card = new Div(name, facts);
            card.addClassName("statistics-opponent-card");
            opponentCards.add(card);
        }
    }

    private static Span opponentFact(String key, int count) {
        Span fact = new Span(I18n.t(key) + ": " + count);
        fact.addClassName("statistics-opponent-fact");
        return fact;
    }

    private void renderCompletedGames(List<CompletedGameStatistics> games, Map<String, String> displayNames) {
        completedGames.removeAll();
        for (CompletedGameStatistics game : games) {
            Span name = new Span(game.gameName());
            name.addClassName("statistics-history-name");
            Span outcome = new Span(outcomeText(game.outcome()));
            outcome.addClassName("statistics-history-outcome");
            Span finished = new Span(finishedAtText(game));
            Span opponents = new Span(opponentsText(game, displayNames));
            opponents.addClassName("statistics-history-opponents");
            Div card = new Div(name, outcome, finished, opponents);
            card.addClassName("statistics-history-card");
            completedGames.add(card);
        }
    }

    private String outcomeText(CompletedGameOutcome outcome) {
        return switch (outcome) {
            case WIN -> I18n.t(UiTexts.STATISTICS_WIN);
            case LOSS -> I18n.t(UiTexts.STATISTICS_LOSS);
            case DRAW -> I18n.t(UiTexts.STATISTICS_DRAW);
        };
    }

    private String finishedAtText(CompletedGameStatistics game) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(getLocale())
                .withZone(ZoneId.systemDefault())
                .format(game.finishedAt());
    }

    private String opponentsText(CompletedGameStatistics game, Map<String, String> displayNames) {
        var names = new java.util.ArrayList<>(game.aiOpponentNames());
        game.opponentIds().stream().map(id -> displayNames.getOrDefault(id, id)).forEach(names::add);
        if (names.isEmpty()) {
            return I18n.t(UiTexts.STATISTICS_NO_OPPONENTS);
        }
        return names.stream().sorted().reduce((left, right) -> left + ", " + right).orElseThrow();
    }
}
