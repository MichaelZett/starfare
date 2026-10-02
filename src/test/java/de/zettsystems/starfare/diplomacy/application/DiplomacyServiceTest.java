package de.zettsystems.starfare.diplomacy.application;

import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.testsupport.NavigationStates;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DiplomacyServiceTest {
    private final DefaultDiplomacyService service = new DefaultDiplomacyService();
    private static DiplomacyOrder order(DiplomacyOrder.Action action, int subject, int notice) { return new DiplomacyOrder(action, subject, notice); }
    private void found(GameState state, int notice) {
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.FOUND, 2, notice))).isTrue();
        assertThat(service.act(state, 2, order(DiplomacyOrder.Action.APPROVE, state.diplomacy().proposals().getFirst().id(), 0))).isTrue();
    }
    private static GameState threePlayers() {
        var state = NavigationStates.running(); state.players().add(new Player(3, "Third", false, "#009e73")); return state;
    }
    @Test void foundingRequiresConsentAndRejectingDoesNotCreateMembership() {
        var state = threePlayers();
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.FOUND, 2, 3))).isTrue();
        assertThat(state.allied(1, 2)).isFalse(); var proposal = state.diplomacy().proposals().getFirst();
        var before = GameState.toSnapshot(state);
        assertThat(service.act(state, 3, order(DiplomacyOrder.Action.APPROVE, proposal.id(), 0))).isFalse();
        assertThat(service.act(state, 2, order(DiplomacyOrder.Action.FOUND, 3, 0))).isFalse();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(service.act(state, 2, order(DiplomacyOrder.Action.REJECT, proposal.id(), 0))).isTrue();
        assertThat(state.diplomacy().groups()).isEmpty(); assertThat(state.diplomacy().proposals()).isEmpty();
        found(state, 3); assertThat(state.allied(1, 2)).isTrue(); assertThat(state.allied(1, 3)).isFalse();
        assertThat(state.allied(1, null)).isFalse();
    }
    @Test void admissionRequiresEveryExistingMemberAndPreservesOneGroupPerEmpire() {
        var state = threePlayers(); found(state, 2); int group = state.diplomacy().groups().getFirst().id();
        assertThat(service.act(state, 3, order(DiplomacyOrder.Action.JOIN, group, 0))).isTrue();
        int proposal = state.diplomacy().proposals().getFirst().id();
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.APPROVE, proposal, 0))).isTrue();
        assertThat(state.allied(1, 3)).isFalse();
        assertThat(service.act(state, 2, order(DiplomacyOrder.Action.APPROVE, proposal, 0))).isTrue();
        assertThat(state.diplomacy().groups().getFirst().members()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(state.allied(2, 3)).isTrue();
        assertThat(service.act(state, 3, order(DiplomacyOrder.Action.FOUND, 1, 1))).isFalse();
        assertThat(service.act(state, 3, order(DiplomacyOrder.Action.JOIN, group, 1))).isFalse();
    }
    @Test void agreedNoticeNeverChangesAnAlreadyAnnouncedDeparture() {
        var state = threePlayers(); found(state, 3);
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.LEAVE, 0, 0))).isTrue();
        assertThat(state.diplomacy().groups().getFirst().departures()).containsEntry(1, 4);
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.NOTICE, 0, 20))).isTrue();
        int proposal = state.diplomacy().proposals().getFirst().id();
        assertThat(service.act(state, 2, order(DiplomacyOrder.Action.APPROVE, proposal, 0))).isTrue();
        assertThat(state.diplomacy().groups().getFirst().noticeRounds()).isEqualTo(20);
        assertThat(state.diplomacy().groups().getFirst().departures()).containsEntry(1, 4);
        service.beginRound(state, 3); assertThat(state.allied(1, 2)).isTrue();
        service.beginRound(state, 4); assertThat(state.diplomacy().groups()).isEmpty();
        assertThat(state.gameOver()).isFalse();
    }
    @Test void zeroNoticeWaitsUntilTheNextRoundAndStaleElectoratesAreDiscarded() {
        var state = threePlayers(); found(state, 0);
        int group = state.diplomacy().groups().getFirst().id();
        assertThat(service.act(state, 3, order(DiplomacyOrder.Action.JOIN, group, 0))).isTrue();
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.LEAVE, 0, 0))).isTrue();
        assertThat(state.diplomacy().groups().getFirst().departures()).containsEntry(1, 2);
        service.beginRound(state, 1); assertThat(state.allied(1, 2)).isTrue();
        service.beginRound(state, 2); assertThat(state.allied(1, 2)).isFalse();
        assertThat(state.diplomacy().proposals()).isEmpty();
    }
    @Test void aiAnswersInvitationsWithoutForcingHumanMembership() {
        var state = NavigationStates.running(); state.players().set(1, state.players().get(1).asAi());
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.FOUND, 2, 3))).isTrue();
        service.answerAiProposals(state); assertThat(state.allied(1, 2)).isTrue();
        assertThat(service.act(state, 2, order(DiplomacyOrder.Action.LEAVE, 0, 0))).isTrue();
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.NOTICE, 0, 2))).isTrue();
        service.answerAiProposals(state); assertThat(state.diplomacy().proposals()).isEmpty();
        assertThat(state.diplomacy().groups().getFirst().noticeRounds()).isEqualTo(3);
    }
    @Test void classicAndInvalidTreatyValuesAreRejectedWithoutMutation() {
        var classic = new GameState(); classic.start(); var before = GameState.toSnapshot(classic);
        assertThat(service.act(classic, 1, order(DiplomacyOrder.Action.FOUND, 2, 3))).isFalse();
        service.beginRound(classic, 10); assertThat(GameState.toSnapshot(classic)).isEqualTo(before);
        assertThatThrownBy(() -> new AllianceGroup(1, Set.of(1), 1, Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AllianceGroup(1, Set.of(1, 2), -1, Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiplomacyState(3, List.of(new AllianceGroup(1, Set.of(1, 2), 1, Map.of()), new AllianceGroup(2, Set.of(2, 3), 1, Map.of())), List.of())).isInstanceOf(IllegalArgumentException.class);
        var state = NavigationStates.running();
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.FOUND, 99, 3))).isFalse();
        assertThat(service.act(state, 1, order(DiplomacyOrder.Action.FOUND, 2, 51))).isFalse();
        assertThat(state.diplomacy()).isEqualTo(DiplomacyState.EMPTY);
    }
}
