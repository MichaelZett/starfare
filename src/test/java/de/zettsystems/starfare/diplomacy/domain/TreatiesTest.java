package de.zettsystems.starfare.diplomacy.domain;

import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.diplomacy.values.TreatyProposal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TreatiesTest {
    private static final Set<Integer> PLAYERS = Set.of(1, 2, 3, 4, 5, 6);

    private static DiplomacyOrder order(DiplomacyOrder.Action action, int subject) {
        return new DiplomacyOrder(action, subject);
    }
    private static DiplomacyState twoGroups() {
        return new DiplomacyState(3, List.of(new AllianceGroup(1, Set.of(1, 2), 2, Map.of()),
                new AllianceGroup(2, Set.of(3, 4), 2, Map.of())), List.of());
    }

    @Test void everyNewAllianceGetsTheFixedNoticePeriod() {
        var proposed = Treaties.apply(DiplomacyState.EMPTY, 1, order(DiplomacyOrder.Action.FOUND, 2), 1, PLAYERS).orElseThrow();
        assertThat(proposed.proposals().getFirst().noticeRounds()).isEqualTo(AllianceGroup.NOTICE_ROUNDS);
        var founded = Treaties.apply(proposed, 2, order(DiplomacyOrder.Action.APPROVE, proposed.proposals().getFirst().id()), 1, PLAYERS).orElseThrow();
        assertThat(founded.groups()).singleElement().satisfies(group -> {
            assertThat(group.noticeRounds()).isEqualTo(3);
            assertThat(group.departureTurn(5)).isEqualTo(8);
        });
    }

    @Test void joinAsksExactlyTheMembersOfTheNamedGroup() {
        var joined = Treaties.apply(twoGroups(), 5, order(DiplomacyOrder.Action.JOIN, 2), 1, PLAYERS).orElseThrow();
        assertThat(joined.proposals()).singleElement().satisfies(proposal -> {
            assertThat(proposal.groupId()).isEqualTo(2);
            assertThat(proposal.voters()).containsExactlyInAnyOrder(3, 4, 5);
        });
        assertThat(Treaties.apply(twoGroups(), 5, order(DiplomacyOrder.Action.JOIN, 9), 1, PLAYERS)).isEmpty();
    }

    @Test void pendingProposalsOnlyBlockTheirOwnVoters() {
        var pending = Treaties.apply(DiplomacyState.EMPTY, 1, order(DiplomacyOrder.Action.FOUND, 2), 1, PLAYERS).orElseThrow();
        assertThat(Treaties.apply(pending, 3, order(DiplomacyOrder.Action.FOUND, 4), 1, PLAYERS)).hasValueSatisfying(
                state -> assertThat(state.proposals()).hasSize(2));
        assertThat(Treaties.apply(pending, 3, order(DiplomacyOrder.Action.FOUND, 2), 1, PLAYERS)).isEmpty();
    }

    @Test void votesNeedAnExistingProposalAndCountOncePerVoter() {
        var join = Treaties.apply(twoGroups(), 5, order(DiplomacyOrder.Action.JOIN, 2), 1, PLAYERS).orElseThrow();
        int proposal = join.proposals().getFirst().id();
        assertThat(Treaties.apply(join, 3, order(DiplomacyOrder.Action.APPROVE, proposal + 1), 1, PLAYERS)).isEmpty();
        assertThat(Treaties.apply(join, 5, order(DiplomacyOrder.Action.APPROVE, proposal), 1, PLAYERS)).isEmpty();
        var once = Treaties.apply(join, 3, order(DiplomacyOrder.Action.APPROVE, proposal), 1, PLAYERS).orElseThrow();
        assertThat(once.proposals().getFirst().approvals()).containsExactlyInAnyOrder(3, 5);
        assertThat(Treaties.apply(once, 3, order(DiplomacyOrder.Action.APPROVE, proposal), 1, PLAYERS)).isEmpty();
    }

    @Test void newRoundKeepsProposalsWhoseElectorateDidNotChange() {
        var groups = List.of(new AllianceGroup(1, Set.of(1, 2), 2, Map.of()), new AllianceGroup(2, Set.of(3, 4), 2, Map.of(3, 5)));
        var join = new TreatyProposal(3, TreatyProposal.Kind.JOIN, 1, 5, 3, Set.of(1, 2, 5), Set.of(5));
        var state = new DiplomacyState(4, groups, List.of(join));
        assertThat(Treaties.beginRound(state, 4)).isEqualTo(state);
        var departed = Treaties.beginRound(state, 5);
        assertThat(departed.groups()).extracting(AllianceGroup::id).containsExactly(1);
        assertThat(departed.proposals()).containsExactly(join);
    }

    @Test void aSavedNoticeChangeChangesNothingAndLapsesAtTheNextRound() {
        var group = new AllianceGroup(1, Set.of(1, 2), 2, Map.of());
        var notice = new TreatyProposal(2, TreatyProposal.Kind.NOTICE, 1, 1, 9, Set.of(1, 2), Set.of(1));
        var state = new DiplomacyState(3, List.of(group), List.of(notice));
        var approved = Treaties.apply(state, 2, order(DiplomacyOrder.Action.APPROVE, 2), 1, PLAYERS).orElseThrow();
        assertThat(approved.groups()).containsExactly(group);
        assertThat(approved.proposals()).isEmpty();
        assertThat(Treaties.beginRound(state, 2).proposals()).isEmpty();
    }
}
