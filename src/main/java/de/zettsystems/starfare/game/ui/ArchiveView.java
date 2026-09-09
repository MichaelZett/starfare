package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

@Route("archive")
@PermitAll
@CssImport("./styles/starfare.css")
public class ArchiveView extends VerticalLayout {
    private final GameService games;
    private final Broadcaster broadcaster;
    private final PlayerDirectory players;
    private final Grid<GameSummary> grid = new Grid<>(GameSummary.class, false);
    private final TextField search = new TextField();
    private final Checkbox own = new Checkbox();
    private final Span empty = new Span(I18n.t(UiTexts.ARCHIVE_EMPTY));
    private @Nullable Subscription subscription;

    public ArchiveView(GameService games, Broadcaster broadcaster, PlayerDirectory players) {
        this.games = games;
        this.broadcaster = broadcaster;
        this.players = players;
        setSizeFull();
        search.setPlaceholder(I18n.t(UiTexts.LOBBY_SEARCH));
        search.setClearButtonVisible(true);
        search.addValueChangeListener(_ -> refresh());
        own.setLabel(I18n.t(UiTexts.LOBBY_FILTER_OWN));
        own.addValueChangeListener(_ -> refresh());
        add(new H1(I18n.t(UiTexts.ARCHIVE_TITLE)), new HorizontalLayout(search, own,
                new Button(I18n.t(UiTexts.MAP_ACTION_LOBBY),
                        _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)))));
        grid.addColumn(GameSummary::name).setHeader(I18n.t(UiTexts.LOBBY_COLUMN_GAME));
        grid.addColumn(s -> I18n.t(s.visibility() == GameVisibility.PRIVATE
                ? UiTexts.GAME_PRIVATE : UiTexts.GAME_PUBLIC));
        grid.addColumn(this::participants).setHeader(I18n.t(UiTexts.LOBBY_COLUMN_PLAYERS));
        grid.addColumn(this::winner).setHeader(I18n.t(UiTexts.ARCHIVE_WINNER));
        grid.addColumn(GameSummary::turn).setHeader(I18n.t(UiTexts.LOBBY_COLUMN_TURN));
        grid.addColumn(this::finishedAt).setHeader(I18n.t(UiTexts.ARCHIVE_FINISHED));
        grid.addComponentColumn(this::viewButton);
        grid.setWidthFull();
        grid.getColumns().forEach(column -> column.setAutoWidth(true));
        add(grid, empty);
    }

    private Button viewButton(GameSummary summary) {
        Button button = new Button(I18n.t(UiTexts.ARCHIVE_VIEW), _ -> getUI().ifPresent(ui ->
                ui.navigate(MainView.class, new RouteParameters("gameId", summary.gameId().value()))));
        button.setEnabled(summary.belongsTo(account()) || summary.observersAllowed());
        return button;
    }

    private String participants(GameSummary summary) {
        return summary.players().stream().map(player -> summary.seatByPlayer().entrySet().stream()
                .filter(entry -> entry.getValue() == player.id())
                .map(entry -> players.displayName(entry.getKey())).findFirst().orElse(player.name()))
                .collect(Collectors.joining(", "));
    }

    private String winner(GameSummary summary) {
        Integer winner = summary.outcome().winnerId();
        if (winner == null) { return I18n.t(UiTexts.ARCHIVE_NO_WINNER); }
        return summary.seatByPlayer().entrySet().stream().filter(e -> e.getValue().equals(winner))
                .map(e -> players.displayName(e.getKey())).findFirst().orElseGet(() ->
                        summary.players().stream().filter(p -> p.id() == winner).map(Player::name)
                                .findFirst().orElse(I18n.t(UiTexts.ARCHIVE_NO_WINNER)));
    }

    private String finishedAt(GameSummary summary) {
        var finished = summary.outcome().finishedAt();
        if (finished == null) { return I18n.t(UiTexts.ARCHIVE_UNKNOWN_TIME); }
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(getLocale())
                .withZone(ZoneId.systemDefault()).format(finished);
    }

    private String account() { return UserContext.currentPlayerId().orElse(""); }

    private void refresh() {
        String account = account();
        String term = search.getValue().strip().toLowerCase(Locale.ROOT);
        var summaries = games.visibleGamesFor(account, GameListScope.ARCHIVE).stream()
                .filter(s -> !own.getValue() || s.belongsTo(account))
                .filter(s -> (s.name() + " " + participants(s)).toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing((GameSummary s) -> s.outcome().finishedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(GameSummary::name).thenComparing(s -> s.gameId().value())).toList();
        grid.setItems(summaries);
        empty.setVisible(summaries.isEmpty());
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored") // UI update is scheduled by Vaadin.
    protected void onAttach(AttachEvent event) {
        subscription = broadcaster.subscribeAll(_ -> event.getUI().access(this::refresh));
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        if (subscription != null) { subscription.remove(); subscription = null; }
    }
}
