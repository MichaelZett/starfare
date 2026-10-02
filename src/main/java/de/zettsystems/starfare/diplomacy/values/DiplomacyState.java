package de.zettsystems.starfare.diplomacy.values;

import java.util.List;
import java.util.Optional;

/** Immutable treaty state; never contains military intelligence. */
public record DiplomacyState(int nextId, List<AllianceGroup> groups, List<TreatyProposal> proposals) {
    public static final DiplomacyState EMPTY = new DiplomacyState(1, List.of(), List.of());
    public DiplomacyState {
        groups = List.copyOf(groups); proposals = List.copyOf(proposals);
        var members = groups.stream().flatMap(group -> group.members().stream()).toList();
        var ids = java.util.stream.Stream.concat(groups.stream().map(AllianceGroup::id), proposals.stream().map(TreatyProposal::id)).toList();
        if (nextId < 1 || members.stream().distinct().count() != members.size()
                || ids.stream().distinct().count() != ids.size() || ids.stream().anyMatch(id -> id >= nextId)) {
            throw new IllegalArgumentException("Invalid diplomacy state");
        }
        for (var proposal : proposals) {
            if (!validProposal(groups, proposal) || proposal.unanimous() || proposal.approvals().isEmpty()) { throw new IllegalArgumentException("Invalid pending consent"); }
        }
        var voters = proposals.stream().flatMap(proposal -> proposal.voters().stream()).toList();
        if (voters.stream().distinct().count() != voters.size()) { throw new IllegalArgumentException("Overlapping votes"); }
    }
    private static boolean validProposal(List<AllianceGroup> groups, TreatyProposal proposal) {
        if (proposal.kind() == TreatyProposal.Kind.FOUND) {
            return proposal.voters().contains(proposal.candidate()) && groups.stream().noneMatch(group -> group.members().stream().anyMatch(proposal.voters()::contains));
        }
        var group = groups.stream().filter(candidate -> candidate.id() == proposal.groupId()).findFirst();
        if (group.isEmpty()) { return false; }
        var voters = new java.util.HashSet<>(group.orElseThrow().members());
        if (proposal.kind() == TreatyProposal.Kind.JOIN) {
            if (groups.stream().anyMatch(candidate -> candidate.members().contains(proposal.candidate()))) { return false; }
            voters.add(proposal.candidate());
        } else if (!voters.contains(proposal.candidate())) { return false; }
        return voters.equals(proposal.voters());
    }
    public Optional<AllianceGroup> groupFor(int player) { return groups.stream().filter(group -> group.members().contains(player)).findFirst(); }
    public boolean allied(int left, int right) { return left != right && groupFor(left).map(group -> group.members().contains(right)).orElse(false); }
}
