package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameChatService;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import de.zettsystems.starfare.style.CssProperties;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Primary UI view showing map and fleet controls. Route is /map/{gameId}.
 * Fleet commands are issued directly on the map: click own system to select source,
 * click target to open the send dialog; right-click a fleet line for Wait/Disband.
 */
@Route("map/:gameId")
@CssImport("./styles/starfare.css")
@JsModule("./battle-replay.js")
@PermitAll
public class MainView extends VerticalLayout implements BeforeEnterObserver {

    private enum TargetSelection { NONE, FLEET, RELOCATION }
    private static final String GAME_ID_PARAMETER = "gameId";
    private final GameService game;
    private final GameChatService gameChat;
    private final PlayerDirectory players;
    private final Broadcaster broadcaster;
    private @Nullable Subscription broadcasterSubscription;
    private final MapCanvas mapCanvas;
    private final MapHeaderBar header;
    private final ReviewControls reviewControls;
    private final SpectatorControls spectatorControls;
    private final RoundStatusBar roundStatus = new RoundStatusBar();
    private final Div gameOverBanner = new Div();
    private final FleetAndOrdersPanel fleetsPanel;
    private final Div planningActions = new Div();

    @SuppressWarnings("NullAway.Init")
    private GameId gameId;
    private boolean reviewing;
    private @Nullable VisibleSystem selectedSystem;
    private @Nullable VisibleSystem selectedFrom;
    private TargetSelection targetSelection = TargetSelection.NONE;
    private @Nullable Integer highlightedFleetId;
    private @Nullable Integer highlightedStandingOrderId;
    private @Nullable Integer highlightedReportSystemId;
    private @Nullable ReplayEventMarker pendingReplayEvent;
    private @Nullable Integer displayedTurn;
    /** Remember the pending outcome even when the view was reloaded during the final battles. */
    private boolean witnessedRunningGame;
    private boolean outcomeAcknowledged;
    private boolean outcomeDialogOpen;
    private @Nullable Dialog outcomeDialog;
    private @Nullable GameChatDialog chatDialog;
    private boolean logisticsMode;
    private final Set<Integer> badgesShowingFleetNo = new HashSet<>();
    private final BattleAcknowledgements acknowledgements;

