package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Route("archive")
@PermitAll
@CssImport("./styles/starfare.css")
public class ArchiveView extends VerticalLayout {
    private final GameService games;
    private final Broadcaster broadcaster;
    private final Grid<GameSummary> grid = new Grid<>(GameSummary.class, false);
    private final TextField search = new TextField();
    private final Checkbox own = new Checkbox();
    private final ComboBox<ArchiveResult> resultFilter = new ComboBox<>();
    private final Span empty = new Span(I18n.t(UiTexts.ARCHIVE_EMPTY));
    private @Nullable Subscription subscription;

    public ArchiveView(GameService games, Broadcaster broadcaster) {
        this.games = games;
        this.broadcaster = broadcaster;
        setSizeFull();
        search.setPlaceholder(I18n.t(UiTexts.LOBBY_SEARCH));
        search.setClearButtonVisible(true);
        search.addValueChangeListener(_ -> refresh());
        own.setLabel(I18n.t(UiTexts.LOBBY_FILTER_OWN));
        own.addValueChangeListener(_ -> refresh());
        resultFilter.setLabel(I18n.t(UiTexts.ARCHIVE_RESULT_FILTER));
        resultFilter.setItems(ArchiveResult.values());
        resultFilter.setItemLabelGenerator(result -> I18n.t(result.textKey()));
        resultFilter.setValue(ArchiveResult.ALL);
        resultFilter.addValueChangeListener(_ -> refresh());
        add(new H1(I18n.t(UiTexts.ARCHIVE_TITLE)), new HorizontalLayout(search, own, resultFilter,
                new Button(I18n.t(UiTexts.MAP_ACTION_LOBBY),
                        _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)))));
        grid.addComponentColumn(this::archiveCard).setHeader("").setFlexGrow(1);
        grid.setWidthFull();
        add(grid, empty);
    }

    private Div archiveCard(GameSummary summary) {
        Div card = new Div();
        card.addClassName("archive-game-card");
        Span name = new Span(summary.name());
        name.addClassName("archive-game-card-name");
        Span winner = new Span(I18n.t(UiTexts.ARCHIVE_WINNER) + ": " + winnerName(summary));
        winner.addClassName("archive-game-card-winner");
        Player winnerPlayer = winnerPlayer(summary);
        if (winnerPlayer != null) {
            winner.getStyle().set(CssProperties.BORDER_COLOR, winnerPlayer.colorHex());
        }
        Span personalResult = personalResult(summary);
        personalResult.addClassName("archive-game-card-result");
        Span meta = new Span(I18n.t(UiTexts.LOBBY_TURN_LABEL, summary.turn()) + " · " + finishedAt(summary));
        meta.addClassName("archive-game-card-meta");
        Span participants = new Span(participants(summary));
        participants.addClassName("archive-game-card-participants");
        Div details = new Div(personalResult, winner, meta, participants);
        details.addClassName("archive-game-card-details");
        card.add(name, details, viewButton(summary));
        return card;
    }

    private Button viewButton(GameSummary summary) {
        Button button = new Button(I18n.t(UiTexts.ARCHIVE_VIEW), _ -> getUI().ifPresent(ui ->
                ui.navigate(MainView.class, new RouteParameters("gameId", summary.gameId().value()))));
        button.setEnabled(summary.belongsTo(account()) || summary.observersAllowed());
        return button;
    }

    private String participants(GameSummary summary) {
        return summary.players().stream().map(Player::label)
                .collect(Collectors.joining(", "));
    }

    private String winnerName(GameSummary summary) {
        Player winner = winnerPlayer(summary);
        return winner == null ? I18n.t(UiTexts.ARCHIVE_NO_WINNER) : winner.label();
    }

    private @Nullable Player winnerPlayer(GameSummary summary) {
        Integer winner = summary.outcome().winnerId();
        if (winner == null) { return null; }
        return summary.players().stream().filter(player -> player.id() == winner)
                .findFirst().orElse(null);
    }

    private Span personalResult(GameSummary summary) {
        Integer seat = summary.seatByPlayer().get(account());
        if (seat == null) {
            return new Span(I18n.t(UiTexts.ARCHIVE_NOT_A_PLAYER));
        }
        Integer winner = summary.outcome().winnerId();
        String key;
        if (winner == null) {
            key = UiTexts.ARCHIVE_RESULT_NO_WINNER;
        } else if (winner.equals(seat)) {
            key = UiTexts.ARCHIVE_RESULT_VICTORY;
        } else {
            key = UiTexts.ARCHIVE_RESULT_DEFEAT;
        }
        Span result = new Span(I18n.t(key));
        result.addClassName(resultStyle(key));
        return result;
    }

    private static String resultStyle(String key) {
        if (key.equals(UiTexts.ARCHIVE_RESULT_VICTORY)) { return "archive-result-victory"; }
        if (key.equals(UiTexts.ARCHIVE_RESULT_DEFEAT)) { return "archive-result-defeat"; }
        return "archive-result-no-winner";
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
                .filter(s -> resultMatches(s, resultFilter.getValue()))
                .filter(s -> (s.name() + " " + participants(s)).toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing((GameSummary s) -> s.outcome().finishedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(GameSummary::name).thenComparing(s -> s.gameId().value())).toList();
        grid.setItems(summaries);
        empty.setVisible(summaries.isEmpty());
    }

    private boolean resultMatches(GameSummary summary, @Nullable ArchiveResult result) {
        if (result == null || result == ArchiveResult.ALL) {
            return true;
        }
        Integer seat = summary.seatByPlayer().get(account());
        return switch (result) {
            case ALL -> true;
            case VICTORY -> seat != null && Objects.equals(summary.outcome().winnerId(), seat);
            case DEFEAT -> seat != null && summary.outcome().winnerId() != null
                    && !Objects.equals(summary.outcome().winnerId(), seat);
            case NO_WINNER -> seat != null && summary.outcome().winnerId() == null;
        };
    }

    private enum ArchiveResult {
        ALL(UiTexts.ARCHIVE_RESULT_ALL),
        VICTORY(UiTexts.ARCHIVE_RESULT_VICTORY_FILTER),
        DEFEAT(UiTexts.ARCHIVE_RESULT_DEFEAT_FILTER),
        NO_WINNER(UiTexts.ARCHIVE_RESULT_NO_WINNER_FILTER);

        private final String textKey;

        ArchiveResult(String textKey) {
            this.textKey = textKey;
        }

        String textKey() {
            return textKey;
        }
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
