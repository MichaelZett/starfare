package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.social.application.*;
import de.zettsystems.starfare.social.ui.*;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Route("")
@CssImport("./styles/starfare.css")
@PermitAll
public class LobbyView extends VerticalLayout {
    private final GameService game;
    private final Broadcaster broadcaster;
    private final InvitationService invitationService;
    private final PresenceTracker presence;
    private final VisibilityFilter visibilityFilter;
    private final PlayerDirectory players;
    private final SocialBroadcaster socialBroadcaster;
    private final Grid<LobbyGameRow> grid = new Grid<>(LobbyGameRow.class, false);
    private final Div emptyState = new Div();
    private final OnlineUsersPanel onlineUsersPanel;
    private final FriendRequestsPanel friendRequestsPanel;
    private final InvitationsPanel invitationsPanel;
    private final ChatDrawer chatDrawer;
    private @Nullable Button newGameButton;
    private @Nullable Subscription broadcasterSubscription;

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
        toolbarAction.add(new LanguageSwitcher(), visibilityMenu, newGameTopButton);

        toolbar.add(toolbarText, toolbarAction);

        Div card = new Div();
        card.addClassName("lobby-card");

        configureGrid();
        card.add(grid);

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
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterSubscription != null) {
            broadcasterSubscription.remove();
            broadcasterSubscription = null;
        }
    }

    private void configureGrid() {
        grid.addClassName("lobby-grid");
        grid.setWidthFull();
        grid.addComponentColumn(this::nameWithHost)
                .setHeader(I18n.t(UiTexts.LOBBY_COLUMN_GAME))
                .setAutoWidth(true).setFlexGrow(2);
        grid.addColumn(row -> I18n.t(UiTexts.LOBBY_TURN_LABEL, row.turn()))
                .setHeader(I18n.t(UiTexts.LOBBY_COLUMN_TURN)).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(LobbyGameRow::players)
                .setHeader(I18n.t(UiTexts.LOBBY_COLUMN_PLAYERS))
                .setAutoWidth(true).setFlexGrow(3);
        grid.addComponentColumn(this::statusBadge)
                .setHeader(I18n.t(UiTexts.LOBBY_COLUMN_STATUS)).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::actionButtons)
                .setHeader(I18n.t(UiTexts.LOBBY_COLUMN_ACTIONS)).setAutoWidth(true).setFlexGrow(0);
        grid.setAllRowsVisible(true);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
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
        String playerId = UserContext.currentPlayerId().orElse(null);
        List<LobbyGameRow> rows = game.listGames().stream()
                .map(id -> LobbyGameRow.from(game.summaryOf(id), playerId))
                .toList();
        boolean any = !rows.isEmpty();
        grid.setVisible(any);
        emptyState.setVisible(!any);
        if (any) {
            grid.setItems(rows);
        }
    }

    private Component nameWithHost(LobbyGameRow row) {
        Div container = new Div();
        container.addClassName("lobby-game-name");
        container.add(new Span(row.name()));
        if (row.isHostedByCurrentUser()) {
            Span badge = new Span(I18n.t(UiTexts.LOBBY_HOST_BADGE));
            badge.addClassName("lobby-host-badge");
            container.add(badge);
        }
        return container;
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

    private Component actionButtons(LobbyGameRow row) {
        VerticalLayout actions = new VerticalLayout(
                buildJoinButton(row),
                buildStartButton(row),
                buildPlayButton(row),
                buildObserveButton(row),
                buildManageButton(row),
                buildAbortButton(row));
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.addClassName("lobby-actions");
        return actions;
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
            if (game.joinGame(row.gameId(), playerId).isEmpty()) {
                Notification.show(I18n.t(UiTexts.LOBBY_JOIN_FAILED));
            } else {
                Notification.show(I18n.t(UiTexts.LOBBY_JOINED));
            }
            refresh();
        });
        join.addThemeVariants(ButtonVariant.SMALL);
        boolean canJoinFresh = !row.started() && !row.joinedByCurrentUser() && row.hasOpenHumanSeat();
        boolean canRejoin = row.started() && !row.finished() && row.reentryAllowed() && !row.joinedByCurrentUser()
                && row.knownToCurrentUser();
        join.setEnabled(canJoinFresh || canRejoin);
        return join;
    }

    private Button buildStartButton(LobbyGameRow row) {
        Button start = new Button(I18n.t(UiTexts.LOBBY_ACTION_START), _ -> {
            boolean started = game.startGame(row.gameId());
            if (!started) {
                Notification.show(I18n.t(UiTexts.LOBBY_START_FAILED));
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
        CreateGameWizardDialog.open(game, this::refresh);
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

    private record LobbyGameRow(GameId gameId, String name, String players, int turn, boolean finished,
                                boolean started, boolean canStart, boolean joinedByCurrentUser,
                                boolean knownToCurrentUser, boolean hasOpenHumanSeat,
                                boolean observersAllowed, boolean reentryAllowed,
                                boolean isHostedByCurrentUser, boolean hasHost) {
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
            return new LobbyGameRow(summary.gameId(), displayName, players, summary.turn(), summary.gameOver(),
                    summary.started(), canStart, joinedByCurrent, known, summary.hasOpenHumanSeat(),
                    summary.observersAllowed(), summary.reentryAllowed(), hostedByCurrent, hasHost);
        }

        private static String playerLabel(Player player) {
            return player.name() + (player.ai() ? " (AI)" : "");
        }
    }
}
