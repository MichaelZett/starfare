package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.economy.values.ProductionAllocation;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Commands from the wrong account, for the wrong round or with impossible values change nothing. */
class GameServiceRejectedInputTest extends AbstractIntegrationTest {
    private static final String HOST = "host";
    private static final String GUEST = "guest";
    private static final String STRANGER = "stranger";
    private static final GameSetup TWO_HUMANS = GameSetup.defaults().seatPlayers(2, 0).chooseGalaxy(20, GalaxyLayout.EVEN);
    private static final GameSetup HUMAN_AND_COMPUTER = GameSetup.defaults().seatPlayers(1, 1).chooseGalaxy(20, GalaxyLayout.EVEN);

    @Autowired
    private GameService games;

    @Test
    void economyAndDiplomacyCommandsNeedARunningGame() {
        GameId lobby = games.newGame(GameSetup.defaults(), HOST, "Lobby");
        assertThat(games.joinGame(lobby, HOST)).isPresent();

        for (GameId id : List.of(lobby, GameId.newId())) {
            assertThat(games.commitProduction(id, HOST, new ProductionAllocation(1, 1, 0)).accepted()).isFalse();
            assertThat(games.allocateExpansion(id, 1, 1, 1, 0)).isFalse();
            assertThat(games.diplomacyOrder(id, HOST, 1, new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2))).isFalse();
            assertThat(games.sendFleet(id, 1, 1, 1, 2, 1, 1)).isFalse();
        }
        assertThat(registry.readState(lobby, GameState::started)).isFalse();
    }

    @Test
    void impossibleValuesAndForeignSystemsAreRejected() {
        GameId id = runningSoloGame();
        int seat = seat(id, HOST);
        int own = ownSystem(id, seat);
        int foreign = foreignSystem(id, seat);
        GameStateSnapshot before = snapshot(id);

        assertThat(games.sendFleet(id, seat, foreign, own, 1)).as("from a foreign system").isFalse();
        assertThat(games.sendFleet(id, seat, 9_999, own, 1)).as("from an unknown system").isFalse();
        assertThat(games.removeStandingOrder(id, seat, 1)).as("no relocation yet").isFalse();
        assertThat(games.removeStandingOrderFrom(id, seat, own)).as("no relocation yet").isFalse();
        assertThat(snapshot(id)).isEqualTo(before);
    }

    @Test
    void onlyTheSeatedPlayerMayPlanAndOnlyForTheCurrentRound() {
        GameId id = runningSoloGame();
        int seat = seat(id, HOST);
        int own = ownSystem(id, seat);
        int target = foreignSystem(id, seat);
        PlannedOrder planned = new PlannedOrder(0, "send", own, target, "From", "To", 1, false, null, null, null);
        GameStateSnapshot before = snapshot(id);

        assertThat(games.commitProduction(id, STRANGER, new ProductionAllocation(own, 1, 0)).accepted()).isFalse();
        assertThat(games.commitProduction(id, HOST, new ProductionAllocation(own, 1, 0)).accepted())
                .as("without the expected plan").isFalse();
        assertThat(games.diplomacyOrder(id, STRANGER, 1, new DiplomacyOrder(DiplomacyOrder.Action.FOUND, seat))).isFalse();

        int staleRound = 2;
        assertThat(games.sendFleet(id, seat, staleRound, own, target, 1, seat)).isFalse();
        assertThat(games.editOrder(id, seat, staleRound, planned, target, 1)).isFalse();
        assertThat(games.undoOrders(id, seat, staleRound)).isFalse();
        assertThat(games.planJointAttack(id, seat, staleRound, target, List.of())).isFalse();
        assertThat(games.allocateExpansion(id, seat, staleRound, own, 0)).isFalse();
        assertThat(snapshot(id)).isEqualTo(before);
    }

    @Test
    void startingNeedsTheHostOfAKnownGameInItsLobby() {
        GameId id = games.newGame(TWO_HUMANS, HOST, "Start");
        assertThat(games.joinGame(id, HOST)).isPresent();
        assertThat(games.inviteUser(id, GUEST)).isPresent();
        assertThat(games.joinGame(id, GUEST)).isPresent();

        assertThat(games.startGame(id, null)).isFalse();
        assertThat(games.startGame(id, " ")).isFalse();
        assertThat(games.startGame(GameId.newId(), HOST)).isFalse();
        assertThat(games.canStartGame(id, GUEST)).isFalse();
        assertThat(games.startGame(id, GUEST)).isFalse();
        assertThat(registry.readState(id, GameState::started)).isFalse();

        assertThat(games.startGame(id, HOST)).isTrue();
        assertThat(games.startGame(id, HOST)).as("already running").isFalse();
        assertThat(games.canStartGame(id)).isFalse();
    }

    @Test
    void invitationsNeedAnAccountAndAFreeSeatBeforeTheStart() {
        GameId id = games.newGame(TWO_HUMANS, HOST, "Invite");
        assertThat(games.joinGame(id, HOST)).isPresent();

        assertThat(games.inviteUser(id, null)).isEmpty();
        assertThat(games.inviteUser(id, " ")).isEmpty();
        assertThat(games.inviteUser(id, HOST)).as("already seated").isEmpty();
        assertThat(games.inviteUser(id, GUEST)).isPresent();
        assertThat(games.inviteUser(id, GUEST)).as("already invited").isEmpty();
        assertThat(games.inviteUser(id, STRANGER)).as("the last seat is reserved").isEmpty();
        assertThat(games.revokeInvite(id, null)).isEmpty();
        assertThat(games.seatReservedFor(id, null)).isEmpty();
        assertThat(games.invitedSeatsOf(id)).containsOnlyKeys(GUEST);

        assertThat(games.joinGame(id, GUEST)).isPresent();
        assertThat(games.startGame(id, HOST)).isTrue();
        assertThat(games.inviteUser(id, STRANGER)).as("after the start").isEmpty();
    }

    @Test
    void withoutAnAccountNobodyIsSeatedOrWatching() {
        GameId id = runningSoloGame();

        assertThat(games.joinGame(id, (String) null)).isEmpty();
        assertThat(games.joinGame(id, " ")).isEmpty();
        assertThat(games.joinGame(GameId.newId(), GUEST)).isEmpty();
        assertThat(games.seatFor(id, null)).isEmpty();
        assertThat(games.observeGame(id, null)).isFalse();
        assertThat(games.isObserver(id, null)).isFalse();
        assertThat(games.leaveObserve(id, null)).isFalse();
        assertThat(games.advanceForObserver(id, null)).isFalse();
        Set<String> seated = registry.readState(id, state -> Set.copyOf(state.seatByUser().keySet()));
        assertThat(seated).containsExactly(HOST);
    }

    @Test
    void seatsOfTheComputerOrOfAFinishedGameCannotBeTaken() {
        GameId id = games.newGame(HUMAN_AND_COMPUTER, HOST, "Seats");
        int computer = registry.readState(id, state -> state.players().stream().filter(Player::ai)
                .map(Player::id).findFirst().orElseThrow());
        assertThat(games.joinGame(id, computer)).as("the computer's seat").isFalse();
        assertThat(games.joinGame(id, HOST)).isPresent();
        assertThat(games.startGame(id, HOST)).isTrue();
        int seat = seat(id, HOST);
        registry.writeState(id, state -> { state.endGame(seat); return null; });

        assertThat(games.joinGame(id, HOST)).isEmpty();
        assertThat(games.joinGame(id, seat)).isFalse();
        assertThat(games.canStartGame(id)).isFalse();
        assertThat(games.startGame(id)).isFalse();
    }

    private GameId runningSoloGame() {
        GameId id = games.newGame(GameSetup.defaults(), HOST, "Solo");
        assertThat(games.joinGame(id, HOST)).isPresent();
        assertThat(games.startGame(id, HOST)).isTrue();
        return id;
    }

    private int seat(GameId id, String account) {
        return games.seatFor(id, account).orElseThrow();
    }

    private int ownSystem(GameId id, int seat) {
        return registry.readState(id, state -> state.systems().stream()
                .filter(system -> Objects.equals(system.ownerId(), seat)).findFirst().orElseThrow().id());
    }

    private int foreignSystem(GameId id, int seat) {
        return registry.readState(id, state -> state.systems().stream()
                .filter(system -> !Objects.equals(system.ownerId(), seat)).map(StarSystem::id).findFirst().orElseThrow());
    }

    private GameStateSnapshot snapshot(GameId id) {
        return registry.readState(id, GameState::toSnapshot);
    }
}
