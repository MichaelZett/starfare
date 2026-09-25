package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.social.application.*;
import de.zettsystems.starfare.social.ui.*;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Route("")
@CssImport("./styles/starfare.css")
@PermitAll
public class LobbyView extends VerticalLayout {
    /** Laufende Partien zuerst, darin gestartete und weiter fortgeschrittene oben, dann alphabetisch. */
    private static final Comparator<LobbyGameRow> ROW_ORDER =
            Comparator.comparing(LobbyGameRow::finished)
                    .thenComparing(Comparator.comparing(LobbyGameRow::started).reversed())
                    .thenComparing(Comparator.comparingInt(LobbyGameRow::turn).reversed())
                    .thenComparing(LobbyGameRow::name);

    private final GameService game;
    private final Broadcaster broadcaster;
    private final InvitationService invitationService;
    private final PresenceTracker presence;
    private final VisibilityFilter visibilityFilter;
    private final PlayerDirectory players;
    private final SocialBroadcaster socialBroadcaster;
    private final Grid<LobbyGameRow> grid = new Grid<>(LobbyGameRow.class, false);
    private final TextField search = new TextField();
    private final ComboBox<LobbyFilter> filter = new ComboBox<>();
    private final Div emptyState = new Div();
    private final OnlineUsersPanel onlineUsersPanel;
    private final FriendRequestsPanel friendRequestsPanel;
    private final InvitationsPanel invitationsPanel;
    private final ChatDrawer chatDrawer;
    private @Nullable Button newGameButton;
    private @Nullable Subscription broadcasterSubscription;
    private @Nullable Subscription socialSubscription;

    @Autowired
    public LobbyView(GameService game, Broadcaster broadcaster, PresenceTracker presence,
                     SocialBroadcaster socialBroadcaster, VisibilityFilter visibilityFilter,
                     FriendshipService friendshipService, UserPreferencesService preferencesService, PlayerDirectory players,
                     MessageService messageService, InvitationService invitationService) {
        this.game = game;
        this.broadcaster = broadcaster;
        this.invitationService = invitationService;
        this.presence = presence;
        this.visibilityFilter = visibilityFilter;
        this.players = players;
        this.socialBroadcaster = socialBroadcaster;
        this.chatDrawer = new ChatDrawer(messageService, socialBroadcaster, players);
        this.onlineUsersPanel = new OnlineUsersPanel(presence, socialBroadcaster, visibilityFilter,
                friendshipService, players, chatDrawer::openChatWith);
        this.friendRequestsPanel = new FriendRequestsPanel(friendshipService, socialBroadcaster, players);
        this.invitationsPanel = new InvitationsPanel(game, invitationService, socialBroadcaster, players);
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        addClassName("lobby-root");

        Div toolbar = new Div();
        toolbar.addClassName("lobby-toolbar");

        Div toolbarText = new Div();
        toolbarText.addClassName("lobby-toolbar-text");
        H1 appHeader = new H1(I18n.t(UiTexts.LOBBY_HEADER_TITLE));
        Span subtitle = new Span(I18n.t(UiTexts.LOBBY_SUBTITLE));
        subtitle.addClassName("lobby-subtitle");
        toolbarText.add(appHeader, subtitle);

        Div toolbarAction = new Div();
        toolbarAction.addClassName("lobby-toolbar-action");
        Button newGameTopButton = new Button(I18n.t(UiTexts.LOBBY_NEW_GAME), _ -> openCreateGameWizard());
        newGameTopButton.addThemeVariants(ButtonVariant.PRIMARY);
        VisibilityMenu visibilityMenu = new VisibilityMenu(preferencesService, onlineUsersPanel::refresh);
        toolbarAction.add(new LanguageSwitcher(), visibilityMenu, new Button(I18n.t(UiTexts.ARCHIVE_TITLE),
                _ -> getUI().ifPresent(ui -> ui.navigate(ArchiveView.class))), new Button(I18n.t(UiTexts.STATISTICS_TITLE),
                _ -> getUI().ifPresent(ui -> ui.navigate(StatisticsView.class))), newGameTopButton);

        toolbar.add(toolbarText, toolbarAction);

        search.setPlaceholder(I18n.t(UiTexts.LOBBY_SEARCH));
        search.setClearButtonVisible(true);
        search.addValueChangeListener(_ -> refresh());
        filter.setLabel(I18n.t(UiTexts.LOBBY_FILTER));
        filter.setItems(LobbyFilter.values());
        filter.setItemLabelGenerator(item -> I18n.t(item.textKey()));
        filter.setValue(LobbyFilter.ALL);
        filter.addValueChangeListener(_ -> refresh());

        Div card = new Div();
        card.addClassName("lobby-card");

        configureGrid();
        HorizontalLayout lobbyFilters = new HorizontalLayout(search, filter);
        lobbyFilters.addClassName("lobby-filters");
        card.add(lobbyFilters, grid);

        buildEmptyState();
        card.add(emptyState);

        // Div statt VerticalLayout: das Layout setzt inline width:100% und würde
        // die feste Sidebar-Breite aus dem Stylesheet überstimmen.
        Div sidebar = new Div(friendRequestsPanel, invitationsPanel, onlineUsersPanel, chatDrawer);
        sidebar.addClassName("lobby-sidebar");

        HorizontalLayout body = new HorizontalLayout();
        body.addClassName("lobby-body");
        body.setWidthFull();
        body.setPadding(false);
        body.setSpacing(true);
        body.setFlexGrow(1, card);
        body.setFlexGrow(0, sidebar);
        body.add(card, sidebar);

        add(toolbar, body);
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        broadcasterSubscription = broadcaster.subscribeAll(_ ->
                ui.access(this::refresh));
        socialSubscription = socialBroadcaster.subscribe(_ -> ui.access(this::refresh));
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (socialSubscription != null) { socialSubscription.remove(); socialSubscription = null; }
        if (broadcasterSubscription != null) {
            broadcasterSubscription.remove();
            broadcasterSubscription = null;
        }
    }

