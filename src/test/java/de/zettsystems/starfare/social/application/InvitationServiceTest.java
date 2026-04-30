package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.social.values.SocialEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private GameService games;

    @Autowired
    private InvitationService invitations;

    @Autowired
    private SocialBroadcaster broadcaster;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserPreferencesRepository preferencesRepository;

    private List<SocialEvent> events;

    @BeforeEach
    void cleanSocial() {
        friendshipRepository.deleteAll();
        preferencesRepository.deleteAll();
        events = new ArrayList<>();
        broadcaster.subscribe(events::add);
    }

    private GameId newTwoHumanGame(String host) {
        GameSetup setup = new GameSetup(
                20, 2, 0,
                List.of(5, 5),
                2, 5, 6, GameConfig.PLAYER_PALETTE.getFirst(),
                false, false);
        return games.newGame(setup, host, "test-game");
    }

    @Test
    void hostInvitesInviteeReservesSeatAndPublishesEvent() {
        GameId id = newTwoHumanGame("host");
        games.joinGame(id, "host");

        Optional<Integer> seat = invitations.inviteUser(id, "host", "bob");

        assertThat(seat).isPresent();
        assertThat(games.invitedSeatsOf(id)).containsEntry("bob", seat.get());
        assertThat(events.stream().anyMatch(e ->
                e instanceof SocialEvent.InviteReceived inv
                        && "bob".equals(inv.to())
                        && inv.seatId() == seat.get())).isTrue();
    }

    @Test
    void nonHostCannotInvite() {
        GameId id = newTwoHumanGame("host");

        assertThat(invitations.inviteUser(id, "bob", "eve")).isEmpty();
        assertThat(games.invitedSeatsOf(id)).isEmpty();
    }

    @Test
    void cannotInviteSelf() {
        GameId id = newTwoHumanGame("host");

        assertThat(invitations.inviteUser(id, "host", "host")).isEmpty();
    }

    @Test
    void reservedSeatIsNotClaimableByOthers() {
        GameId id = newTwoHumanGame("host");
        games.joinGame(id, "host");
        invitations.inviteUser(id, "host", "bob");

        Optional<Integer> intruder = games.joinGame(id, "eve");

        assertThat(intruder).isEmpty();
    }

    @Test
    void canStartGameBlocksOnOpenInvite() {
        GameId id = newTwoHumanGame("host");
        games.joinGame(id, "host");
        invitations.inviteUser(id, "host", "bob");

        assertThat(games.canStartGame(id)).isFalse();
    }

    @Test
    void inviteeCanAcceptAndClaimReservedSeat() {
        GameId id = newTwoHumanGame("host");
        Optional<Integer> hostSeat = games.joinGame(id, "host");
        Optional<Integer> invitedSeat = invitations.inviteUser(id, "host", "bob");

        boolean accepted = invitations.acceptInvite(id, "bob");

        assertThat(accepted).isTrue();
        assertThat(games.seatFor(id, "bob")).isEqualTo(invitedSeat);
        assertThat(games.seatFor(id, "bob").get()).isNotEqualTo(hostSeat.get());
        assertThat(games.invitedSeatsOf(id)).isEmpty();
        assertThat(games.canStartGame(id)).isTrue();
        assertThat(events.stream().filter(SocialEvent.InviteAccepted.class::isInstance).findFirst().orElseThrow()).isInstanceOf(SocialEvent.InviteAccepted.class);
    }

    @Test
    void acceptWithoutInviteIsRejected() {
        GameId id = newTwoHumanGame("host");
        games.joinGame(id, "host");

        assertThat(invitations.acceptInvite(id, "bob")).isFalse();
    }

    @Test
    void hostRevokesFreesSeat() {
        GameId id = newTwoHumanGame("host");
        games.joinGame(id, "host");
        invitations.inviteUser(id, "host", "bob");

        assertThat(invitations.revokeInvite(id, "host", "bob")).isTrue();

        assertThat(games.invitedSeatsOf(id)).isEmpty();
        assertThat(games.joinGame(id, "eve")).isPresent();
        assertThat(events.stream().anyMatch(SocialEvent.InviteWithdrawn.class::isInstance)).isTrue();
    }

    @Test
    void nonHostCannotRevoke() {
        GameId id = newTwoHumanGame("host");
        invitations.inviteUser(id, "host", "bob");

        assertThat(invitations.revokeInvite(id, "mallory", "bob")).isFalse();
        assertThat(games.invitedSeatsOf(id)).hasSize(1);
    }

    @Test
    void inviteeDeclinesFreesSeat() {
        GameId id = newTwoHumanGame("host");
        invitations.inviteUser(id, "host", "bob");

        assertThat(invitations.declineInvite(id, "bob")).isTrue();

        assertThat(games.invitedSeatsOf(id)).isEmpty();
        assertThat(events.stream().anyMatch(SocialEvent.InviteDeclined.class::isInstance)).isTrue();
    }

    @Test
    void cannotInviteWhenAllHumanSeatsTaken() {
        GameId id = newTwoHumanGame("host");
        games.joinGame(id, "host");
        games.joinGame(id, "alice");

        assertThat(invitations.inviteUser(id, "host", "bob")).isEmpty();
    }

    @Test
    void cannotInviteAfterGameStarted() {
        GameSetup setup = new GameSetup(
                20, 1, 0,
                List.of(5),
                2, 5, 6, GameConfig.PLAYER_PALETTE.getFirst(),
                false, false);
        GameId id = games.newGame(setup, "host", "t");
        games.joinGame(id, "host");
        games.startGame(id);

        assertThat(invitations.inviteUser(id, "host", "bob")).isEmpty();
    }
}
