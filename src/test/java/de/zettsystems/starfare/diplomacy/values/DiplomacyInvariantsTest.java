package de.zettsystems.starfare.diplomacy.values;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiplomacyInvariantsTest {
    private static final AllianceGroup FIRST = new AllianceGroup(1, Set.of(1, 2), 2, Map.of());
    private static final AllianceGroup SECOND = new AllianceGroup(2, Set.of(3, 4), 2, Map.of());

    private static TreatyProposal proposal(int id, TreatyProposal.Kind kind, int group, int candidate, Set<Integer> voters) {
        return new TreatyProposal(id, kind, group, candidate, 1, voters, Set.of(candidate));
    }

    @Test void allianceGroupBoundsIncludeTheirLimits() {
        assertThatCode(() -> new AllianceGroup(1, Set.of(1, 2), 0, Map.of(1, 1))).doesNotThrowAnyException();
        assertThatCode(() -> new AllianceGroup(1, Set.of(1, 2), 50, Map.of())).doesNotThrowAnyException();
        Set<Integer> members = Set.of(1, 2);
        Map<Integer, Integer> none = Map.of();
        Map<Integer, Integer> turnZero = Map.of(1, 0);
        Map<Integer, Integer> stranger = Map.of(3, 4);
        assertThatThrownBy(() -> new AllianceGroup(0, members, 1, none)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AllianceGroup(1, members, 51, none)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AllianceGroup(1, members, 1, turnZero)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AllianceGroup(1, members, 1, stranger)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void treatyProposalBoundsIncludeTheirLimits() {
        assertThatCode(() -> new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 1, 0, Set.of(1, 2), Set.of(1))).doesNotThrowAnyException();
        assertThatCode(() -> new TreatyProposal(1, TreatyProposal.Kind.JOIN, 1, 1, 50, Set.of(1, 2, 3), Set.of())).doesNotThrowAnyException();
        Set<Integer> voters = Set.of(1, 2);
        Set<Integer> alone = Set.of(1);
        Set<Integer> approvals = Set.of(1);
        assertThatThrownBy(() -> new TreatyProposal(0, TreatyProposal.Kind.FOUND, 0, 1, 1, voters, approvals)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 0, 1, voters, approvals)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 1, -1, voters, approvals)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 1, 51, voters, approvals)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TreatyProposal(1, TreatyProposal.Kind.JOIN, -1, 1, 1, voters, approvals)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 1, 1, alone, approvals)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void idsMustStayBelowNextId() {
        var groups = List.of(FIRST, SECOND);
        List<TreatyProposal> none = List.of();
        assertThatCode(() -> new DiplomacyState(3, groups, none)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new DiplomacyState(2, groups, none)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void oneEmpireCannotVoteOnTwoProposals() {
        var proposals = List.of(proposal(1, TreatyProposal.Kind.FOUND, 0, 1, Set.of(1, 2)),
                proposal(2, TreatyProposal.Kind.FOUND, 0, 3, Set.of(2, 3)));
        List<AllianceGroup> none = List.of();
        assertThatThrownBy(() -> new DiplomacyState(3, none, proposals))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Overlapping votes");
    }

    @Test void foundingProposalsMustNotIncludeAlliedEmpires() {
        var groups = List.of(FIRST);
        var valid = List.of(proposal(2, TreatyProposal.Kind.FOUND, 0, 3, Set.of(3, 4)));
        var allied = List.of(proposal(2, TreatyProposal.Kind.FOUND, 0, 3, Set.of(2, 3)));
        assertThatCode(() -> new DiplomacyState(3, groups, valid)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new DiplomacyState(3, groups, allied))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid pending consent");
    }

    @Test void joinProposalsNeedTheNamedGroupsMembersAndAnUnalliedCandidate() {
        var groups = List.of(FIRST, SECOND);
        var valid = List.of(proposal(3, TreatyProposal.Kind.JOIN, 2, 5, Set.of(3, 4, 5)));
        var alliedCandidate = List.of(proposal(3, TreatyProposal.Kind.JOIN, 2, 1, Set.of(3, 4, 1)));
        var wrongVoters = List.of(proposal(3, TreatyProposal.Kind.JOIN, 2, 5, Set.of(3, 5)));
        assertThatCode(() -> new DiplomacyState(4, groups, valid)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new DiplomacyState(4, groups, alliedCandidate))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid pending consent");
        assertThatThrownBy(() -> new DiplomacyState(4, groups, wrongVoters))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid pending consent");
    }
}