    private void configureGrid() {
        grid.addClassName("lobby-grid");
        grid.setWidthFull();
        grid.addComponentColumn(this::gameCard).setHeader("").setFlexGrow(1);
        grid.setAllRowsVisible(true);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
    }

    private Component gameCard(LobbyGameRow row) {
        Div card = new Div();
        card.addClassName("lobby-game-card");
        Div heading = new Div();
        heading.addClassName("lobby-game-card-heading");
        heading.add(nameWithHost(row), statusBadge(row));
        Span turn = new Span(I18n.t(UiTexts.LOBBY_TURN_LABEL, row.turn()));
        turn.addClassName("lobby-game-card-turn");
        Span participants = new Span(row.players());
        participants.addClassName("lobby-game-card-participants");
        Span seats = new Span(I18n.t(UiTexts.LOBBY_CARD_OPEN_SEATS, row.openHumanSeats()));
        seats.addClassName("lobby-game-card-seats");
        Div overview = new Div(turn, participants, seats, participantColors(row));
        overview.addClassName("lobby-game-card-overview");
        Button primary = primaryAction(row);
        primary.addClassName("lobby-game-card-primary");
        card.add(heading, overview, primary, secondaryActions(row));
        return card;
    }

    private Button primaryAction(LobbyGameRow row) {
        if (row.started() && row.joinedByCurrentUser()) {
            return buildPlayButton(row);
        }
        if (!row.started() && row.canStart()) {
            return buildStartButton(row);
        }
        if (row.canJoin()) {
            return buildJoinButton(row);
        }
        if (row.observersAllowed() && !row.joinedByCurrentUser()) {
            return buildObserveButton(row);
        }
        Button unavailable = new Button(I18n.t(statusKey(row)));
        unavailable.setEnabled(false);
        return unavailable;
    }

    private Details secondaryActions(LobbyGameRow row) {
        VerticalLayout actions = new VerticalLayout();
        actions.setPadding(false);
        actions.setSpacing(true);
        Button manage = buildManageButton(row);
        if (manage.isVisible()) {
            actions.add(manage);
        }
        Button abort = buildAbortButton(row);
        if (abort.isEnabled()) {
            actions.add(abort);
        }
        if (actions.getComponentCount() == 0) {
            return new Details();
        }
        Details details = new Details(I18n.t(UiTexts.LOBBY_CARD_MORE_ACTIONS), actions);
        details.addClassName("lobby-game-card-more");
        return details;
    }

    private void buildEmptyState() {
        emptyState.addClassName("lobby-empty");
        Div textBlock = new Div();
        textBlock.addClassName("lobby-empty-text");
        H2 emptyTitle = new H2(I18n.t(UiTexts.LOBBY_EMPTY_TITLE));
        Span emptyBody = new Span(I18n.t(UiTexts.LOBBY_EMPTY_BODY));
        emptyBody.addClassName("lobby-empty-body");
        textBlock.add(emptyTitle, emptyBody);

        newGameButton = new Button(I18n.t(UiTexts.LOBBY_NEW_GAME));
        newGameButton.addThemeVariants(ButtonVariant.PRIMARY);
        newGameButton.addClickListener(_ -> openCreateGameWizard());

        emptyState.add(textBlock, newGameButton);
    }

