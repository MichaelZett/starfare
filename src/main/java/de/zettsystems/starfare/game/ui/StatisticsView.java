package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
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

/** Personal overview of completed games, retained independently from archived sessions. */
@Route("statistics")
@PermitAll
@CssImport("./styles/starfare.css")
public class StatisticsView extends VerticalLayout {
    private final GameStatisticsService statistics;
    private final Span totals = new Span();
    private final Div summaryCards = new Div();
    private final Grid<CompletedGameStatistics> completedGames = new Grid<>(CompletedGameStatistics.class, false);
    private final Grid<OpponentStatistics> opponents = new Grid<>(OpponentStatistics.class, false);
    private final Paragraph empty = new Paragraph(I18n.t(UiTexts.STATISTICS_EMPTY));

    public StatisticsView(GameStatisticsService statistics, PlayerDirectory players) {
        this.statistics = statistics;
        setSizeFull();
        addClassName("statistics-view");
        summaryCards.addClassName("statistics-summary-cards");
        completedGames.addColumn(CompletedGameStatistics::gameName).setHeader(I18n.t(UiTexts.STATISTICS_GAME));
        completedGames.addColumn(row -> outcomeText(row.outcome())).setHeader(I18n.t(UiTexts.STATISTICS_RESULT));
        completedGames.addColumn(row -> finishedAtText(row)).setHeader(I18n.t(UiTexts.STATISTICS_FINISHED));
        completedGames.addColumn(row -> opponentsText(row, players)).setHeader(I18n.t(UiTexts.STATISTICS_OPPONENT));
        completedGames.setWidthFull();
        completedGames.getColumns().forEach(column -> column.setAutoWidth(true));
        opponents.addColumn(row -> players.displayName(row.opponentId())).setHeader(I18n.t(UiTexts.STATISTICS_OPPONENT));
        opponents.addColumn(OpponentStatistics::games).setHeader(I18n.t(UiTexts.STATISTICS_GAMES));
        opponents.addColumn(OpponentStatistics::wins).setHeader(I18n.t(UiTexts.STATISTICS_WINS));
        opponents.addColumn(OpponentStatistics::losses).setHeader(I18n.t(UiTexts.STATISTICS_LOSSES));
        opponents.setWidthFull();
        opponents.getColumns().forEach(column -> column.setAutoWidth(true));
        add(new H1(I18n.t(UiTexts.STATISTICS_TITLE)), summaryCards, totals,
                new HorizontalLayout(new Button(I18n.t(UiTexts.MAP_ACTION_LOBBY),
                        _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)))),
                new H2(I18n.t(UiTexts.STATISTICS_HISTORY)), completedGames, opponents, empty);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        refresh();
    }

    private void refresh() {
        PlayerStatistics view = statistics.statisticsFor(UserContext.currentPlayerId().orElse(""));
        totals.setText(I18n.t(UiTexts.STATISTICS_TOTALS, view.games(), view.wins(), view.losses()));
        summaryCards.removeAll();
        summaryCards.add(statCard("◷", UiTexts.STATISTICS_GAMES, view.games()),
                statCard("🏆", UiTexts.STATISTICS_WINS, view.wins()),
                statCard("✕", UiTexts.STATISTICS_LOSSES, view.losses()),
                balanceCard(view));
        completedGames.setItems(view.completedGames());
        opponents.setItems(view.opponents());
        empty.setVisible(view.games() == 0);
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
        Span label = new Span(I18n.t(UiTexts.STATISTICS_BALANCE, winPercent));
        Div track = new Div();
        track.addClassName("statistics-balance-track");
        Div win = new Div();
        win.addClassName("statistics-balance-win");
        win.getStyle().set("width", winPercent + "%");
        track.add(win);
        Div card = new Div(label, track);
        card.addClassName("statistics-summary-card");
        return card;
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

    private String opponentsText(CompletedGameStatistics game, PlayerDirectory players) {
        var names = new java.util.ArrayList<>(game.aiOpponentNames());
        game.opponentIds().stream().map(players::displayName).forEach(names::add);
        if (names.isEmpty()) {
            return I18n.t(UiTexts.STATISTICS_NO_OPPONENTS);
        }
        return names.stream().sorted().reduce((left, right) -> left + ", " + right).orElseThrow();
    }
}
