package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import de.zettsystems.starfare.diplomacy.ui.DiplomacyDialog;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import de.zettsystems.starfare.game.application.GameTemplateService;
import de.zettsystems.starfare.game.application.RulesetAccess;
import de.zettsystems.gamesplatformapi.MatchView;
import de.zettsystems.gamesplatformapi.PlatformSettings;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Primary UI view showing map and fleet controls. Route is /map/{gameId}.
 * Fleet commands are issued directly on the map: click own system to select source,
 * click target to open the send dialog; right-click a fleet line for Wait/Disband.
 */
@Route(StarfareRoutes.MAP)
@CssImport("./styles/starfare.css")
@JsModule("./battle-replay.js")
@PermitAll
public class MainView extends VerticalLayout implements BeforeEnterObserver, MatchView {

    private enum TargetSelection { NONE, FLEET, RELOCATION }
    private static final String GAME_ID_PARAMETER = "gameId";
    private final GameService game;
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
    private @Nullable DiplomacyDialog diplomacyDialog;
    /** Present only on phones; desktops get the split layout instead, never both. */
    private final @Nullable MobileMapLayout mobile;

    @SuppressWarnings("NullAway.Init")
    private GameId gameId;
    private boolean reviewing;
    /** The shown game limits flight range; the map help then explains stations. */
    private boolean routeRules;
    private @Nullable VisibleSystem selectedSystem;
    private @Nullable VisibleSystem selectedFrom;
    private TargetSelection targetSelection = TargetSelection.NONE;
    private @Nullable Integer highlightedFleetId;
    /** Arrival rounds at the last badge target, asked again only for a new target or round. */
    private String arrivalKey = "";
    private java.util.Map<Integer, Integer> arrivalTurns = java.util.Map.of();
    /** Sources picked on the map for a joint attack on the current target; view-local, reset with target or round. */
    private final Set<Integer> jointSources = new java.util.HashSet<>();
    private @Nullable Integer highlightedStandingOrderId;
    private @Nullable Integer highlightedReportSystemId;
    private @Nullable ReplayEventMarker pendingReplayEvent;
    private @Nullable Integer displayedTurn;
    /** The walkthrough event the view last centred on, as game, round and report index. */
    private @Nullable String walkthroughFocus;
    private @Nullable VisibleSystem walkthroughSystem;
    /** Seated players go through the located events of the round before they may give orders. */
    private boolean walkthroughOpen;
    /** No orders: the walkthrough is open or the game is decided. */
    private boolean planningLocked;
    /** The open walkthrough event in front of the whole board; set up once the board is laid out. */
    private final RoundCurtain curtain;
    /** Remember the pending outcome even when the view was reloaded during the final battles. */
    private boolean witnessedRunningGame;
    private boolean outcomeAcknowledged;
    private boolean outcomeDialogOpen;
    private @Nullable Dialog outcomeDialog;
    private final Set<Integer> badgesShowingFleetNo = new HashSet<>();
    private final BattleAcknowledgements acknowledgements;
    private final GameTemplateService templates;
    private final RulesetAccess rulesetAccess;
    private boolean templateOffered;