    private void refresh() {
        String playerId = UserContext.currentPlayerId().orElse("");
        List<LobbyGameRow> rows = game.visibleGamesFor(playerId, GameListScope.LOBBY).stream()
                .map(summary -> LobbyGameRow.from(summary, playerId))
                .filter(this::matchesFilter)
                .sorted(ROW_ORDER)
                .toList();
        boolean any = !rows.isEmpty();
        grid.setVisible(any);
        emptyState.setVisible(!any);
        if (any) {
            grid.setItems(rows);
        }
    }

    private boolean matchesFilter(LobbyGameRow row) {
        String term = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (!term.isEmpty() && !(row.name() + " " + row.players()).toLowerCase(Locale.ROOT).contains(term)) {
            return false;
        }
        LobbyFilter selected = filter.getValue();
        return selected == null || selected.matches(row);
    }

    /** Auswahl im Lobby-Filter. Identitaet haengt am Enum, nicht am uebersetzten Label. */
    private enum LobbyFilter {
        ALL(UiTexts.LOBBY_FILTER_ALL),
        OWN(UiTexts.LOBBY_FILTER_OWN),
        OPEN(UiTexts.LOBBY_FILTER_OPEN),
        RUNNING(UiTexts.LOBBY_FILTER_RUNNING);

        private final String textKey;

        LobbyFilter(String textKey) {
            this.textKey = textKey;
        }

        String textKey() {
            return textKey;
        }

        boolean matches(LobbyGameRow row) {
            return switch (this) {
                case ALL -> true;
                case OWN -> row.knownToCurrentUser() || row.isHostedByCurrentUser();
                case OPEN -> !row.started() && row.hasOpenHumanSeat();
                case RUNNING -> row.started() && !row.finished();

            };
        }
    }

    private Component nameWithHost(LobbyGameRow row) {
        Div container = new Div();
        container.addClassName("lobby-game-name");
        Span name = new Span(row.name());
        name.addClassName("lobby-game-name-text");
        container.add(name, new Span(I18n.t(row.visibilityKey())));
        if (row.isHostedByCurrentUser()) {
            Span badge = new Span(I18n.t(UiTexts.LOBBY_HOST_BADGE));
            badge.addClassName("lobby-host-badge");
            container.add(badge);
        }
        return container;
    }

    private static Div participantColors(LobbyGameRow row) {
        Div colors = new Div();
        colors.addClassName("lobby-game-card-colors");
        row.summary().players().forEach(player -> {
            Span color = new Span(player.label());
            color.addClassName("lobby-game-card-color");
            color.getStyle().set("--seat-color", player.colorHex());
            colors.add(color);
        });
        return colors;
    }

    private Span statusBadge(LobbyGameRow row) {
        Span badge = new Span(I18n.t(statusKey(row)));
        badge.addClassName("lobby-status");
        badge.addClassName(statusStyleClass(row));
        return badge;
    }

    private static String statusKey(LobbyGameRow row) {
        if (row.finished()) {
            return UiTexts.STATUS_FINISHED;
        }
        if (row.started()) {
            return UiTexts.STATUS_RUNNING;
        }
        return row.canStart() ? UiTexts.STATUS_WAITING : UiTexts.STATUS_CREATED;
    }

    private static String statusStyleClass(LobbyGameRow row) {
        if (row.finished()) {
            return "lobby-status-finished";
        }
        if (row.started()) {
            return "lobby-status-live";
        }
        return "lobby-status-idle";
    }

    private Button buildPlayButton(LobbyGameRow row) {
        Button play = new Button(I18n.t(UiTexts.LOBBY_ACTION_PLAY), _ -> navigateToMap(row.gameId()));
        play.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        play.setEnabled(row.started() && row.joinedByCurrentUser());
        return play;
    }

    private Button buildJoinButton(LobbyGameRow row) {
        Button join = new Button(I18n.t(UiTexts.LOBBY_ACTION_JOIN), _ -> {
            String playerId = UserContext.currentPlayerId().orElse(null);
            if (playerId == null) {
                Notification.show(I18n.t(UiTexts.LOBBY_JOIN_FAILED));
                return;
            }
            JoinGameDialog.open(game, row.summary(), playerId, players.displayName(playerId), this::refresh);
        });
        join.addThemeVariants(ButtonVariant.SMALL);
        join.setEnabled(row.canJoin());
        return join;
    }

    private Button buildStartButton(LobbyGameRow row) {
        Button start = new Button(I18n.t(UiTexts.LOBBY_ACTION_START), _ -> {
            if (!game.startGame(row.gameId())) {
                Notification.show(I18n.t(UiTexts.LOBBY_START_FAILED));
                refresh();
                return;
            }
            // Wer die Partie startet und selbst mitspielt, will sie auch sehen.
            // Ein blosser Host ohne Sitz bleibt in der Lobby.
            if (row.joinedByCurrentUser()) {
                navigateToMap(row.gameId());
                return;
            }
            refresh();
        });
        start.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        start.setEnabled(!row.started() && row.canStart());
        return start;
    }

