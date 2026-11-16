package de.zettsystems.starfare.game.ui;

import de.zettsystems.gamesplatformapi.PlatformFriends;
import de.zettsystems.gamesplatformapi.PlatformPresence;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameTemplateService;
import de.zettsystems.starfare.game.application.RulesetAccess;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.game.application.InvitationService;
import de.zettsystems.starfare.game.ui.ManageGameDialog;
import de.zettsystems.starfare.style.CssProperties;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * The waiting room of one game that has not started: who has a seat, who is invited, which seats
 * are free. People join here, the host invites, changes the visibility and starts. Once the game
 * runs, everyone with a seat moves on to the map.
 */
@Route(StarfareRoutes.MATCH)
@CssImport("./styles/starfare.css")
@PermitAll
public class WaitingRoomView extends VerticalLayout implements BeforeEnterObserver {
    private static final String GAME_ID_PARAMETER = "gameId";

    private final GameService game;
    private final GameTemplateService templates;
    private final RulesetAccess rulesetAccess;
    private final Broadcaster broadcaster;
    private final InvitationService invitations;
    private final PlatformPresence presence;
    private final PlatformFriends friends;
    private final PlayerDirectory players;
    private final H1 title = new H1();
    private final Span facts = new Span();
    private final Span status = new Span();
    private final Div seats = new Div();
    private final Div actions = new Div();
    private @Nullable GameId gameId;
    private @Nullable Subscription gameSubscription;

    @SuppressWarnings("java:S107") // Spring-Konstruktor: die Ansicht verbindet Spiel, Einladungen und Anzeige.
    public WaitingRoomView(GameService game, GameTemplateService templates, RulesetAccess rulesetAccess,
                           Broadcaster broadcaster, InvitationService invitations, PlatformPresence presence,
                           PlatformFriends friends, PlayerDirectory players) {
        this.game = game;
        this.templates = templates;
        this.rulesetAccess = rulesetAccess;
        this.broadcaster = broadcaster;
        this.invitations = invitations;
        this.presence = presence;
        this.friends = friends;
        this.players = players;
        addClassName("waiting-room");
        setPadding(true);
        Button back = new Button(I18n.t(UiTexts.WAITING_BACK), VaadinIcon.ARROW_LEFT.create(),
                _ -> navigate(LobbyView.class, RouteParameters.empty()));
        back.addThemeVariants(ButtonVariant.TERTIARY);
        back.setId("waiting-room-back");
        facts.addClassName("waiting-room-facts");
        status.addClassName("waiting-room-status");
        status.setId("waiting-room-status");
        seats.addClassName("waiting-room-seats");
        seats.setId("waiting-room-seats");
        actions.addClassName("waiting-room-actions");
        add(back, title, facts, status, seats, actions);
    }

