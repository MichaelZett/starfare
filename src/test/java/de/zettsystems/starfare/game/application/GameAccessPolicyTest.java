package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameAccessPolicyTest {
    private final GameAccessPolicy policy = new DefaultGameAccessPolicy();
    private final GameState state = new GameState();

    @Test
    void privateGameIsLimitedToHostSeatsAndInvitations() {
        state.seatByUser().put("member", 1);
        state.invitedSeats().put("invited", 2);
        state.observers().add("outsider");
        assertThat(policy.visible(state, "host", "host")).isTrue();
        assertThat(policy.visible(state, "host", "member")).isTrue();
        assertThat(policy.visible(state, "host", "invited")).isTrue();
        assertThat(policy.visible(state, "host", "outsider")).isFalse();
        assertThat(policy.visible(state, "host", "")).isFalse();
    }

    @Test
    void invitationDoesNotRevealMapAndWithdrawalRemovesVisibility() {
        state.configureLobby(true, true);
        state.invitedSeats().put("invited", 2);
        assertThat(policy.canObserve(state, "host", "invited")).isFalse();
        state.invitedSeats().clear();
        assertThat(policy.visible(state, "host", "invited")).isFalse();
    }

    @Test
    void publicGameAllowsVisibilityButRespectsObserverSetting() {
        state.publishInLobby();
        state.configureLobby(false, false);
        assertThat(policy.visible(state, "host", "outsider")).isTrue();
        assertThat(policy.canObserve(state, "host", "outsider")).isFalse();
        state.configureLobby(true, false);
        assertThat(policy.canObserve(state, "host", "outsider")).isTrue();
    }

    @Test
    void completedPrivateGameExcludesInvitationsButPreservesFormerMembers() {
        state.seatByUser().put("former", 1);
        state.invitedSeats().put("invited", 2);
        state.endGame(1);
        assertThat(policy.canReview(state, "host", "former")).isTrue();
        assertThat(policy.canReview(state, "host", "host")).isTrue();
        assertThat(policy.visible(state, "host", "invited")).isFalse();
        assertThat(policy.canObserve(state, "host", "host")).isFalse();
    }

    @Test
    void publicArchiveRequiresObserverPermissionForOutsiders() {
        state.publishInLobby();
        state.configureLobby(false, false);
        state.endGame(null);
        assertThat(policy.visible(state, null, "outsider")).isTrue();
        assertThat(policy.canReview(state, null, "outsider")).isFalse();
        state.configureLobby(true, false);
        assertThat(policy.canReview(state, null, "outsider")).isTrue();
    }

    @Test
    void summaryOfAnActiveGameDecidesLikeItsState() {
        state.seatByUser().put("member", 1);
        state.invitedSeats().put("invited", 2);
        assertSameDecision();
        state.publishInLobby();
        assertSameDecision();
        state.makePrivate();
        state.endGame(1);
        assertSameDecision();
    }

    private void assertSameDecision() {
        var summary = GameSession.summaryOf(GameId.of("g"), "Game", "host", state);
        for (String account : List.of("host", "member", "invited", "outsider", "")) {
            assertThat(policy.visible(summary, account)).as(account).isEqualTo(policy.visible(state, "host", account));
        }
    }

    @Test
    void outcomeSurvivesCopyAndRepeatedEnd() {
        state.endGame(1);
        var finished = state.finishedAt();
        state.endGame(1);
        assertThat(state.finishedAt()).isEqualTo(finished);
        assertThat(GameState.copyOf(state).finishedAt()).isEqualTo(finished);
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(state)).finishedAt()).isEqualTo(finished);
        state.resetForNewGame();
        assertThat(state.finishedAt()).isNull();
    }

    @Test
    void whoLeftReadsTheChatButNeitherWritesNorCommands() {
        state.seatByUser().put("player", 1);
        state.seatByUser().put("former", 2);
        state.joinedHumanPlayerIds().add(1);
        state.observers().add("watcher");

        assertThat(policy.mayWriteChat(state, "player")).as("waiting room").isTrue();
        assertThat(policy.mayCommand(state, "player")).as("not started").isFalse();
        state.start();
        assertThat(policy.mayCommand(state, "player")).isTrue();
        assertThat(policy.mayReadChat(state, "former")).isTrue();
        assertThat(policy.mayWriteChat(state, "former")).isFalse();
        assertThat(policy.mayCommand(state, "former")).isFalse();
        assertThat(policy.mayReadChat(state, "watcher")).isFalse();
        assertThat(policy.mayWriteChat(state, "watcher")).isFalse();
        assertThat(policy.mayReadChat(state, "")).isFalse();
    }

    @Test
    void afterTheEndTheChatCanOnlyBeRead() {
        state.seatByUser().put("player", 1);
        state.joinedHumanPlayerIds().add(1);
        state.start();

        state.endGame(1);

        assertThat(policy.mayReadChat(state, "player")).isTrue();
        assertThat(policy.mayWriteChat(state, "player")).isFalse();
        assertThat(policy.mayCommand(state, "player")).isFalse();
    }
}
