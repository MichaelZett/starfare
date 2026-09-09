package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import org.junit.jupiter.api.Test;

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
}
