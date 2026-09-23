package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
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

    @SuppressWarnings("NullAway.Init")
    private GameId gameId;
    private boolean reviewing;
    private @Nullable VisibleSystem selectedSystem;
    private @Nullable VisibleSystem selectedFrom;
    private @Nullable Integer highlightedFleetId;
    private @Nullable Integer highlightedStandingOrderId;
    private @Nullable Integer highlightedReportSystemId;
    private @Nullable Integer displayedTurn;
    /** Remember the pending outcome even when the view was reloaded during the final battles. */
    private boolean witnessedRunningGame;
    private boolean outcomeAcknowledged;
    private boolean outcomeDialogOpen;
    private final Set<Integer> badgesShowingFleetNo = new HashSet<>();
    private final BattleAcknowledgements acknowledgements;

    @Autowired
    public MainView(GameService service, Broadcaster broadcaster, BattleAcknowledgementService battleAcknowledgements) {
        this.game = service;
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
                this::setGarrisonReserve);
        mapCanvas = new MapCanvas(gameId == null ? "none" : gameId.value(), this::onMapBackgroundClick);
        header = new MapHeaderBar(this::onNextRound, this::doLeave,
                () -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)));

        gameOverBanner.setVisible(false);
        gameOverBanner.getStyle().set(CssProperties.FONT_WEIGHT, "600");

        reviewControls = new ReviewControls(() -> {
            selectedSystem = null;
            selectedFrom = null;
            highlightedFleetId = null;
            highlightedReportSystemId = null;
            badgesShowingFleetNo.clear();
            refresh();
        });
        spectatorControls = new SpectatorControls(() -> {
            selectedSystem = null;
            selectedFrom = null;
            highlightedFleetId = null;
            highlightedReportSystemId = null;
            badgesShowingFleetNo.clear();
            refresh();
        });
        add(header, roundStatus, spectatorControls, reviewControls, buildContent());
    }

    private SplitLayout buildContent() {
        var left = new VerticalLayout();
        left.setPadding(false);
        left.setSpacing(true);
        left.setWidthFull();
        left.setHeightFull();
        left.addClassName("map-left");
        left.add(gameOverBanner, mapCanvas);
        left.setFlexGrow(1, mapCanvas);

        var content = new SplitLayout(left, fleetsPanel);
        content.setWidthFull();
        content.setHeightFull();
        content.setSplitterPosition(70);
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
        if (game.isWaitingForOtherPlayers(gameId, pid)) {
            Notification.show(I18n.t(UiTexts.MAP_SUBMIT_WAITING));
            refresh();
            return;
        }
        refresh();
    }

    private void onMapBackgroundClick() {
        if (selectedFrom != null || highlightedFleetId != null || highlightedStandingOrderId != null) {
            selectedFrom = null;
            highlightedFleetId = null;
            highlightedStandingOrderId = null;
            refresh();
        }
    }

    private void onFleetRowSelected(int fleetId) {
        Integer next = fleetId < 0 ? null : fleetId;
        if (Objects.equals(highlightedFleetId, next)) {
            if (next != null) {
                fleetsPanel.showDetails();
            }
            return;
        }
        highlightedFleetId = next;
        selectedFrom = null;
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
        displayedTurn = null;
        witnessedRunningGame = false;
        outcomeAcknowledged = false;
        outcomeDialogOpen = false;
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
        broadcasterSubscription = broadcaster.subscribe(gameId, _ -> ui.access(this::refresh));
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
        configureHeader(view, observer);
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
        List<Integer> replayTurns = game.replayTurns(gameId, UserContext.currentPlayerId().orElse(""));
        reviewControls.show(view.players(), currentSeat(), replayTurns);
        int replayTurn = reviewControls.replayTurn();
        if (replayTurn >= 0) {
            return game.replayFor(gameId, UserContext.currentPlayerId().orElse(""),
                    reviewControls.perspective(), replayTurn).orElse(null);
        }
        return game.reviewFor(gameId, UserContext.currentPlayerId().orElse(""),
                reviewControls.perspective(), reviewControls.fogOfWar()).orElse(null);
    }

    private void configureHeader(PlayerViewState view, boolean observer) {
        reviewControls.setVisible(reviewing);
        fleetsPanel.setViewsVisible(!observer || reviewing);
        fleetsPanel.setReportAvailable(currentSeat() >= 0);
        header.setNextVisible(!reviewing && (!observer || game.isAiOnly(gameId)));
        header.setLeaveVisible(!reviewing);
        header.setLeaveText(I18n.t(observer ? UiTexts.MAP_ACTION_LEAVE_OBSERVE : UiTexts.MAP_ACTION_LEAVE));
        header.setEmpireStatsVisible(!observer && pendingBattleSystemIds(view).isEmpty());
        header.setRound(view.turn());
        roundStatus.update(reviewing ? RoundStatus.NONE : view.roundStatus());
        header.setGameName(game.gameNameOf(gameId));
        header.setNextEnabled(!view.gameOver());
        gameOverBanner.setVisible(view.gameOver() && reviewing);
        if (view.gameOver() && reviewing) {
            gameOverBanner.setText(I18n.t(UiTexts.MAP_GAME_OVER, winnerName(view)));
        }
        if (!observer) {
            header.updateEmpireStats(view);
        }
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
        GameOutcomeDialog.open(Objects.equals(view.winnerId(), currentSeat()), winnerName(view), statistics, () -> {
            outcomeDialogOpen = false;
            outcomeAcknowledged = true;
            refresh();
        });
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
                game, gameId, view, playerId, observer, selectedFrom,
                highlightedFleetId, highlightedStandingOrderId, reportedSystemIds(view), highlightedReportSystemId,
                pendingBattleSystemIds(view),
                badgesShowingFleetNo, systems,
                this::inspectSystem,
                this::selectSource,
                this::openSend,
                this::openRelocation,
                this::onBadgeToggleLabel,
                this::onFleetHighlight,
                this::onReportMarkerSelected,
                this::refresh));
        fleetsPanel.update(gameId, view, selectedSystem, highlightedFleetId, highlightedStandingOrderId, observer);
    }

    private void inspectSystem(VisibleSystem system) {
        selectedSystem = system;
        selectedFrom = null;
        highlightedFleetId = null;
        fleetsPanel.showDetails();
        refresh();
    }

    private void selectSource(VisibleSystem system) {
        selectedFrom = selectedFrom != null && selectedFrom.id() == system.id() ? null : system;
        selectedSystem = system;
        highlightedFleetId = null;
        fleetsPanel.showDetails();
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
            selectedFrom = null;
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
            selectedFrom = null;
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
