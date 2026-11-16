package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class GameVisibilityTest extends AbstractIntegrationTest {
    @Autowired private GameService games;

    @Test
    void privateCreationInvitationAndPublicationControlEntry() {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Private");
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PRIVATE);
        assertThat(games.summaryFor(id, "stranger")).isEmpty();
        assertThat(games.joinGame(id, "stranger")).isEmpty();
        assertThat(games.observeGame(id, "stranger")).isFalse();
        assertThat(games.viewForAccount(id, "stranger")).isEmpty();
        assertThat(games.inviteUser(id, "guest")).isPresent();
        assertThat(games.summaryFor(id, "guest")).isPresent();
        assertThat(games.viewForAccount(id, "guest")).isEmpty();
        assertThat(games.summaryOf(id).canJoin("guest")).isTrue();
        assertThat(games.summaryOf(id).canJoin("stranger")).isFalse();
        assertThat(games.revokeInvite(id, "guest")).isPresent();
        assertThat(games.summaryFor(id, "guest")).isEmpty();
        assertThat(games.changeVisibility(id, "stranger", GameVisibility.PUBLIC)).isFalse();
        assertThat(games.changeVisibility(id, "host", GameVisibility.PUBLIC)).isTrue();
        assertThat(games.visibleGamesFor("stranger", GameListScope.LOBBY)).hasSize(1);
        assertThat(games.joinGame(id, "stranger")).isPresent();
        assertThat(games.startGame(id)).isTrue();
        assertThat(games.changeVisibility(id, "host", GameVisibility.PRIVATE)).isFalse();
    }

    @Test
    void onlyTheHostStartsAMatchOnceEveryHumanSeatIsTaken() {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Host starts");
        assertThat(games.changeVisibility(id, "host", GameVisibility.PUBLIC)).isTrue();
        assertThat(games.canStartGame(id, "host")).as("seat still free").isFalse();
        assertThat(games.joinGame(id, "guest")).isPresent();

        assertThat(games.canStartGame(id, "guest")).isFalse();
        assertThat(games.startGame(id, "guest")).as("a seated guest").isFalse();
        assertThat(games.startGame(id, "stranger")).isFalse();
        assertThat(games.startGame(id, null)).isFalse();
        assertThat(games.hasStartedGame(id)).isFalse();

        assertThat(games.canStartGame(id, "host")).isTrue();
        assertThat(games.startGame(id, "host")).as("the host, even without a seat").isTrue();
        assertThat(games.hasStartedGame(id)).isTrue();
    }

    @Test
    void aGameWhoseHostHasGoneIsStartedByASeatedPerson() {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Hostless");
        assertThat(games.changeVisibility(id, "host", GameVisibility.PUBLIC)).isTrue();
        assertThat(games.joinGame(id, "guest")).isPresent();
        registry.require(id).transferHostTo(null);

        assertThat(games.startGame(id, "stranger")).isFalse();
        assertThat(games.startGame(id, "host")).as("no longer the host").isFalse();
        assertThat(games.startGame(id, "guest")).isTrue();
    }

    @Test
    void unknownGamesCannotBeStarted() {
        assertThat(games.canStartGame(GameId.newId(), "host")).isFalse();
        assertThat(games.startGame(GameId.newId(), "host")).isFalse();
    }

    @Test
    void makingPrivateRemovesForeignObservers() {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Observers");
        registry.writeState(id, state -> { state.configureLobby(true, true); return null; });
        assertThat(games.changeVisibility(id, "host", GameVisibility.PUBLIC)).isTrue();
        assertThat(games.observeGame(id, "stranger")).isTrue();
        assertThat(games.viewForAccount(id, "stranger")).isPresent();
        assertThat(games.changeVisibility(id, "host", GameVisibility.PRIVATE)).isTrue();
        assertThat(games.isObserver(id, "stranger")).isFalse();
        assertThat(games.viewForAccount(id, "stranger")).isEmpty();
    }

    @Test
    void unknownIdsBehaveLikeInaccessibleGames() {
        GameId id = GameId.newId();
        assertThat(games.summaryFor(id, "stranger")).isEmpty();
        assertThat(games.viewForAccount(id, "stranger")).isEmpty();
        assertThat(games.reviewFor(id, "stranger")).isEmpty();
        assertThat(games.changeVisibility(id, "host", GameVisibility.PUBLIC)).isFalse();
        assertThat(games.joinGame(id, "stranger")).isEmpty();
    }

    @Autowired private Broadcaster broadcaster;

    @Test
    void onlyAcceptedVisibilityChangesPublishEvents() {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Events");
        var events = new ArrayList<GameEvent>();
        var subscription = broadcaster.subscribe(id, events::add);
        try {
            assertThat(games.changeVisibility(id, "intruder", GameVisibility.PUBLIC)).isFalse();
            assertThat(events).isEmpty();
            assertThat(games.changeVisibility(id, "host", GameVisibility.PUBLIC)).isTrue();
            assertThat(events).containsExactly(new GameEvent.VisibilityChanged(id));
        } finally {
            subscription.remove();
        }
    }

    @Test
    void acceptedInvitationsAndWithdrawalsPublishEvents() {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Invitation events");
        var events = new ArrayList<GameEvent>();
        var subscription = broadcaster.subscribe(id, events::add);
        try {
            assertThat(games.inviteUser(id, "guest")).isPresent();
            assertThat(games.inviteUser(id, "guest")).isEmpty();
            assertThat(games.revokeInvite(id, "guest")).isPresent();
            assertThat(games.revokeInvite(id, "guest")).isEmpty();
            assertThat(events).containsExactly(new GameEvent.InvitationChanged(id), new GameEvent.InvitationChanged(id));
        } finally {
            subscription.remove();
        }
    }

    @Test
    void simultaneousClaimsCannotTakeTheSameSeat() throws Exception {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Concurrent");
        games.changeVisibility(id, "host", GameVisibility.PUBLIC);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return games.joinGame(id, "first"); });
            var second = executor.submit(() -> { start.await(); return games.joinGame(id, "second"); });
            start.countDown();
            var results = List.of(first.get(), second.get());
            assertThat(results.stream().filter(Optional::isPresent).count()).isEqualTo(1);
            assertThat(games.summaryOf(id).seatByPlayer()).hasSize(1);
        }
    }
}
