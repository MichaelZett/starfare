package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameStatisticsService;
import de.zettsystems.starfare.game.values.OpponentStatistics;
import de.zettsystems.starfare.game.values.PlayerStatistics;
import de.zettsystems.starfare.i18n.I18n;
import jakarta.annotation.security.PermitAll;

/** Personal overview of completed games, retained independently from archived sessions. */
@Route("statistics")
@PermitAll
@CssImport("./styles/starfare.css")
public class StatisticsView extends VerticalLayout {
    private final GameStatisticsService statistics;
    private final Span totals = new Span();
    private final Grid<OpponentStatistics> opponents = new Grid<>(OpponentStatistics.class, false);
    private final Paragraph empty = new Paragraph(I18n.t(UiTexts.STATISTICS_EMPTY));

    public StatisticsView(GameStatisticsService statistics, PlayerDirectory players) {
        this.statistics = statistics;
        setSizeFull();
        addClassName("statistics-view");
        opponents.addColumn(row -> players.displayName(row.opponentId())).setHeader(I18n.t(UiTexts.STATISTICS_OPPONENT));
        opponents.addColumn(OpponentStatistics::games).setHeader(I18n.t(UiTexts.STATISTICS_GAMES));
        opponents.addColumn(OpponentStatistics::wins).setHeader(I18n.t(UiTexts.STATISTICS_WINS));
        opponents.addColumn(OpponentStatistics::losses).setHeader(I18n.t(UiTexts.STATISTICS_LOSSES));
        opponents.setWidthFull();
        opponents.getColumns().forEach(column -> column.setAutoWidth(true));
        add(new H1(I18n.t(UiTexts.STATISTICS_TITLE)), totals,
                new HorizontalLayout(new Button(I18n.t(UiTexts.MAP_ACTION_LOBBY),
                        _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)))), opponents, empty);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        refresh();
    }

    private void refresh() {
        PlayerStatistics view = statistics.statisticsFor(UserContext.currentPlayerId().orElse(""));
        totals.setText(I18n.t(UiTexts.STATISTICS_TOTALS, view.games(), view.wins(), view.losses()));
        opponents.setItems(view.opponents());
        empty.setVisible(view.games() == 0);
    }
}