    /** Route parameters of the waiting room of {@code id}. */
    public static RouteParameters parameters(GameId id) {
        return new RouteParameters(GAME_ID_PARAMETER, id.value());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get(GAME_ID_PARAMETER).orElse("");
        if (parameter.isBlank()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        GameId candidate = GameId.of(parameter);
        Optional<GameSummary> summary = game.summaryFor(candidate, account());
        if (summary.isEmpty()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        if (summary.get().started()) {
            // Running or finished: the map decides whether the account may see it.
            event.forwardTo(MainView.class, parameters(candidate));
            return;
        }
        this.gameId = candidate;
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    protected void onAttach(AttachEvent attachEvent) {
        if (gameId == null) {
            return;
        }
        UI ui = attachEvent.getUI();
        gameSubscription = broadcaster.subscribe(gameId, _ -> ui.access(this::refresh));
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (gameSubscription != null) {
            gameSubscription.remove();
            gameSubscription = null;
        }
    }

    private void refresh() {
        GameId id = gameId;
        if (id == null) {
            return;
        }
        String account = account();
        Optional<GameSummary> current = game.summaryFor(id, account);
        if (current.isEmpty()) {
            Notification.show(I18n.t(UiTexts.WAITING_GONE));
            navigate(LobbyView.class, RouteParameters.empty());
            return;
        }
        GameSummary summary = current.get();
        if (summary.started()) {
            boolean seated = summary.seatByPlayer().containsKey(account);
            navigate(seated ? MainView.class : LobbyView.class, seated ? parameters(id) : RouteParameters.empty());
            return;
        }
        show(summary, account);
    }

    private void show(GameSummary summary, String account) {
        title.setText(summary.name().isBlank() ? GameConfig.DEFAULT_GAME_NAME : summary.name());
        facts.setText(RulesetLabels.label(game.rulesets(), summary.ruleset()) + " · "
                + I18n.t(summary.visibility() == GameVisibility.PUBLIC ? UiTexts.GAME_PUBLIC : UiTexts.GAME_PRIVATE)
                + " · " + I18n.t(UiTexts.WAITING_HOST, hostName(summary)));
        status.setText(statusText(summary, account));
        seats.removeAll();
        Map<String, String> names = players.displayNames(summary.seatByPlayer().keySet());
        Map<String, String> invitedNames = players.displayNames(summary.invitedSeats().keySet());
        summary.players().forEach(player -> seats.add(seat(summary, player, names, invitedNames)));
        actions.removeAll();
        addActions(summary, account);
    }

    private String statusText(GameSummary summary, String account) {
        long humans = summary.players().stream().filter(player -> !player.ai()).count();
        if (!summary.allHumansJoined()) {
            return I18n.t(UiTexts.WAITING_STATUS_OPEN, summary.joinedHumanSeats().size(), humans);
        }
        return I18n.t(isHost(summary, account) ? UiTexts.WAITING_STATUS_READY_HOST : UiTexts.WAITING_STATUS_READY);
    }

    private static Component seat(GameSummary summary, Player player, Map<String, String> names,
                                  Map<String, String> invitedNames) {
        Span color = new Span();
        color.addClassName("waiting-room-seat-color");
        Span label = new Span(seatLabel(summary, player, names, invitedNames));
        label.addClassName("waiting-room-seat-label");
        Div row = new Div(color, label);
        row.addClassName("waiting-room-seat");
        row.addClassName(seatState(summary, player));
        row.getStyle().set(CssProperties.SEAT_COLOR, player.colorHex());
        return row;
    }

    private static String seatLabel(GameSummary summary, Player player, Map<String, String> names,
                                    Map<String, String> invitedNames) {
        if (player.ai()) {
            return I18n.t(UiTexts.WAITING_SEAT_AI, player.label());
        }
        if (summary.joinedHumanSeats().contains(player.id())) {
            String account = accountOn(summary.seatByPlayer(), player.id());
            String empire = player.empireNameOrName();
            return account == null ? player.label() : empire + " (" + names.getOrDefault(account, account) + ")";
        }
        String invitee = accountOn(summary.invitedSeats(), player.id());
        if (invitee != null) {
            return I18n.t(UiTexts.WAITING_SEAT_INVITED, invitedNames.getOrDefault(invitee, invitee));
        }
        return I18n.t(UiTexts.WAITING_SEAT_FREE);
    }

    private static String seatState(GameSummary summary, Player player) {
        if (player.ai()) {
            return "waiting-room-seat-ai";
        }
        if (summary.joinedHumanSeats().contains(player.id())) {
            return "waiting-room-seat-taken";
        }
        return summary.invitedSeats().containsValue(player.id()) ? "waiting-room-seat-invited" : "waiting-room-seat-free";
    }

    private static @Nullable String accountOn(Map<String, Integer> seats, int seat) {
        return seats.entrySet().stream().filter(entry -> entry.getValue() == seat)
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private void addActions(GameSummary summary, String account) {
        boolean host = isHost(summary, account);
        if (summary.invitedSeats().containsKey(account)) {
            actions.add(button(UiTexts.INVITATIONS_ACCEPT, "waiting-room-accept", ButtonVariant.PRIMARY,
                    () -> accept(summary)));
            actions.add(button(UiTexts.INVITATIONS_DECLINE, "waiting-room-decline", ButtonVariant.TERTIARY,
                    () -> decline(summary)));
        } else if (summary.canJoin(account)) {
            actions.add(button(UiTexts.LOBBY_ACTION_JOIN, "waiting-room-join", ButtonVariant.PRIMARY,
                    () -> JoinGameDialog.open(game, summary, account, players.displayName(account), this::refresh)));
        }
        if (host || (!hasHost(summary) && summary.seatByPlayer().containsKey(account))) {
            Button start = button(UiTexts.LOBBY_ACTION_START, "waiting-room-start", ButtonVariant.PRIMARY,
                    () -> start(summary));
            start.setEnabled(game.canStartGame(summary.gameId(), account));
            actions.add(start);
        }
        if (host) {
            actions.add(button(UiTexts.LOBBY_ACTION_MANAGE, "waiting-room-manage", ButtonVariant.TERTIARY,
                    () -> new ManageGameDialog(summary.gameId(), account, game, invitations, presence, friends,
                            broadcaster, players).open()));
        }
        if (summary.belongsTo(account) && rulesetAccess.mayUse(summary.ruleset(), account)) {
            actions.add(GameTemplateDialog.button(templates, summary.gameId(), summary.name(), this::refresh));
        }
        if (game.canAbort(summary.gameId(), account)) {
            actions.add(button(UiTexts.LOBBY_ACTION_ABORT, "waiting-room-abort", ButtonVariant.ERROR,
                    () -> abort(summary)));
        }
    }

    private void accept(GameSummary summary) {
        if (!invitations.acceptInvite(summary.gameId(), account())) {
            Notification.show(I18n.t(UiTexts.INVITATIONS_ACCEPT_FAILED));
        }
        refresh();
    }

    private void decline(GameSummary summary) {
        invitations.declineInvite(summary.gameId(), account());
        navigate(LobbyView.class, RouteParameters.empty());
    }

    private void start(GameSummary summary) {
        if (!game.startGame(summary.gameId(), account())) {
            Notification.show(I18n.t(UiTexts.LOBBY_START_FAILED));
        }
        refresh();
    }

    private void abort(GameSummary summary) {
        if (!game.abortGame(summary.gameId(), account())) {
            Notification.show(I18n.t(UiTexts.LOBBY_ABORT_DENIED));
            return;
        }
        navigate(LobbyView.class, RouteParameters.empty());
    }

    private String hostName(GameSummary summary) {
        String host = summary.hostPlayerId();
        return host == null || host.isBlank() ? "–" : players.displayName(host);
    }

    private static boolean hasHost(GameSummary summary) {
        String host = summary.hostPlayerId();
        return host != null && !host.isBlank();
    }

    private static boolean isHost(GameSummary summary, String account) {
        return !account.isBlank() && account.equals(summary.hostPlayerId());
    }

    private static Button button(String key, String id, ButtonVariant variant, Runnable action) {
        Button button = new Button(I18n.t(key), _ -> action.run());
        button.addThemeVariants(variant);
        button.setId(id);
        return button;
    }

    private void navigate(Class<? extends Component> target, RouteParameters parameters) {
        getUI().ifPresent(ui -> ui.navigate(target, parameters));
    }

    private static String account() {
        return UserContext.currentPlayerId().orElse("");
    }
}