    @Autowired
    public MainView(GameService service, Broadcaster broadcaster, BattleAcknowledgementService battleAcknowledgements,
                    @Nullable PlatformSettings preferences,
                    GameTemplateService templates, RulesetAccess rulesetAccess,
                    @Nullable OwnFrameParts frame) {
        DisplayPreferences.load(preferences);
        this.game = service;
        this.templates = templates;
        this.rulesetAccess = rulesetAccess;
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
                this::selectFleetTarget,
                this::selectRelocationTarget,
                this::cancelTargetSelectionAndRefresh);
        mapCanvas = new MapCanvas(this::onMapBackgroundClick);
        fleetsPanel.onEditOrder(order -> planningDialogs().edit(order));
        fleetsPanel.onCancelDeparture(this::cancelDeparture);
        fleetsPanel.onFleetWait(id -> executeFleetAction(id, false));
        fleetsPanel.onFleetDisband(id -> executeFleetAction(id, true));
        fleetsPanel.onSystemSelected(id -> {
            var view = viewForCurrentMode();
            if (view != null) { view.systems().stream().filter(system -> system.id() == id).findFirst().ifPresent(system -> {
                selectedSystem = system; highlightedFleetId = null; cancelTargetSelection();
                mapCanvas.centerOn(system.x(), system.y()); fleetsPanel.showDetails(); refresh();
            }); }
        });
        fleetsPanel.onDiplomacySelected(this::openDiplomacy);
        fleetsPanel.onProductionAllocation(this::allocateExpansion);
        fleetsPanel.onProductionPreview(plan -> game.previewProduction(gameId, UserContext.currentPlayerId().orElse(""), plan));
        planningActions.addClassName("planning-actions");
        planningActions.add(iconAction(VaadinIcon.ROTATE_LEFT, UiTexts.PLAN_UNDO, () -> {
            if (!game.undoOrders(gameId, currentSeat(), Objects.requireNonNull(displayedTurn))) {
                Notification.show(I18n.t(UiTexts.PLAN_REJECTED));
            }
            refresh();
        }));
        actionBar.addClassName("map-controls");
        actionBar.add(treatyActions, planningActions);
        fleetsPanel.setActions(actionBar);
        treatyActions.add(iconAction(VaadinIcon.HANDSHAKE, UiTexts.DIP_TITLE, this::openDiplomacy));
        boolean phone = Devices.mobile();
        header = new MapHeaderBar(new MapHeaderBar.Actions(this::onNextRound, this::leaveGameNow,
                () -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)), this::openRoundRules, this::confirmResign), phone, frame);

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
        mobile = phone ? new MobileMapLayout(List.of(header, roundStatus, spectatorControls, reviewControls), gameOverBanner,
                mapCanvas, fleetsPanel, this::cancelTargetSelectionAndRefresh, () -> routeRules) : null;
        layOut(frame);
        curtain = new RoundCurtain(getChildren().toList());
        add(curtain);
    }

    /** Phones get the sheet layout, wider screens the sidebar beside the map; the own start adds its footer. */
    private void layOut(@Nullable OwnFrameParts frame) {
        MobileMapLayout phoneLayout = mobile;
        if (phoneLayout != null) {
            if (frame != null) { phoneLayout.addBelowPanel(frame.legalFooter()); }
            addClassName("map-root-mobile");
            add(phoneLayout);
        } else {
            add(header, roundStatus, spectatorControls, reviewControls, buildContent());
            if (frame != null) { add(frame.legalFooter()); }
        }
    }

    /** Brings the sidebar into view; on phones it lies as a sheet over the map. */
    private void revealPanel() {
        if (mobile != null) { mobile.showPanel(); }
    }

    private static com.vaadin.flow.component.button.Button iconAction(VaadinIcon icon, String key, Runnable action) {
        var button = new com.vaadin.flow.component.button.Button(icon.create(), _ -> action.run());
        button.setAriaLabel(I18n.t(key));
        button.setTooltipText(I18n.t(key));
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
            fleetsPanel.showFleets();
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
        left.add(gameOverBanner, new MapControls(mapCanvas, () -> routeRules), mapCanvas);
        left.setFlexGrow(1, mapCanvas);

        var content = new SplitLayout(left, fleetsPanel);
        content.setWidthFull();
        content.setHeightFull();
        content.setSplitterPosition(Math.clamp(DisplayPreferences.number(de.zettsystems.starfare.game.values.DisplaySetting.SIDEBAR, 70), 20, 85));
        content.addSplitterDragEndListener(_ -> DisplayPreferences.choose(
                de.zettsystems.starfare.game.values.DisplaySetting.SIDEBAR, String.valueOf(content.getSplitterPosition())));
        content.addClassName("map-content");
        var workspace = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(content);
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
        Integer shownTurn = displayedTurn;
        if (walkthroughOpen) {
            Notification.show(I18n.t(UiTexts.ROUND_WALKTHROUGH_HINT));
            refresh();
            return;
        }
        if (pid < 0 || shownTurn == null) {
            Notification.show(I18n.t(UiTexts.MAP_SUBMIT_FAILED));
            return;
        }
        // The shown round: a deadline that ended it first must not turn the click into a submission of the next one.
        if (!game.submitTurn(gameId, pid, shownTurn)) {
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

    private void cancelDeparture(PlannedDeparture departure) {
        int pid = currentSeat();
        PlayerViewState view = viewForCurrentMode();
        if (pid < 0 || view == null || !game.cancelDeparture(gameId, pid, view.turn(), departure)) {
            Notification.show(I18n.t(UiTexts.MAP_CANCEL_ORDER_FAILED));
        }
        refresh();
    }

    private void toggleJointSource(int systemId) {
        if (!jointSources.remove(systemId)) { jointSources.add(systemId); }
        refresh();
    }

    private void openJointAttack(JointArrival joint) {
        planningDialogs().jointAttack(joint, () -> {
            jointSources.clear();
            refresh();
            fleetsPanel.showFleets();
        });
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

    /** The game shown once {@link #beforeEnter} let the account see it; the platform puts its chat around it. */
    @Override
    public Optional<String> shownMatchId() {
        return Optional.ofNullable(gameId).map(GameId::value);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get(GAME_ID_PARAMETER).orElse(null);
        if (parameter == null || parameter.isBlank()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        GameId candidate = GameId.of(parameter);
        String account = UserContext.currentPlayerId().orElse("");
        if (game.viewForAccount(candidate, account).isEmpty()) {
            // A game that has not started yet has no map; its players gather in the waiting room.
            if (!game.hasStartedGame(candidate) && game.summaryFor(candidate, account).isPresent()) {
                event.forwardTo(WaitingRoomView.class, WaitingRoomView.parameters(candidate));
                return;
            }
            event.forwardTo(LobbyView.class);
            return;
        }
        this.gameId = candidate;
        routeRules = game.summaryFor(candidate, account).map(summary -> summary.ruleset().isExtended()).orElse(false);
        mapCanvas.useGame(candidate);
        if (diplomacyDialog != null) { diplomacyDialog.close(); diplomacyDialog = null; }
        displayedTurn = null;
        walkthroughFocus = null;
        witnessedRunningGame = false;
        resetOutcomePresentation();
        reviewControls.reset();
        templateOffered = false;
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
            GameId waiting = gameId;
            getUI().ifPresent(ui -> ui.navigate(WaitingRoomView.class, WaitingRoomView.parameters(waiting)));
            return;
        }
        UI ui = attachEvent.getUI();
        broadcasterSubscription = broadcaster.subscribe(gameId, event -> ui.access(() -> {
            if (event instanceof GameEvent.GameContinued) {
                witnessedRunningGame = true;
                resetOutcomePresentation();
                reviewControls.reset();
        templateOffered = false;
            }
            refresh();
        }));
        // A fresh view centres on the open walkthrough event again instead of the last viewport.
        walkthroughFocus = null;
        refresh();
        mapCanvas.installDragToPan();
        VisibleSystem focus = walkthroughSystem;
        if (focus != null) {
            mapCanvas.restoreZoomAndCenterOn(focus.x(), focus.y());
        } else {
            restoreViewport();
        }
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

    private void confirmResign() {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setId("resign-confirm");
        confirm.setHeader(I18n.t(UiTexts.MAP_RESIGN_TITLE));
        confirm.setText(I18n.t(UiTexts.MAP_RESIGN_TEXT));
        confirm.setCancelable(true);
        confirm.setCancelText(I18n.t(UiTexts.MAP_DIALOG_CANCEL));
        confirm.setConfirmText(I18n.t(UiTexts.MAP_RESIGN_CONFIRM));
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(_ -> resignNow());
        confirm.open();
    }

    private void resignNow() {
        int pid = currentSeat();
        if (pid < 0 || !game.resignGame(gameId, pid)) {
            Notification.show(I18n.t(UiTexts.MAP_LEAVE_FAILED));
            return;
        }
        Notification.show(I18n.t(UiTexts.MAP_RESIGNED));
        getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
    }

    /**
     * The review offers a new game with these settings (or a rematch) to whoever hosted or held a seat,
     * for rulesets the account may still use. It moved here from the own start's archive (plan S.3).
     */
    private void offerTemplate() {
        if (templateOffered) { return; }
        templateOffered = true;
        String account = UserContext.currentPlayerId().orElse("");
        game.summaryFor(gameId, account)
                .filter(summary -> summary.belongsTo(account) && rulesetAccess.mayUse(summary.ruleset(), account))
                .ifPresent(summary -> {
                    Button template = GameTemplateDialog.button(templates, gameId, summary.name(), () -> { });
                    template.addThemeVariants(ButtonVariant.TERTIARY);
                    template.setId("review-template");
                    reviewControls.offer(template);
                });
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
        refreshDiplomacy(observer);
        RoundWalkthrough.Step step = observer ? null : walkthroughStep(view);
        walkthroughOpen = step != null;
        planningLocked = walkthroughOpen || view.gameOver();
        if (planningLocked) { cancelTargetSelection(); }
        planningActions.setVisible(!observer && !planningLocked && !view.roundStatus().hasSubmitted(playerId));
        actionBar.setVisible(treatyActions.isVisible() || planningActions.isVisible());
        selectPendingReplayEvent(view);
        boolean newFocus = focusWalkthrough(view, step);
        fleetsPanel.walkthrough(step);
        configureHeader(view, observer, playerId);
        renderMapAndSidebar(view, playerId, observer);
        if (advancedTurn) {
            fleetsPanel.showReport();
            revealPanel();
        }
        curtain.show(fleetsPanel.walkthroughCard(game.isTutorial(gameId)).orElse(null));
        if (newFocus) { revealMap(); }
        showOutcomeWhenDue(view);
    }

    /** Alliance actions and allied owners for a seated player; spectators and the review see no diplomacy. */
    private void refreshDiplomacy(boolean observer) {
        var diplomacy = observer ? java.util.Optional.<de.zettsystems.starfare.diplomacy.values.DiplomacyView>empty()
                : game.diplomacyFor(gameId, UserContext.currentPlayerId().orElse(""));
        treatyActions.setVisible(diplomacy.isPresent());
        fleetsPanel.diplomacyAvailable(treatyActions.isVisible());
        fleetsPanel.alliedOwners(diplomacy.flatMap(value -> value.treaties().groupFor(value.player()))
                .map(de.zettsystems.starfare.diplomacy.values.AllianceGroup::members).orElse(java.util.Set.of()));
        var treaties = diplomacyDialog;
        if (treaties != null && treaties.isOpened()) { treaties.refresh(); }
    }

    /** The open event of the round walkthrough for the seated player; spectators and the review are never held up. */
    private RoundWalkthrough.@Nullable Step walkthroughStep(PlayerViewState view) {
        TurnReport report = view.report();
        if (reviewing || report == null || currentSeat() < 0) {
            return null;
        }
        return acknowledgements.walkthrough(gameId, report).orElse(null);
    }

    /**
     * Selects a new walkthrough event in the report and centres the map on it. Returns whether the event changed, so
     * phones can bring the map to the front; the same event on a later refresh leaves the view alone.
     */
    private boolean focusWalkthrough(PlayerViewState view, RoundWalkthrough.@Nullable Step step) {
        if (step == null) {
            walkthroughFocus = null;
            walkthroughSystem = null;
            return false;
        }
        String focus = gameId.value() + ":" + step.turn() + ":" + step.eventIndex();
        if (focus.equals(walkthroughFocus)) {
            return false;
        }
        walkthroughFocus = focus;
        highlightedReportSystemId = step.systemId();
        walkthroughSystem = view.systems().stream().filter(system -> system.id() == step.systemId())
                .findFirst().orElse(null);
        VisibleSystem system = walkthroughSystem;
        if (system != null) {
            mapCanvas.centerOn(system.x(), system.y());
        }
        fleetsPanel.focusReportSystem(step.systemId());
        return true;
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
        boolean hasOpenEvents = report != null && acknowledgements.walkthrough(gameId, report).isPresent();
        if (hasOpenEvents) {
            witnessedRunningGame = true;
        }
        boolean awaitingOutcome = view.gameOver() && !outcomeAcknowledged && currentSeat() >= 0
                && (witnessedRunningGame || hasOpenEvents);
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
        if (reviewing) { offerTemplate(); }
        fleetsPanel.setViewsVisible(!observer || reviewing);
        fleetsPanel.setReportAvailable(currentSeat() >= 0);
        header.setNextVisible(!reviewing && (!observer || game.isAiOnly(gameId)));
        header.setLeaveVisible(!reviewing);
        header.setResignVisible(!reviewing && !observer && !view.gameOver() && currentSeat() >= 0);
        header.setRoundRulesVisible(!reviewing && hostMayChangeRoundRules());
        header.setLeaveText(I18n.t(observer ? UiTexts.MAP_ACTION_LEAVE_OBSERVE : UiTexts.MAP_ACTION_LEAVE));
        header.setEmpireStatsVisible(!observer && pendingBattleSystemIds(view).isEmpty());
        header.setRound(view.turn());
        RoundStatus status = reviewing ? RoundStatus.NONE : view.roundStatus();
        roundStatus.update(status, playerId);
        header.setGameName(game.gameNameOf(gameId));
        header.setTutorial(game.isTutorial(gameId));
        header.setEmpireName(observer ? "" : view.players().stream().filter(player -> player.id() == playerId)
                .map(de.zettsystems.starfare.game.values.Player::empireNameOrName).findFirst().orElse(""));
        header.updateTurnAction(status, playerId, observer, view.gameOver() || walkthroughOpen);
        showGameOverBanner(view);
        if (!observer) {
            header.updateEmpireStats(view);
        }
    }

    /** A tempo fixes the deadlines, so only the host of a game without one may change them. */
    private boolean hostMayChangeRoundRules() {
        return UserContext.currentPlayerId()
                .flatMap(account -> game.hostPlayerIdOf(gameId).filter(account::equals))
                .flatMap(account -> game.roundRulesFor(gameId, account))
                .filter(rules -> rules.tempo() == null).isPresent();
    }

    private void showGameOverBanner(PlayerViewState view) {
        boolean shown = view.gameOver() && reviewing;
        gameOverBanner.setVisible(shown);
        if (shown) {
            gameOverBanner.setText(I18n.t(UiTexts.MAP_GAME_OVER, winnerName(view)));
        }
    }

    private void openRoundRules() {
        RoundRulesDialog.open(game, gameId, this::refresh);
    }

    /**
     * Opens the outcome dialog once the walkthrough of the final round is done, its battles included. A final
     * round without own located events (a defeat decided elsewhere) opens it right away; otherwise the
     * player would stay in the waiting state with no way into the review.
     */
    private void showOutcomeWhenDue(PlayerViewState view) {
        if (reviewing || !view.gameOver() || outcomeDialogOpen) {
            return;
        }
        if (walkthroughOpen) {
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
                statistics, new GameOutcomeDialog.Actions(() -> {
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
        }));
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
        // A locked map still inspects systems and fleets, but offers no orders.
        boolean ordersOffered = !observer && !planningLocked;
        mapCanvas.render(new MapRenderer.Inputs(
                game, gameId, view, playerId, !ordersOffered, selectedFrom, targetSelection == TargetSelection.RELOCATION,
                highlightedFleetId, highlightedStandingOrderId, reportedSystemIds(view), highlightedReportSystemId,
                pendingBattleSystemIds(view), game.isTutorial(gameId),
                badgesShowingFleetNo, systems,
                this::inspectSystem,
                this::selectSource,
                this::openSend,
                this::openRelocation,
                this::onBadgeToggleLabel,
                this::onFleetHighlight,
                this::onReportMarkerSelected,
                this::refresh,
                rangeForSelection(playerId, observer),
                jointArrival(view, playerId, !ordersOffered),
                this::toggleJointSource,
                this::openJointAttack));
        fleetsPanel.update(gameId, view, selectedSystem, highlightedFleetId, highlightedStandingOrderId, observer,
                targetSelection != TargetSelection.NONE);
        if (mobile != null) { mobile.targetSelection(targetSelection != TargetSelection.NONE); }
    }

    /**
     * Arrival badges for the selected fleet's destination, or for a selected system that is not the player's own;
     * the routes are asked once per target and round.
     */
    private @Nullable JointArrival jointArrival(PlayerViewState view, int playerId, boolean observer) {
        if (observer || playerId < 0 || view.gameOver() || targetSelection != TargetSelection.NONE) { return null; }
        Integer fleet = highlightedFleetId;
        VisibleSystem system = selectedSystem;
        JointArrival.Aim aim = null;
        if (fleet != null) {
            aim = JointArrival.forFleet(view, fleet);
        } else if (system != null && !Objects.equals(system.ownerId(), playerId)) {
            aim = JointArrival.forSystem(view, system.id());
        }
        if (aim == null) { return null; }
        String key = aim.targetId() + "/" + view.turn() + "/" + playerId;
        if (!key.equals(arrivalKey)) {
            arrivalTurns = game.arrivalTurnsTo(gameId, playerId, aim.targetId());
            arrivalKey = key;
            jointSources.clear();
        }
        JointArrival joint = JointArrival.of(view, aim, arrivalTurns, !view.roundStatus().hasSubmitted(playerId), jointSources);
        jointSources.retainAll(joint == null ? Set.of() : joint.selected());
        return joint;
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
        if (planningLocked) { return; }
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
        if (planningLocked) { return; }
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
                .map(RoundWalkthrough::siteOf)
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
        if (reviewing || planningLocked) { return; }
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
        if (reviewing || planningLocked) {
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
