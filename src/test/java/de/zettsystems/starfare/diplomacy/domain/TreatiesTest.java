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

    private static DiplomacyOrder order(DiplomacyOrder.Action action, int subject, int notice) {
        return new DiplomacyOrder(action, subject, notice);
    }
    private static DiplomacyState twoGroups() {
        return new DiplomacyState(3, List.of(new AllianceGroup(1, Set.of(1, 2), 2, Map.of()),
                new AllianceGroup(2, Set.of(3, 4), 2, Map.of())), List.of());
    }

    @Test void noticeRoundsAreAcceptedFromZeroToFifty() {
        assertThat(Treaties.apply(DiplomacyState.EMPTY, 1, order(DiplomacyOrder.Action.FOUND, 2, 0), 1, PLAYERS)).isPresent();
        assertThat(Treaties.apply(DiplomacyState.EMPTY, 1, order(DiplomacyOrder.Action.FOUND, 2, 50), 1, PLAYERS)).isPresent();
        assertThat(Treaties.apply(DiplomacyState.EMPTY, 1, order(DiplomacyOrder.Action.FOUND, 2, -1), 1, PLAYERS)).isEmpty();
        assertThat(Treaties.apply(DiplomacyState.EMPTY, 1, order(DiplomacyOrder.Action.FOUND, 2, 51), 1, PLAYERS)).isEmpty();
    }

    @Test void joinAsksExactlyTheMembersOfTheNamedGroup() {
        var joined = Treaties.apply(twoGroups(), 5, order(DiplomacyOrder.Action.JOIN, 2, 0), 1, PLAYERS).orElseThrow();
        assertThat(joined.proposals()).singleElement().satisfies(proposal -> {
            assertThat(proposal.groupId()).isEqualTo(2);
            assertThat(proposal.voters()).containsExactlyInAnyOrder(3, 4, 5);
        });
        assertThat(Treaties.apply(twoGroups(), 5, order(DiplomacyOrder.Action.JOIN, 9, 0), 1, PLAYERS)).isEmpty();
    }

    @Test void pendingProposalsOnlyBlockTheirOwnVoters() {
        var pending = Treaties.apply(DiplomacyState.EMPTY, 1, order(DiplomacyOrder.Action.FOUND, 2, 1), 1, PLAYERS).orElseThrow();
        assertThat(Treaties.apply(pending, 3, order(DiplomacyOrder.Action.FOUND, 4, 1), 1, PLAYERS)).hasValueSatisfying(
                state -> assertThat(state.proposals()).hasSize(2));
        assertThat(Treaties.apply(pending, 3, order(DiplomacyOrder.Action.FOUND, 2, 1), 1, PLAYERS)).isEmpty();
    }

    @Test void votesNeedAnExistingProposalAndCountOncePerVoter() {
        var join = Treaties.apply(twoGroups(), 5, order(DiplomacyOrder.Action.JOIN, 2, 0), 1, PLAYERS).orElseThrow();
        int proposal = join.proposals().getFirst().id();
        assertThat(Treaties.apply(join, 3, order(DiplomacyOrder.Action.APPROVE, proposal + 1, 0), 1, PLAYERS)).isEmpty();
        assertThat(Treaties.apply(join, 5, order(DiplomacyOrder.Action.APPROVE, proposal, 0), 1, PLAYERS)).isEmpty();
        var once = Treaties.apply(join, 3, order(DiplomacyOrder.Action.APPROVE, proposal, 0), 1, PLAYERS).orElseThrow();
        assertThat(once.proposals().getFirst().approvals()).containsExactlyInAnyOrder(3, 5);
        assertThat(Treaties.apply(once, 3, order(DiplomacyOrder.Action.APPROVE, proposal, 0), 1, PLAYERS)).isEmpty();
    }

    @Test void newRoundKeepsProposalsWhoseElectorateDidNotChange() {
        var groups = List.of(new AllianceGroup(1, Set.of(1, 2), 2, Map.of()), new AllianceGroup(2, Set.of(3, 4), 2, Map.of(3, 5)));
        var notice = new TreatyProposal(3, TreatyProposal.Kind.NOTICE, 1, 1, 4, Set.of(1, 2), Set.of(1));
        var state = new DiplomacyState(4, groups, List.of(notice));
        assertThat(Treaties.beginRound(state, 4)).isEqualTo(state);
        var departed = Treaties.beginRound(state, 5);
        assertThat(departed.groups()).extracting(AllianceGroup::id).containsExactly(1);
        assertThat(departed.proposals()).containsExactly(notice);
    }
}
