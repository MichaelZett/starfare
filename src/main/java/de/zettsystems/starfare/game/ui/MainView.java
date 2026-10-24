package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import de.zettsystems.starfare.diplomacy.ui.DiplomacyDialog;
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
import de.zettsystems.starfare.style.ui.Devices;
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
    private final Div treatyActions = new Div();
    private final Div actionBar = new Div();
    private final Div chatHost = new Div();
    private @Nullable DiplomacyDialog diplomacyDialog;
    /** Present only on phones; desktops get the split layout instead, never both. */
    private final @Nullable MobileMapLayout mobile;

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
    private @Nullable GameChatPanel chatPanel;
    private boolean logisticsMode;
    private @Nullable PlayerViewState economicView;
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
        fleetsPanel.onMining(() -> de.zettsystems.starfare.economy.ui.MiningAutomationDialog.open(game, gameId,
                UserContext.currentPlayerId().orElse(""), fleetsPanel.protectedProductionSystems()));
        fleetsPanel.onMiningProtection((system, protect) -> {
            if (displayedTurn != null && !game.protectMiningSystem(gameId, UserContext.currentPlayerId().orElse(""),
                    displayedTurn, system, protect)) { Notification.show(I18n.t(UiTexts.MINING_REJECTED)); }
        });
        fleetsPanel.onEditOrder(order -> planningDialogs().edit(order));
        fleetsPanel.onFleetWait(id -> executeFleetAction(id, false));
        fleetsPanel.onFleetDisband(id -> executeFleetAction(id, true));
        fleetsPanel.onRouteStationSelected(id -> {
            var view = viewForCurrentMode();
            if (view != null) { view.systems().stream().filter(system -> system.id() == id).findFirst().ifPresent(system -> {
                selectedSystem = system; highlightedFleetId = null; cancelTargetSelection();
                mapCanvas.centerOn(system.x(), system.y()); fleetsPanel.showDetails(); refresh();
            }); }
        });
        fleetsPanel.onDiplomacySelected(this::openDiplomacy);
        fleetsPanel.onProductionAllocation(this::allocateExpansion);
        fleetsPanel.onEconomySelected(this::openEconomicSystem);
        fleetsPanel.onProductionPreview(plan -> game.previewProduction(gameId, UserContext.currentPlayerId().orElse(""), plan));
        fleetsPanel.onRecycling(order -> {
            int player = currentSeat();
            if (player < 0 || !game.planRecycling(gameId, player, order.turn(), order.systemId(), order.ships())) {
                Notification.show(I18n.t(UiTexts.PLAN_REJECTED));
                refresh();
            }
        });
        planningActions.addClassName("planning-actions");
        planningActions.add(planningButton(UiTexts.PLAN_UNDO_SHORT, UiTexts.PLAN_UNDO, () -> {
            if (!game.undoOrders(gameId, currentSeat(), Objects.requireNonNull(displayedTurn))) {
                Notification.show(I18n.t(UiTexts.PLAN_REJECTED));
            }
            refresh();
        }), planningButton(UiTexts.PLAN_DISPATCH_SHORT, UiTexts.PLAN_DISPATCH, () -> planningDialogs().dispatch()),
                planningButton(UiTexts.PLAN_RESERVES_SHORT, UiTexts.PLAN_RESERVES, () -> planningDialogs().reserves()));
        actionBar.addClassName("map-controls");
        actionBar.add(treatyActions, planningActions);
        fleetsPanel.setActions(actionBar);
        chatHost.addClassName("map-chat-host");
        chatHost.setVisible(false);
        treatyActions.add(new com.vaadin.flow.component.button.Button(I18n.t(UiTexts.DIP_TITLE), _ -> openDiplomacy()));
        boolean phone = Devices.mobile();
        header = new MapHeaderBar(new MapHeaderBar.Actions(this::onNextRound, this::toggleLogisticsMode, this::leaveGameNow,
                () -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)), this::openGameChat, this::openRoundRules, this::openShipResearch), phone);

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
        if (phone) {
            mobile = new MobileMapLayout(List.of(header, roundStatus, spectatorControls, reviewControls), gameOverBanner,
                    mapCanvas, fleetsPanel, chatHost, this::cancelTargetSelectionAndRefresh, this::hideGameChat);
            addClassName("map-root-mobile");
            add(mobile);
        } else {
            mobile = null;
            add(header, roundStatus, spectatorControls, reviewControls, buildContent(),
                    new de.zettsystems.starfare.legal.ui.LegalFooter());
        }
    }

    /** Brings the sidebar into view; on phones it lies as a sheet over the map. */
    private void revealPanel() {
        if (mobile != null) { mobile.showPanel(); }
    }

    private static com.vaadin.flow.component.button.Button planningButton(String shortKey, String fullKey, Runnable action) {
        var button = new com.vaadin.flow.component.button.Button(I18n.t(shortKey), _ -> action.run());
        button.setAriaLabel(I18n.t(fullKey));
        button.setTooltipText(I18n.t(fullKey));
        return button;
    }

    /** On phones the sheet covers the map, so anything that points at the map closes it. */
    private void revealMap() {
        if (mobile != null) { mobile.showMap(); }
    }

    private void openDiplomacy() {
        if (game.diplomacyFor(gameId, UserContext.currentPlayerId().orElse("")).isEmpty()) { return; }
        var dialog = new DiplomacyDialog(game, gameId, UserContext.currentPlayerId().orElse(""));
        diplomacyDialog = dialog; dialog.refresh(); dialog.open();
    }

    private PlanningDialogs planningDialogs() {
        return new PlanningDialogs(game, gameId, currentSeat(), concealedPlanningView(), () -> {
            refresh();
            fleetsPanel.showOrders();
        });
    }

    private PlayerViewState concealedPlanningView() {
        PlayerViewState view = game.viewFor(gameId, currentSeat());
        return CoalitionConcealment.hide(view, pendingBattleSystemIds(view));
    }

    private com.vaadin.flow.component.orderedlayout.HorizontalLayout buildContent() {
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
        var workspace = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(content, chatHost);
        workspace.setPadding(false);
        workspace.setSpacing(false);
        workspace.setSizeFull();
        workspace.setFlexGrow(1, content);
        workspace.addClassName("map-workspace");
        return workspace;
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

    private void updateEconomyNotices() {
        var view = economicView;
        if (view == null) { return; }
        var issues = de.zettsystems.starfare.economy.values.EconomyIssues.forView(view, currentSeat(), pendingBattleSystemIds(view));
        boolean hidden = isObserver() || reviewing || !pendingBattleSystemIds(view).isEmpty();
        header.economyNotices().show(hidden ? List.of() : issues, kind -> { fleetsPanel.showEconomy(kind); revealPanel(); });
        mapCanvas.economyMarkers(hidden ? List.of() : issues, reportedSystemIds(view), id -> {
            issues.stream().filter(issue -> issue.system().id() == id).findFirst()
                    .ifPresent(issue -> { fleetsPanel.showEconomy(issue.reasons().getFirst()); revealPanel(); });
        });
    }

    private void openEconomicSystem(int id, int section) {
        var view = viewForCurrentMode();
        if (view == null) { return; }
        view.systems().stream().filter(system -> system.id() == id && system.garrisonReserve() != null).findFirst().ifPresent(system -> {
            selectedSystem = system; cancelTargetSelection(); highlightedFleetId = null;
            mapCanvas.centerOn(system.x(), system.y()); fleetsPanel.showDetails(); refresh(); fleetsPanel.openProduction(section);
        });
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
            revealPanel();
        }
        refresh();
    }

    private void executeFleetAction(int fleetId, boolean disband) {
        int pid = currentSeat();
        boolean accepted = pid >= 0 && (disband
                ? game.disbandFleet(gameId, pid, fleetId)
                : game.setFleetWait(gameId, pid, fleetId));
        if (!accepted) {
            Notification.show(I18n.t(disband ? UiTexts.MAP_DISBAND_FAILED : UiTexts.MAP_WAIT_FAILED));
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
            revealPanel();
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

    private de.zettsystems.starfare.economy.values.ProductionCommit allocateExpansion(
            de.zettsystems.starfare.economy.values.ProductionAllocation allocation) {
        var result = currentSeat() < 0 ? de.zettsystems.starfare.economy.values.ProductionCommit.REJECTED
                : game.commitProduction(gameId, UserContext.currentPlayerId().orElse(""), allocation);
        if (!result.accepted()) {
            Notification.show(I18n.t(de.zettsystems.starfare.economy.ui.ProductionKeys.NOT_SAVED));
            refresh();
        }
        return result;
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
        SendFleetDialog.editRelocation(game, gameId, pid, from, to, order.ships(), new SendDialogOutcome(this::refresh, this::refresh));
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
        if (diplomacyDialog != null) { diplomacyDialog.close(); diplomacyDialog = null; }
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
        detachEvent.getUI().getPage().executeJs("window.__starfareProductionDirty = false");
        closeGameChat();
        resetOutcomePresentation();
        if (diplomacyDialog != null) { diplomacyDialog.close(); diplomacyDialog = null; }
        if (broadcasterSubscription != null) {
            broadcasterSubscription.remove();
            broadcasterSubscription = null;
        }
    }

    private void leaveGameNow() {
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
        fleetsPanel.miningPolicy(observer ? null : game.miningAutomationFor(gameId, UserContext.currentPlayerId().orElse(""))
                .map(de.zettsystems.starfare.economy.values.MiningAutomationView::policy).orElse(null));
        int playerId = observer ? -1 : currentSeat();
        var diplomacy = observer ? java.util.Optional.<de.zettsystems.starfare.diplomacy.values.DiplomacyView>empty()
                : game.diplomacyFor(gameId, UserContext.currentPlayerId().orElse(""));
        treatyActions.setVisible(diplomacy.isPresent());
        fleetsPanel.diplomacyAvailable(treatyActions.isVisible());
        fleetsPanel.alliedOwners(diplomacy.flatMap(value -> value.treaties().groupFor(value.player()))
                .map(de.zettsystems.starfare.diplomacy.values.AllianceGroup::members).orElse(java.util.Set.of()));
        var treaties = diplomacyDialog;
        if (treaties != null && treaties.isOpened()) { treaties.refresh(); }
        planningActions.setVisible(!observer && !view.gameOver() && !view.roundStatus().hasSubmitted(playerId));
        actionBar.setVisible(treatyActions.isVisible() || planningActions.isVisible());
        selectPendingReplayEvent(view);
        configureHeader(view, observer, playerId);
        renderMapAndSidebar(view, playerId, observer);
        if (advancedTurn) {
            fleetsPanel.showReport();
            revealPanel();
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
            return CoalitionConcealment.hide(view, pendingBattleSystemIds(view));
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
        header.setResearchVisible(!reviewing && !observer && pendingBattleSystemIds(view).isEmpty() && game.shipResearchFor(gameId,
                UserContext.currentPlayerId().orElse("")).isPresent());
        header.setRound(view.turn());
        RoundStatus status = reviewing ? RoundStatus.NONE : view.roundStatus();
        roundStatus.update(status, playerId);
        header.setGameName(game.gameNameOf(gameId));
        header.setEmpireName(observer ? "" : view.players().stream().filter(player -> player.id() == playerId)
                .map(de.zettsystems.starfare.game.values.Player::empireNameOrName).findFirst().orElse(""));
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
        if (targetSelection != TargetSelection.NONE || reviewing || isObserver() || !game.hasActiveGame(gameId)) { return; }
        GameChatPanel current = chatPanel;
        if (current == null) {
            current = new GameChatPanel(gameChat, players, gameId, broadcaster, () -> {
                chatHost.setVisible(false);
                header.getElement().executeJs("this.querySelector('.map-chat-toggle')?.focus()");
            });
            chatPanel = current;
            chatHost.add(current);
        }
        chatHost.setVisible(true);
        current.open();
    }

    private void closeGameChat() {
        GameChatPanel current = chatPanel;
        chatPanel = null;
        if (current != null) {
            current.close();
        }
        chatHost.removeAll();
        chatHost.setVisible(false);
    }

    private void hideGameChat() {
        GameChatPanel current = chatPanel;
        if (current != null) { current.close(); }
    }

    private void openShipResearch() {
        if (!reviewing) {
            de.zettsystems.starfare.research.ui.ResearchDialog.open(game, gameId, UserContext.currentPlayerId().orElse(""));
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
        outcomeDialog = GameOutcomeDialog.open(view.wonBy(currentSeat()),
                game.canContinueAfterVictory(gameId, account), winnerName(view), winnerColor,
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
                this::refresh,
                rangeForSelection(playerId, observer)));
        fleetsPanel.update(gameId, view, selectedSystem, highlightedFleetId, highlightedStandingOrderId, observer,
                targetSelection != TargetSelection.NONE);
        if (mobile != null) { mobile.targetSelection(targetSelection != TargetSelection.NONE); }
        boolean chatPermitted = !observer && !view.gameOver();
        header.setChatAvailable(chatPermitted, targetSelection != TargetSelection.NONE);
        if ((!chatPermitted || targetSelection != TargetSelection.NONE) && chatPanel != null) { chatPanel.close(); }
        economicView = view;
        updateEconomyNotices();
    }

    private de.zettsystems.starfare.navigation.values.@Nullable RangeView rangeForSelection(int playerId, boolean observer) {
        VisibleSystem source = selectedFrom;
        if (source == null || observer || playerId < 0 || targetSelection == TargetSelection.NONE) { return null; }
        return game.rangeFor(gameId, playerId, source.id()).orElse(null);
    }

    private void inspectSystem(VisibleSystem system) {
        selectedSystem = system;
        cancelTargetSelection();
        highlightedFleetId = null;
        fleetsPanel.showDetails();
        revealPanel();
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
        revealMap();
        refresh();
    }

    private void onReportMarkerSelected(int systemId) {
        highlightedReportSystemId = systemId;
        fleetsPanel.selectReportSystem(systemId);
        revealPanel();
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
        SendFleetDialog.open(game, gameId, pid, from, to, new SendDialogOutcome(() -> {
            cancelTargetSelection();
            selectedSystem = from;
            refresh();
        }, this::refresh));
    }

    private void openRelocation(VisibleSystem from, VisibleSystem to) {
        if (reviewing) {
            return;
        }
        int pid = currentSeat();
        if (pid < 0) {
            return;
        }
        SendFleetDialog.openRelocation(game, gameId, pid, from, to, new SendDialogOutcome(() -> {
            cancelTargetSelection();
            selectedSystem = from;
            refresh();
        }, this::refresh));
    }

    private String winnerName(PlayerViewState view) {
        return OutcomeLabels.winningEmpires(view.outcome(), view.players());
    }
}
