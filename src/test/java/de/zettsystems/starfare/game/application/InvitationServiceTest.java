package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.InvitationSent;
import de.zettsystems.starfare.testsupport.TestPlatformSocial;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Players are account ids as text: the platform answers who may see whom only for accounts (plan M13). */
@RecordApplicationEvents
class InvitationServiceTest extends AbstractIntegrationTest {
    private static final String HOST = "910001";
    private static final String BOB = "910002";
    private static final String EVE = "910003";
    private static final String ALICE = "910004";
    private static final String MALLORY = "910005";

    @Autowired
    private GameService games;

    @Autowired
    private InvitationService invitations;

    @Autowired
    private Broadcaster broadcaster;

    @Autowired
    private TestPlatformSocial social;

    @Autowired
    private PlayerDirectory players;

    @Autowired
    private ApplicationEvents platformEvents;

    private final List<GameEvent> events = new ArrayList<>();
    private Subscription subscription;

    @BeforeEach
    void listen() {
        subscription = broadcaster.subscribeAll(events::add);
    }

    @AfterEach
    void stopListening() {
        subscription.remove();
        social.reset();
    }

    private GameId newTwoHumanGame(String host) {
        GameSetup setup = GameSetup.defaults().seatPlayers(2, 0).chooseGalaxy(20, GalaxyLayout.EVEN);
        return games.newGame(setup, host, "test-game");
    }

    @Test
    void hostInvitesInviteeReservesSeatAndPublishesEvent() {
        GameId id = newTwoHumanGame(HOST);
        games.joinGame(id, HOST);

        Optional<Integer> seat = invitations.inviteUser(id, HOST, BOB);

        assertThat(seat).isPresent();
        assertThat(games.invitedSeatsOf(id)).containsEntry(BOB, seat.get());
        assertThat(events).contains(new GameEvent.InvitationChanged(id));
        assertThat(platformEvents.stream(InvitationSent.class)).singleElement().satisfies(sent -> {
            assertThat(sent.matchId()).isEqualTo(id.value());
            assertThat(sent.invited()).isEqualTo(AccountId.of(Long.parseLong(BOB)));
            assertThat(sent.invitedBy()).isEqualTo(AccountId.of(Long.parseLong(HOST)));
        });
    }

    @Test
    void anInviteeWhoHidesFromTheHostCannotBeInvited() {
        GameId id = newTwoHumanGame(HOST);
        social.hide(AccountId.of(Long.parseLong(BOB)), AccountId.of(Long.parseLong(HOST)));

        assertThat(invitations.inviteUser(id, HOST, BOB)).isEmpty();
        social.reset();
        social.block(AccountId.of(Long.parseLong(BOB)), AccountId.of(Long.parseLong(HOST)));
        assertThat(invitations.inviteUser(id, HOST, BOB)).isEmpty();
        assertThat(invitations.inviteUser(id, HOST, "no-account")).as("only accounts").isEmpty();
        assertThat(games.invitedSeatsOf(id)).isEmpty();
    }

    @Test
    void nonHostCannotInvite() {
        GameId id = newTwoHumanGame(HOST);

        assertThat(invitations.inviteUser(id, BOB, EVE)).isEmpty();
        assertThat(games.invitedSeatsOf(id)).isEmpty();
    }

    @Test
    void cannotInviteSelf() {
        GameId id = newTwoHumanGame(HOST);

        assertThat(invitations.inviteUser(id, HOST, HOST)).isEmpty();
    }

    @Test
    void reservedSeatIsNotClaimableByOthers() {
        GameId id = newTwoHumanGame(HOST);
        games.joinGame(id, HOST);
        invitations.inviteUser(id, HOST, BOB);

        Optional<Integer> intruder = games.joinGame(id, EVE);

        assertThat(intruder).isEmpty();
    }

    @Test
    void canStartGameBlocksOnOpenInvite() {
        GameId id = newTwoHumanGame(HOST);
        games.joinGame(id, HOST);
        invitations.inviteUser(id, HOST, BOB);

        assertThat(games.canStartGame(id)).isFalse();
    }

    @Test
    void inviteeCanAcceptAndClaimReservedSeat() {
        GameId id = newTwoHumanGame(HOST);
        Optional<Integer> hostSeat = games.joinGame(id, HOST);
        Optional<Integer> invitedSeat = invitations.inviteUser(id, HOST, BOB);

        boolean accepted = invitations.acceptInvite(id, BOB);

        assertThat(accepted).isTrue();
        assertThat(games.seatFor(id, BOB)).isEqualTo(invitedSeat);
        assertThat(games.seatFor(id, BOB).get()).isNotEqualTo(hostSeat.get());
        assertThat(games.invitedSeatsOf(id)).isEmpty();
        assertThat(games.summaryOf(id).players().stream().filter(player -> player.id() == invitedSeat.orElseThrow())
                .findFirst().orElseThrow().label()).as("named like a seat taken through the dialog")
                .contains(players.displayName(BOB) + " Empire");
        assertThat(games.canStartGame(id)).isTrue();
        assertThat(events).contains(new GameEvent.PlayerJoined(id, invitedSeat.orElseThrow()));
    }

    @Test
    void acceptWithoutInviteIsRejected() {
        GameId id = newTwoHumanGame(HOST);
        games.joinGame(id, HOST);

        assertThat(invitations.acceptInvite(id, BOB)).isFalse();
    }

    @Test
    void hostRevokesFreesSeat() {
        GameId id = newTwoHumanGame(HOST);
        games.changeVisibility(id, HOST, GameVisibility.PUBLIC);
        games.joinGame(id, HOST);
        invitations.inviteUser(id, HOST, BOB);

        assertThat(invitations.revokeInvite(id, HOST, BOB)).isTrue();

        assertThat(games.invitedSeatsOf(id)).isEmpty();
        assertThat(games.joinGame(id, EVE)).isPresent();
        assertThat(events).contains(new GameEvent.InvitationChanged(id));
    }

    @Test
    void nonHostCannotRevoke() {
        GameId id = newTwoHumanGame(HOST);
        invitations.inviteUser(id, HOST, BOB);

        assertThat(invitations.revokeInvite(id, MALLORY, BOB)).isFalse();
        assertThat(games.invitedSeatsOf(id)).hasSize(1);
    }

    @Test
    void inviteeDeclinesFreesSeat() {
        GameId id = newTwoHumanGame(HOST);
        invitations.inviteUser(id, HOST, BOB);

        assertThat(invitations.declineInvite(id, BOB)).isTrue();

        assertThat(games.invitedSeatsOf(id)).isEmpty();
        assertThat(events).contains(new GameEvent.InvitationChanged(id));
    }

    @Test
    void cannotInviteWhenAllHumanSeatsTaken() {
        GameId id = newTwoHumanGame(HOST);
        games.joinGame(id, HOST);
        invitations.inviteUser(id, HOST, ALICE);
        games.joinGame(id, ALICE);

        assertThat(invitations.inviteUser(id, HOST, BOB)).isEmpty();
    }

    @Test
    void cannotInviteAfterGameStarted() {
        GameSetup setup = GameSetup.defaults().seatPlayers(1, 0).chooseGalaxy(20, GalaxyLayout.EVEN);
        GameId id = games.newGame(setup, HOST, "t");
        games.joinGame(id, HOST);
        games.startGame(id);

        assertThat(invitations.inviteUser(id, HOST, BOB)).isEmpty();
    }
}
