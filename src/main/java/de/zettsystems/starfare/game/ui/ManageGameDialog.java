package de.zettsystems.starfare.game.ui;

import de.zettsystems.gamesplatformapi.PlatformFriends;
import de.zettsystems.gamesplatformapi.PlatformPresence;
import de.zettsystems.starfare.game.platform.StarfareMatches;
import java.util.Comparator;
import java.util.stream.Stream;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.game.application.InvitationService;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Host-only dialog for managing a lobby game: invite friends and visible online players, revoke
 * pending invites, and kick joined humans (seats turn into AI). Who is online and who are friends
 * comes from the platform (plan M13), in Starfare's own start from Starfare itself. Refreshes on game
 * events.
 */
public class ManageGameDialog extends Dialog {

    private final Span ruleset = new Span();
    private final Checkbox publicGame = new Checkbox();
    private final GameService games;
    private final InvitationService invitations;
    private final PlatformPresence presence;
    private final PlatformFriends friends;
    private final Broadcaster gameBroadcaster;
    private final PlayerDirectory players;
    private final GameId gameId;
    private final String host;

    private final Div humansList = new Div();
    private final Div invitesList = new Div();
    private final ComboBox<String> inviteCombo = new ComboBox<>();
    private @Nullable Subscription gameSubscription;