    private Button buildObserveButton(LobbyGameRow row) {
        Button observe = new Button(I18n.t(UiTexts.LOBBY_ACTION_OBSERVE), _ -> {
            String playerId = UserContext.currentPlayerId().orElse(null);
            if (!game.observeGame(row.gameId(), playerId)) {
                Notification.show(I18n.t(UiTexts.LOBBY_OBSERVE_FAILED));
                return;
            }
            Notification.show(I18n.t(UiTexts.LOBBY_OBSERVING));
            navigateToMap(row.gameId());
        });
        observe.addThemeVariants(ButtonVariant.SMALL);
        observe.setVisible(row.observersAllowed());
        observe.setEnabled(row.observersAllowed() && !row.joinedByCurrentUser());
        return observe;
    }

    private Button buildAbortButton(LobbyGameRow row) {
        Button abort = new Button(I18n.t(UiTexts.LOBBY_ACTION_ABORT), _ -> {
            String playerId = UserContext.currentPlayerId().orElse(null);
            if (!game.abortGame(row.gameId(), playerId)) {
                Notification.show(I18n.t(UiTexts.LOBBY_ABORT_DENIED));
                return;
            }
            refresh();
        });
        abort.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        abort.setEnabled(row.isHostedByCurrentUser() || !row.hasHost());
        return abort;
    }

    private Button buildManageButton(LobbyGameRow row) {
        Button manage = new Button(I18n.t(UiTexts.LOBBY_ACTION_MANAGE), _ -> openManageDialog(row));
        manage.addThemeVariants(ButtonVariant.SMALL);
        manage.setVisible(row.isHostedByCurrentUser());
        manage.setEnabled(row.isHostedByCurrentUser() && !row.finished());
        return manage;
    }

    private void openCreateGameWizard() {
        CreateGameWizardDialog.open(game, players, this::refresh);
    }

    private void openManageDialog(LobbyGameRow row) {
        String playerId = UserContext.currentPlayerId().orElse(null);
        if (playerId == null) {
            return;
        }
        ManageGameDialog dialog = new ManageGameDialog(row.gameId(), playerId, game, invitationService,
                presence, visibilityFilter, broadcaster, socialBroadcaster, players);
        dialog.open();
    }

    private void navigateToMap(GameId gameId) {
        getUI().ifPresent(ui -> ui.navigate(MainView.class,
                new RouteParameters("gameId", gameId.value())));
    }

    private record LobbyGameRow(GameSummary summary, GameId gameId, String name, String players, int turn, boolean finished,
                                boolean started, boolean canStart, boolean joinedByCurrentUser,
                                boolean knownToCurrentUser, boolean hasOpenHumanSeat,
                                boolean observersAllowed, boolean reentryAllowed,
                                boolean isHostedByCurrentUser, boolean hasHost, boolean canJoin, String visibilityKey) {
        static LobbyGameRow from(GameSummary summary, @Nullable String playerId) {
            String players = summary.players().stream()
                    .map(LobbyGameRow::playerLabel)
                    .collect(Collectors.joining(", "));
            boolean canStart = !summary.started() && summary.allHumansJoined();
            Integer seat = playerId == null ? null : summary.seatByPlayer().get(playerId);
            boolean joinedByCurrent = seat != null && summary.joinedHumanSeats().contains(seat);
            boolean known = seat != null;
            String name = summary.name();
            String displayName = name.isBlank() ? GameConfig.DEFAULT_GAME_NAME : name;
            String host = summary.hostPlayerId();
            boolean hasHost = host != null && !host.isBlank();
            boolean hostedByCurrent = hasHost && host != null && host.equals(playerId);
            return new LobbyGameRow(summary, summary.gameId(), displayName, players, summary.turn(), summary.gameOver(),
                    summary.started(), canStart, joinedByCurrent, known, summary.hasOpenHumanSeat(),
                    summary.observersAllowed() && (summary.visibility() == GameVisibility.PUBLIC
                            || hostedByCurrent || known), summary.reentryAllowed(), hostedByCurrent, hasHost,
                    summary.canJoin(playerId == null ? "" : playerId),
                    summary.visibility() == GameVisibility.PRIVATE
                            ? UiTexts.GAME_PRIVATE : UiTexts.GAME_PUBLIC);
        }

        int openHumanSeats() {
            return (int) summary.players().stream().filter(player -> !player.ai())
                    .filter(player -> !summary.joinedHumanSeats().contains(player.id()))
                    .filter(player -> !summary.invitedSeats().containsValue(player.id()))
                    .count();
        }

        private static String playerLabel(Player player) {
            return player.label() + (player.ai() ? " (AI)" : "");
        }
    }
}