    @Autowired
    public MainView(GameService service, Broadcaster broadcaster, BattleAcknowledgementService battleAcknowledgements,
                    GameChatService gameChat, PlayerDirectory players,
                    de.zettsystems.starfare.social.application.UserPreferencesService preferences) {
        DisplayPreferences.load(preferences);
        this.game = service;
        this.gameChat = gameChat;
        this.players = players;
        this.broadcaster = broadcaster;
        this.acknowledgements = new BattleAcknowledgements(battleAcknowledgements);
        setWidthFull();
        setHeightFull();
        setSpacing(false);
        setPadding(false);
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        addClassName("map-root");

        fleetsPanel = new FleetAndOrdersPanel(acknowledgements, this::cancelOrder, this::editStandingOrder, this::deleteStandingOrder,
                this::refresh,
                this::onFleetRowSelected, this::onStandingOrderSelected, this::onReportSystemSelected,
                this::onResolvedBattleSelected,
                this::setGarrisonReserve,
                this::selectFleetTarget,
                this::selectRelocationTarget,
                this::cancelTargetSelectionAndRefresh);
        mapCanvas = new MapCanvas(this::onMapBackgroundClick);
        fleetsPanel.onEditOrder(order -> planningDialogs().edit(order));
        planningActions.addClassName("map-controls");
        planningActions.add(new com.vaadin.flow.component.button.Button(I18n.t(UiTexts.PLAN_UNDO), _ -> {
            if (!game.undoOrders(gameId, currentSeat(), Objects.requireNonNull(displayedTurn))) {
                Notification.show(I18n.t(UiTexts.PLAN_REJECTED));
            }
            refresh();
        }), new com.vaadin.flow.component.button.Button(I18n.t(UiTexts.PLAN_DISPATCH), _ -> planningDialogs().dispatch()),
                new com.vaadin.flow.component.button.Button(I18n.t(UiTexts.PLAN_RESERVES), _ -> planningDialogs().reserves()));
        header = new MapHeaderBar(this::onNextRound, this::toggleLogisticsMode, this::doLeave,
                () -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)), this::openGameChat, this::openRoundRules);

        gameOverBanner.setVisible(false);
        gameOverBanner.getStyle().set(CssProperties.FONT_WEIGHT, "600");

        reviewControls = new ReviewControls(() -> {
            selectedSystem = null;
            cancelTargetSelection();
            highlightedFleetId = null;
            highlightedReportSystemId = null;
            badgesShowingFleetNo.clear();
            refresh();
        }, marker -> {
            pendingReplayEvent = marker;
            highlightedReportSystemId = marker.systemId();
            fleetsPanel.showDetails();
            refresh();
        });
        spectatorControls = new SpectatorControls(() -> {
            selectedSystem = null;
            cancelTargetSelection();
            highlightedFleetId = null;
            highlightedReportSystemId = null;
            badgesShowingFleetNo.clear();
            refresh();
        });
        add(header, roundStatus, planningActions, spectatorControls, reviewControls, buildContent());
    }

    private PlanningDialogs planningDialogs() {
        return new PlanningDialogs(game, gameId, currentSeat(), game.viewFor(gameId, currentSeat()), () -> {
            refresh();
            fleetsPanel.showOrders();
        });
    }

    private SplitLayout buildContent() {
        var left = new VerticalLayout();
        left.setPadding(false);
        left.setSpacing(true);
        left.setWidthFull();
        left.setHeightFull();
        left.addClassName("map-left");
        left.add(gameOverBanner, new MapControls(mapCanvas), mapCanvas);
        left.setFlexGrow(1, mapCanvas);

        var content = new SplitLayout(left, fleetsPanel);
        content.setWidthFull();
        content.setHeightFull();
        content.setSplitterPosition(Math.clamp(DisplayPreferences.number(de.zettsystems.starfare.social.values.DisplaySetting.SIDEBAR, 70), 20, 85));
        content.addSplitterDragEndListener(_ -> DisplayPreferences.choose(
                de.zettsystems.starfare.social.values.DisplaySetting.SIDEBAR, String.valueOf(content.getSplitterPosition())));
        content.addClassName("map-content");
        return content;
    }

    private void onNextRound() {
        String playerId = UserContext.currentPlayerId().orElse(null);
        if (isObserver()) {
            if (!game.advanceForObserver(gameId, playerId)) {
                Notification.show(I18n.t(UiTexts.MAP_SUBMIT_FAILED));
                return;
            }
            refresh();
            return;
        }
        int pid = currentSeat();
        if (pid < 0) {
            Notification.show(I18n.t(UiTexts.MAP_SUBMIT_FAILED));
            return;
        }
        if (!game.submitTurn(gameId, pid)) {
            Notification.show(I18n.t(UiTexts.MAP_SUBMIT_FAILED));
            return;
        }
        refresh();
    }

    private void onMapBackgroundClick() {
        if (selectedFrom != null || highlightedFleetId != null || highlightedStandingOrderId != null) {
            cancelTargetSelection();
            highlightedFleetId = null;
            highlightedStandingOrderId = null;
            refresh();
        }
    }

    private void toggleLogisticsMode() {
        logisticsMode = !logisticsMode;
        if (logisticsMode) {
            fleetsPanel.showLogistics();
        }
        refresh();
    }

    private void onFleetRowSelected(int fleetId) {
        Integer next = fleetId < 0 ? null : fleetId;
        if (next != null) {
            selectedSystem = null;
        }
        highlightedFleetId = next;
        cancelTargetSelection();
        if (next != null) {
            fleetsPanel.showDetails();
        }
        refresh();
    }

    private void onBadgeToggleLabel(int fleetId) {
        if (!badgesShowingFleetNo.add(fleetId)) {
            badgesShowingFleetNo.remove(fleetId);
        }
    }

    private void onFleetHighlight(int fleetId) {
        highlightedFleetId = highlightedFleetId != null && highlightedFleetId == fleetId ? null : fleetId;
        if (highlightedFleetId != null) {
            selectedSystem = null;
            cancelTargetSelection();
            fleetsPanel.showDetails();
        }
    }

    private void onStandingOrderSelected(int standingOrderId) {
        Integer next = standingOrderId < 0 ? null : standingOrderId;
        highlightedStandingOrderId = Objects.equals(highlightedStandingOrderId, next) ? null : next;
        refresh();
    }

    private void cancelOrder(PlannedOrder o) {
        int pid = currentSeat();
        Integer standingOrderId = o.standingOrderId();
        boolean ok;
        if (o.standing() && standingOrderId != null) {
            ok = pid >= 0 && game.removeStandingOrder(gameId, pid, standingOrderId);
        } else {
            ok = pid >= 0 && game.cancelOrder(gameId, pid, o.index());
        }
        if (!ok) {
            Notification.show(I18n.t(UiTexts.MAP_CANCEL_ORDER_FAILED));
        }
        refresh();
    }

    private void setGarrisonReserve(int systemId, int reserve) {
        int pid = currentSeat();
        if (pid < 0 || !game.setGarrisonReserve(gameId, pid, systemId, reserve)) {
            Notification.show(I18n.t(UiTexts.MAP_INVALID_COMMAND));
            return;
        }
        refresh();
    }

    private void deleteStandingOrder(StandingOrderView order) {
        int pid = currentSeat();
        if (pid < 0 || !game.removeStandingOrder(gameId, pid, order.id())) {
            Notification.show(I18n.t(UiTexts.MAP_REMOVE_STANDING_ORDER_FAILED));
            return;
        }
        refresh();
    }

    private void editStandingOrder(StandingOrderView order) {
        int pid = currentSeat();
        if (pid < 0) {
            return;
        }
        PlayerViewState view = viewForCurrentMode();
        if (view == null) {
            return;
        }
        VisibleSystem from = view.systems().stream().filter(system -> system.id() == order.fromSystemId())
                .findFirst().orElse(null);
        VisibleSystem to = view.systems().stream().filter(system -> system.id() == order.toSystemId())
                .findFirst().orElse(null);
        if (from == null || to == null) {
            Notification.show(I18n.t(UiTexts.MAP_REMOVE_STANDING_ORDER_FAILED));
            return;
        }
        SendFleetDialog.editRelocation(game, gameId, pid, from, to, order.ships(), () -> {
            refresh();
            fleetsPanel.showRelocations();
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get(GAME_ID_PARAMETER).orElse(null);
        if (parameter == null || parameter.isBlank()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        GameId candidate = GameId.of(parameter);
        if (game.viewForAccount(candidate, UserContext.currentPlayerId().orElse("")).isEmpty()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        this.gameId = candidate;
        mapCanvas.useGame(candidate);
        closeGameChat();
        displayedTurn = null;
        witnessedRunningGame = false;
        resetOutcomePresentation();
        reviewControls.reset();
        spectatorControls.reset();
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    protected void onAttach(AttachEvent attachEvent) {
        if (gameId == null) {
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
            return;
        }
        if (isObserver()) {
            if (!game.hasActiveGame(gameId)) {
                getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
                return;
            }
        } else if (!game.hasStartedGame(gameId)) {
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
            return;
        }
        UI ui = attachEvent.getUI();
        broadcasterSubscription = broadcaster.subscribe(gameId, event -> ui.access(() -> {
            if (event instanceof GameEvent.ChatMessage) {
                return;
            }
            if (event instanceof GameEvent.GameContinued) {
                witnessedRunningGame = true;
                resetOutcomePresentation();
                reviewControls.reset();
            }
            refresh();
        }));
        refresh();
        mapCanvas.installDragToPan();
        restoreViewport();
    }

    private void restoreViewport() {
        int pid = currentSeat();
        if (pid < 0) {
            return;
        }
        PlayerViewState view = game.viewForAccount(gameId, UserContext.currentPlayerId().orElse("")).orElse(null);
        if (view == null) { return; }
        VisibleSystem home = view.systems().stream()
                .filter(s -> Objects.equals(s.ownerId(), pid))
                .findFirst().orElse(null);
        if (home == null) {
            return;
        }
        mapCanvas.restoreViewportOrCenterOn(home.x(), home.y());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        closeGameChat();
        resetOutcomePresentation();
        if (broadcasterSubscription != null) {
            broadcasterSubscription.remove();
            broadcasterSubscription = null;
        }
    }

    private void doLeave() {
        String playerId = UserContext.currentPlayerId().orElse(null);
        if (isObserver()) {
            game.leaveObserve(gameId, playerId);
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
            return;
        }
        int pid = currentSeat();
        if (pid < 0 || !game.leaveGame(gameId, pid)) {
            Notification.show(I18n.t(UiTexts.MAP_LEAVE_FAILED));
            return;
        }
        Notification.show(I18n.t(UiTexts.MAP_LEFT));
        getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
    }

    private int currentSeat() {
        String playerId = UserContext.currentPlayerId().orElse(null);
        return playerId == null ? -1 : game.seatFor(gameId, playerId).orElse(-1);
    }

    private boolean isObserver() {
        String playerId = UserContext.currentPlayerId().orElse(null);
        return playerId != null && game.isObserver(gameId, playerId);
    }

    private void refresh() {
        acknowledgements.reload();
        PlayerViewState view = viewForCurrentMode();
        if (view == null) {
            navigateToLobby();
            return;
        }
        boolean advancedTurn = displayedTurn != null && view.turn() > displayedTurn;
        displayedTurn = view.turn();
        boolean observer = isObserver() || reviewing;
        int playerId = observer ? -1 : currentSeat();
        planningActions.setVisible(!observer && !view.gameOver() && !view.roundStatus().hasSubmitted(playerId));
        selectPendingReplayEvent(view);
        configureHeader(view, observer, playerId);
        renderMapAndSidebar(view, playerId, observer);
        if (advancedTurn) {
            fleetsPanel.showReport();
        }
        showOutcomeWhenDue(view);
    }

    private @Nullable PlayerViewState viewForCurrentMode() {
        PlayerViewState view = game.viewForAccount(gameId, UserContext.currentPlayerId().orElse("")).orElse(null);
        if (view == null) {
            return null;
        }
        if (!view.gameOver()) {
            witnessedRunningGame = true;
            resetOutcomePresentation();
        }
        TurnReport report = view.report();
        boolean hasPendingBattles = report != null && !acknowledgements.pending(gameId, report).isEmpty();
        if (hasPendingBattles) {
            witnessedRunningGame = true;
        }
        boolean awaitingOutcome = view.gameOver() && !outcomeAcknowledged && currentSeat() >= 0
                && (witnessedRunningGame || hasPendingBattles);
        reviewing = view.gameOver() && !awaitingOutcome;
        spectatorControls.setVisible(false);
        if (!reviewing) {
            if (isObserver()) {
                spectatorControls.show(view.players());
                return game.observerViewFor(gameId, UserContext.currentPlayerId().orElse(""),
                        spectatorControls.perspective(), spectatorControls.fogOfWar()).orElse(null);
            }
            return view;
        }
        String account = UserContext.currentPlayerId().orElse("");
        List<Integer> replayTurns = game.replayTurns(gameId, account);
        int selectedPerspective = reviewControls.perspective();
        if (selectedPerspective < 0) {
            selectedPerspective = view.players().stream().filter(player -> player.id() == currentSeat())
                    .map(Player::id).findFirst().or(() -> view.players().stream().findFirst().map(Player::id))
                    .orElse(-1);
        }
        List<ReplayEventMarker> eventMarkers = selectedPerspective < 0 ? List.of()
                : game.replayEventMarkers(gameId, account, selectedPerspective);
        reviewControls.show(view.players(), currentSeat(), replayTurns, eventMarkers);
        int replayTurn = reviewControls.replayTurn();
        if (replayTurn >= 0) {
            return game.replayFor(gameId, UserContext.currentPlayerId().orElse(""),
                    reviewControls.perspective(), replayTurn).orElse(null);
        }
        return game.reviewFor(gameId, UserContext.currentPlayerId().orElse(""),
                reviewControls.perspective(), reviewControls.fogOfWar()).orElse(null);
    }

    private void selectPendingReplayEvent(PlayerViewState view) {
        ReplayEventMarker marker = pendingReplayEvent;
        if (marker == null || reviewControls.replayTurn() != marker.turn()) { return; }
        view.systems().stream().filter(system -> system.id() == marker.systemId()).findFirst().ifPresent(system -> {
            selectedSystem = system;
            cancelTargetSelection();
            highlightedReportSystemId = marker.systemId();
            mapCanvas.centerOn(system.x(), system.y());
        });
        pendingReplayEvent = null;
    }

    private void configureHeader(PlayerViewState view, boolean observer, int playerId) {
        reviewControls.setVisible(reviewing);
        fleetsPanel.setViewsVisible(!observer || reviewing);
        fleetsPanel.setReportAvailable(currentSeat() >= 0);
        header.setNextVisible(!reviewing && (!observer || game.isAiOnly(gameId)));
        header.setLeaveVisible(!reviewing);
        header.setRoundRulesVisible(!reviewing && UserContext.currentPlayerId()
                .flatMap(account -> game.hostPlayerIdOf(gameId).filter(account::equals)).isPresent());
        header.setLeaveText(I18n.t(observer ? UiTexts.MAP_ACTION_LEAVE_OBSERVE : UiTexts.MAP_ACTION_LEAVE));
        header.setEmpireStatsVisible(!observer && pendingBattleSystemIds(view).isEmpty());
        header.setRound(view.turn());
        RoundStatus status = reviewing ? RoundStatus.NONE : view.roundStatus();
        roundStatus.update(status, playerId);
        header.setGameName(game.gameNameOf(gameId));
        header.updateTurnAction(status, playerId, observer, view.gameOver());
        header.setLogisticsActive(logisticsMode);
        gameOverBanner.setVisible(view.gameOver() && reviewing);
        if (view.gameOver() && reviewing) {
            gameOverBanner.setText(I18n.t(UiTexts.MAP_GAME_OVER, winnerName(view)));
        }
        if (!observer) {
            header.updateEmpireStats(view);
        }
    }

    private void openGameChat() {
        GameChatDialog current = chatDialog;
        if (current == null) {
            current = new GameChatDialog(gameChat, players, gameId, broadcaster);
            chatDialog = current;
        }
        current.open();
    }

    private void closeGameChat() {
        GameChatDialog current = chatDialog;
        chatDialog = null;
        if (current != null) {
            current.close();
        }
    }

    private void openRoundRules() {
        RoundRulesDialog.open(game, gameId, this::refresh);
    }

    /**
     * Opens the outcome dialog once the last battle of the final round is acknowledged. A final
     * round without own battles (a defeat decided elsewhere) opens it right away; otherwise the
     * player would stay in the waiting state with no way into the review.
     */
    private void showOutcomeWhenDue(PlayerViewState view) {
        if (reviewing || !view.gameOver() || outcomeDialogOpen) {
            return;
        }
        TurnReport report = view.report();
        if (report != null && !acknowledgements.pending(gameId, report).isEmpty()) {
            return;
        }
        String account = UserContext.currentPlayerId().orElse("");
        GameOutcomeStatistics statistics = game.outcomeStatisticsFor(gameId, account).orElse(null);
        if (statistics == null) {
            outcomeAcknowledged = true;
            refresh();
            return;
        }
        outcomeDialogOpen = true;
        String winnerColor = view.players().stream()
                .filter(player -> Objects.equals(view.winnerId(), player.id()))
                .map(Player::colorHex).findFirst().orElse("");
        outcomeDialog = GameOutcomeDialog.open(Objects.equals(view.winnerId(), currentSeat()), winnerName(view), winnerColor,
                statistics, () -> {
            game.continueAfterVictory(gameId, UserContext.currentPlayerId().orElse(""));
            resetOutcomePresentation();
            refresh();
        }, () -> {
            outcomeDialogOpen = false;
            outcomeDialog = null;
            outcomeAcknowledged = true;
            refresh();
        }, () -> {
            outcomeDialogOpen = false;
            outcomeDialog = null;
            outcomeAcknowledged = true;
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
        });
    }

    private void resetOutcomePresentation() {
        outcomeAcknowledged = false;
        outcomeDialogOpen = false;
        Dialog dialog = outcomeDialog;
        outcomeDialog = null;
        if (dialog != null) {
            dialog.close();
        }
    }

    private void renderMapAndSidebar(PlayerViewState view, int playerId, boolean observer) {
        Set<Integer> liveFleetIds = view.ownFleets().stream()
                .map(Fleet::globalId).collect(Collectors.toSet());
        badgesShowingFleetNo.retainAll(liveFleetIds);
        if (highlightedFleetId != null && !liveFleetIds.contains(highlightedFleetId)) {
            highlightedFleetId = null;
        }
        List<VisibleSystem> systems = view.systems().stream()
                .sorted(Comparator.comparingInt(VisibleSystem::id)).toList();
        mapCanvas.render(new MapRenderer.Inputs(
                game, gameId, view, playerId, observer, selectedFrom, targetSelection == TargetSelection.RELOCATION,
                highlightedFleetId, highlightedStandingOrderId, reportedSystemIds(view), highlightedReportSystemId,
                pendingBattleSystemIds(view), logisticsMode,
                badgesShowingFleetNo, systems,
                this::inspectSystem,
                this::selectSource,
                this::openSend,
                this::openRelocation,
                this::onBadgeToggleLabel,
                this::onFleetHighlight,
                this::onReportMarkerSelected,
                this::refresh));
        fleetsPanel.update(gameId, view, selectedSystem, highlightedFleetId, highlightedStandingOrderId, observer,
                targetSelection != TargetSelection.NONE);
    }

    private void inspectSystem(VisibleSystem system) {
        selectedSystem = system;
        cancelTargetSelection();
        highlightedFleetId = null;
        fleetsPanel.showDetails();
        refresh();
    }

    private void selectSource(VisibleSystem system) {
        selectedFrom = selectedFrom != null && selectedFrom.id() == system.id() ? null : system;
        targetSelection = selectedFrom == null ? TargetSelection.NONE : TargetSelection.FLEET;
        selectedSystem = system;
        highlightedFleetId = null;
        fleetsPanel.showDetails();
        refresh();
    }

    private void selectFleetTarget(VisibleSystem system) {
        selectTarget(system, TargetSelection.FLEET);
    }

    private void selectRelocationTarget(VisibleSystem system) {
        selectTarget(system, TargetSelection.RELOCATION);
    }

    private void selectTarget(VisibleSystem system, TargetSelection selection) {
        selectedSystem = system;
        selectedFrom = system;
        targetSelection = selection;
        highlightedFleetId = null;
        fleetsPanel.showDetails();
        refresh();
    }

    private void cancelTargetSelection() {
        selectedFrom = null;
        targetSelection = TargetSelection.NONE;
    }

    private void cancelTargetSelectionAndRefresh() {
        cancelTargetSelection();
        refresh();
    }

    private Set<Integer> reportedSystemIds(PlayerViewState view) {
        TurnReport report = view.report();
        if (report == null) {
            return Set.of();
        }
        return report.events().stream()
                .map(TurnEvent::battleSystemId)
                .filter(OptionalInt::isPresent)
                .map(OptionalInt::getAsInt)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<Integer> pendingBattleSystemIds(PlayerViewState view) {
        TurnReport report = view.report();
        return report == null ? Set.of() : acknowledgements.pending(gameId, report);
    }

    private void onReportSystemSelected(int systemId) {
        highlightedReportSystemId = systemId;
        PlayerViewState view = viewForCurrentMode();
        if (view != null) {
            view.systems().stream().filter(system -> system.id() == systemId).findFirst()
                    .ifPresent(system -> mapCanvas.centerOn(system.x(), system.y()));
        }
        fleetsPanel.selectReportSystem(systemId);
        refresh();
    }

    private void onReportMarkerSelected(int systemId) {
        highlightedReportSystemId = systemId;
        fleetsPanel.selectReportSystem(systemId);
        refresh();
    }

    private void onResolvedBattleSelected(int systemId) {
        highlightedReportSystemId = systemId;
        PlayerViewState view = viewForCurrentMode();
        if (view != null) {
            view.systems().stream().filter(system -> system.id() == systemId).findFirst().ifPresent(system -> {
                selectedSystem = system;
                mapCanvas.centerOn(system.x(), system.y());
            });
        }
        fleetsPanel.showDetails();
        refresh();
    }

    private void navigateToLobby() {
        getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
    }

    private void openSend(VisibleSystem from, VisibleSystem to) {
        if (reviewing) { return; }
        int pid = currentSeat();
        if (pid < 0) {
            return;
        }
        SendFleetDialog.open(game, gameId, pid, from, to, () -> {
            cancelTargetSelection();
            selectedSystem = to;
            refresh();
            fleetsPanel.showOrders();
        });
    }

    private void openRelocation(VisibleSystem from, VisibleSystem to) {
        if (reviewing) {
            return;
        }
        int pid = currentSeat();
        if (pid < 0) {
            return;
        }
        SendFleetDialog.openRelocation(game, gameId, pid, from, to, () -> {
            cancelTargetSelection();
            selectedSystem = to;
            refresh();
            fleetsPanel.showRelocations();
        });
    }

    private String winnerName(PlayerViewState view) {
        Integer winnerId = view.winnerId();
        if (winnerId == null) {
            return "?";
        }
        return view.players().stream()
                .filter(p -> p.id() == winnerId)
                .map(Player::label)
                .findFirst()
                .orElse("?");
    }
}