    @SuppressWarnings("java:S107")
    public ManageGameDialog(GameId gameId, String host, GameService games, InvitationService invitations,
                            PlatformPresence presence, PlatformFriends friends, Broadcaster gameBroadcaster,
                            PlayerDirectory players) {
        this.gameId = gameId;
        this.host = host;
        this.games = games;
        this.invitations = invitations;
        this.presence = presence;
        this.friends = friends;
        this.gameBroadcaster = gameBroadcaster;
        this.players = players;

        addClassName("manage-dialog");
        setHeaderTitle(I18n.t(UiTexts.MANAGE_TITLE));
        setWidth("520px");

        H3 humansHeader = new H3(I18n.t(UiTexts.MANAGE_PLAYERS_HEADER));
        humansHeader.addClassName("manage-section-header");
        humansList.addClassName("manage-list");

        H3 invitesHeader = new H3(I18n.t(UiTexts.MANAGE_INVITES_HEADER));
        invitesHeader.addClassName("manage-section-header");
        invitesList.addClassName("manage-list");

        inviteCombo.setLabel(I18n.t(UiTexts.MANAGE_INVITE_DROPDOWN));
        inviteCombo.setPlaceholder(I18n.t(UiTexts.MANAGE_INVITE_PLACEHOLDER));
        inviteCombo.setClearButtonVisible(true);
        inviteCombo.setItemLabelGenerator(players::displayName);
        Button inviteButton = new Button(I18n.t(UiTexts.MANAGE_INVITE_SEND), _ -> onInvite());
        inviteButton.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout inviteRow = new HorizontalLayout(inviteCombo, inviteButton);
        inviteRow.setAlignItems(FlexComponent.Alignment.END);
        inviteRow.addClassName("manage-invite-row");

        VerticalLayout body = new VerticalLayout(humansHeader, humansList, invitesHeader, invitesList, inviteRow);
        body.setPadding(false);
        body.setSpacing(true);
        publicGame.setLabel(I18n.t(UiTexts.GAME_PUBLIC));
        publicGame.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                games.changeVisibility(gameId, host, Boolean.TRUE.equals(event.getValue())
                        ? GameVisibility.PUBLIC
                        : GameVisibility.PRIVATE);
                refresh();
            }
        });
        add(ruleset, publicGame, body);

        Button close = new Button(I18n.t(UiTexts.MANAGE_CLOSE), _ -> close());
        getFooter().add(close);
        // A UI that goes away with the dialog still open must not stay subscribed.
        addDetachListener(_ -> unsubscribe());
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void open() {
        super.open();
        gameSubscription = gameBroadcaster.subscribeAll(_ -> getUI().ifPresent(ui -> ui.access(this::refresh)));
        refresh();
    }

    @Override
    public void close() {
        unsubscribe();
        super.close();
    }

    private void unsubscribe() {
        Subscription subscription = gameSubscription;
        gameSubscription = null;
        if (subscription != null) {
            subscription.remove();
        }
    }

    private void refresh() {
        GameSummary summary = games.summaryFor(gameId, host).orElse(null);
        if (summary == null || !host.equals(summary.hostPlayerId()) || summary.gameOver()) { close(); return; }
        ruleset.setText(RulesetLabels.label(games.rulesets(), summary.ruleset()));
        publicGame.setValue(summary.visibility() == GameVisibility.PUBLIC);
        publicGame.setEnabled(!summary.started());
        renderHumans(summary);
        renderInvites(summary.invitedSeats());
        updateInviteCandidates(summary);
    }

    private void renderHumans(GameSummary summary) {
        humansList.removeAll();
        List<HumanRow> rows = summary.players().stream()
                .filter(p -> !p.ai())
                .map(p -> new HumanRow(p.id(), p.label(), playerIdForSeat(summary, p.id()).orElse(null)))
                .toList();
        if (rows.isEmpty()) {
            Span empty = new Span(I18n.t(UiTexts.MANAGE_PLAYERS_EMPTY));
            empty.addClassName("manage-empty");
            humansList.add(empty);
            return;
        }
        for (HumanRow row : rows) {
            humansList.add(renderHumanRow(row));
        }
    }

    private Div renderHumanRow(HumanRow row) {
        Div container = new Div();
        container.addClassName("manage-row");
        String playerId = row.playerId();
        String displayName = playerId != null ? players.displayName(playerId) : row.seatName();
        Span label = new Span(displayName);
        label.addClassName("manage-row-label");
        container.add(label);

        Button kick = new Button(I18n.t(UiTexts.MANAGE_PLAYERS_KICK), _ -> {
            if (!games.kickHuman(gameId, host, row.seatId())) {
                Notification.show(I18n.t(UiTexts.MANAGE_PLAYERS_KICK_FAILED));
            }
        });
        kick.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL);
        boolean isSelf = playerId != null && playerId.equals(host);
        kick.setEnabled(!isSelf);
        container.add(kick);
        return container;
    }

    private void renderInvites(Map<String, Integer> invited) {
        invitesList.removeAll();
        if (invited.isEmpty()) {
            Span empty = new Span(I18n.t(UiTexts.MANAGE_INVITES_EMPTY));
            empty.addClassName("manage-empty");
            invitesList.add(empty);
            return;
        }
        invited.keySet().stream().sorted().forEach(inviteePlayerId -> invitesList.add(renderInviteRow(inviteePlayerId)));
    }

    private Div renderInviteRow(String inviteePlayerId) {
        Div row = new Div();
        row.addClassName("manage-row");
        Span label = new Span(players.displayName(inviteePlayerId));
        label.addClassName("manage-row-label");
        Button revoke = new Button(I18n.t(UiTexts.MANAGE_INVITES_REVOKE),
                _ -> invitations.revokeInvite(gameId, host, inviteePlayerId));
        revoke.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        row.add(label, revoke);
        return row;
    }

    private void updateInviteCandidates(GameSummary summary) {
        Set<String> seated = summary.seatByPlayer().keySet();
        Set<String> alreadyInvited = summary.invitedSeats().keySet();
        List<String> candidates = StarfareMatches.account(host).map(self -> Stream.concat(
                        presence.onlineFor(self).stream(), friends.friendsOf(self).stream()))
                .orElseGet(Stream::empty)
                .map(StarfareMatches::accountName)
                .distinct()
                .filter(u -> !u.equals(host))
                .filter(u -> !seated.contains(u))
                .filter(u -> !alreadyInvited.contains(u))
                .sorted(Comparator.comparing(players::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        String previous = inviteCombo.getValue();
        inviteCombo.setItems(candidates);
        if (previous != null && candidates.contains(previous)) {
            inviteCombo.setValue(previous);
        }
    }

    private void onInvite() {
        String inviteePlayerId = inviteCombo.getValue();
        if (inviteePlayerId == null) {
            Notification.show(I18n.t(UiTexts.MANAGE_INVITE_FAILED));
            return;
        }
        if (invitations.inviteUser(gameId, host, inviteePlayerId).isEmpty()) {
            Notification.show(I18n.t(UiTexts.MANAGE_INVITE_FAILED));
            return;
        }
        Notification.show(I18n.t(UiTexts.MANAGE_INVITE_SUCCESS, players.displayName(inviteePlayerId)));
        inviteCombo.clear();
    }

    private static Optional<String> playerIdForSeat(GameSummary summary, int seatId) {
        return summary.seatByPlayer().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() == seatId)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private record HumanRow(int seatId, String seatName, @Nullable String playerId) {
    }
}
