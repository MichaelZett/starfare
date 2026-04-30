package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Primary UI view showing map and fleet controls. Route is /map/{gameId}.
 * Fleet commands are issued directly on the map: click own system to select source,
 * click target to open the send dialog; right-click a fleet line for Wait/Disband.
 */
@Route("map/:gameId")
@CssImport("./styles/starfare.css")
@PermitAll
public class MainView extends VerticalLayout implements BeforeEnterObserver {
    private final GameService game;
    private final Broadcaster broadcaster;
    private @Nullable Subscription broadcasterSubscription;
    private final MapCanvas mapCanvas;
    private final MapHeaderBar header;
    private final Div gameOverBanner = new Div();
    private final FleetAndOrdersPanel fleetsPanel;

    @SuppressWarnings("NullAway.Init")
    private GameId gameId;
    private @Nullable VisibleSystem selectedFrom;
    private @Nullable Integer highlightedFleetId;
    private final Set<Integer> badgesShowingFleetNo = new HashSet<>();

    @Autowired
    public MainView(GameService service, Broadcaster broadcaster) {
        this.game = service;
        this.broadcaster = broadcaster;
        setWidthFull();
        setHeightFull();
        setSpacing(false);
        setPadding(false);
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        addClassName("map-root");

        fleetsPanel = new FleetAndOrdersPanel(this::refresh, this::cancelOrder, this::openStandingOrdersDialog,
                this::onFleetRowSelected);
        mapCanvas = new MapCanvas(this::onMapBackgroundClick);
        header = new MapHeaderBar(this::onNextRound, this::doLeave,
                () -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)));

        gameOverBanner.setVisible(false);
        gameOverBanner.getStyle().set(CssProperties.FONT_WEIGHT, "600");

        add(header, buildContent());
    }

    private HorizontalLayout buildContent() {
        var left = new VerticalLayout();
        left.setPadding(false);
        left.setSpacing(true);
        left.setWidth("75%");
        left.setHeightFull();
        left.addClassName("map-left");
        left.add(gameOverBanner, mapCanvas);
        left.setFlexGrow(1, mapCanvas);

        var content = new HorizontalLayout(left, fleetsPanel);
        content.setWidthFull();
        content.setHeightFull();
        content.setSpacing(true);
        content.addClassName("map-content");
        content.setFlexGrow(1, left);
        content.setFlexGrow(0, fleetsPanel);
        return content;
    }

    private void onNextRound() {
        String username = UserContext.currentUsername().orElse(null);
        if (isObserver()) {
            if (!game.advanceForObserver(gameId, username)) {
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
        if (game.hasSignificantEventsFor(gameId, pid)) {
            getUI().ifPresent(ui -> ui.navigate(RoundView.class,
                    new RouteParameters("gameId", gameId.value())));
        } else {
            refresh();
        }
    }

    private void onMapBackgroundClick() {
        if (selectedFrom != null || highlightedFleetId != null) {
            selectedFrom = null;
            highlightedFleetId = null;
            refresh();
        }
    }

    private void onFleetRowSelected(int fleetId) {
        Integer next = fleetId < 0 ? null : fleetId;
        if (Objects.equals(highlightedFleetId, next)) {
            return;
        }
        highlightedFleetId = next;
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

    private void cancelOrder(PlannedOrder o) {
        int pid = currentSeat();
        boolean ok;
        if (o.standing() && o.standingOrderId() != null) {
            ok = pid >= 0 && game.removeStandingOrder(gameId, pid, o.standingOrderId());
        } else {
            ok = pid >= 0 && game.cancelOrder(gameId, pid, o.index());
        }
        if (!ok) {
            Notification.show(I18n.t(UiTexts.MAP_CANCEL_ORDER_FAILED));
        }
        refresh();
    }

    private void openStandingOrdersDialog() {
        int pid = currentSeat();
        if (pid >= 0) {
            StandingOrdersDialog.open(game, gameId, pid, this::refresh);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("gameId").orElse(null);
        if (parameter == null || parameter.isBlank()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        GameId candidate = GameId.of(parameter);
        if (!game.hasActiveGame(candidate)) {
            event.forwardTo(LobbyView.class);
            return;
        }
        this.gameId = candidate;
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
        centerOnHomeSystem();
    }

    private void centerOnHomeSystem() {
        int pid = currentSeat();
        if (pid < 0) {
            return;
        }
        PlayerViewState view = game.viewFor(gameId, pid);
        VisibleSystem home = view.systems().stream()
                .filter(s -> s.ownerId() != null && s.ownerId() == pid)
                .findFirst().orElse(null);
        if (home == null) {
            return;
        }
        mapCanvas.scrollTo(home.x(), home.y());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterSubscription != null) {
            broadcasterSubscription.remove();
            broadcasterSubscription = null;
        }
    }

    private void doLeave() {
        String username = UserContext.currentUsername().orElse(null);
        if (isObserver()) {
            game.leaveObserve(gameId, username);
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
        String username = UserContext.currentUsername().orElse(null);
        return username == null ? -1 : game.seatFor(gameId, username).orElse(-1);
    }

    private boolean isObserver() {
        String username = UserContext.currentUsername().orElse(null);
        return username != null && game.isObserver(gameId, username);
    }

    private void refresh() {
        boolean observer = isObserver();
        int playerId = observer ? -1 : currentSeat();
        if (!observer && playerId < 0) {
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
            return;
        }
        PlayerViewState view = observer ? game.viewForObserver(gameId) : game.viewFor(gameId, playerId);
        boolean aiOnly = game.isAiOnly(gameId);

        fleetsPanel.setGridsVisible(!observer);
        header.setNextVisible(!observer || aiOnly);
        header.setLeaveText(I18n.t(observer ? UiTexts.MAP_ACTION_LEAVE_OBSERVE : UiTexts.MAP_ACTION_LEAVE));
        header.setEmpireStatsVisible(!observer);
        header.setRound(view.turn());
        header.setGameName(game.gameNameOf(gameId));

        if (view.gameOver()) {
            gameOverBanner.setText(I18n.t(UiTexts.MAP_GAME_OVER, winnerName(view)));
            gameOverBanner.setVisible(true);
            header.setNextEnabled(false);
        } else {
            gameOverBanner.setVisible(false);
            header.setNextEnabled(true);
        }

        List<VisibleSystem> systems = view.systems().stream()
                .sorted(Comparator.comparingInt(VisibleSystem::id)).toList();
        GameState gs = observer ? null : game.snapshot(gameId);

        if (!observer && gs != null) {
            header.updateEmpireStats(view, gs, playerId);
        }

        // Drop fleet-specific UI state for fleets that no longer exist (arrived, disbanded, …).
        Set<Integer> liveFleetIds = view.ownFleets().stream()
                .map(Fleet::globalId).collect(Collectors.toSet());
        badgesShowingFleetNo.retainAll(liveFleetIds);
        if (highlightedFleetId != null && !liveFleetIds.contains(highlightedFleetId)) {
            highlightedFleetId = null;
        }

        mapCanvas.render(new MapRenderer.Inputs(
                game, gameId, view, gs, playerId, observer, selectedFrom,
                highlightedFleetId, badgesShowingFleetNo, systems,
                sel -> {
                    selectedFrom = sel;
                    refresh();
                },
                this::openSend,
                this::onBadgeToggleLabel,
                this::onFleetHighlight,
                this::refresh));

        fleetsPanel.update(view);
        fleetsPanel.setHighlightedFleet(highlightedFleetId);
    }

    private void openSend(VisibleSystem from, VisibleSystem to) {
        int pid = currentSeat();
        if (pid < 0) {
            return;
        }
        SendFleetDialog.open(game, gameId, pid, from, to, () -> {
            selectedFrom = null;
            refresh();
        });
    }

    private String winnerName(PlayerViewState view) {
        Integer winnerId = view.winnerId();
        if (winnerId == null) {
            return "?";
        }
        return view.players().stream()
                .filter(p -> p.id() == winnerId)
                .map(Player::name)
                .findFirst()
                .orElse("?");
    }
}
