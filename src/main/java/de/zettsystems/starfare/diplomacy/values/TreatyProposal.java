package de.zettsystems.starfare.diplomacy.values;

import java.util.HashSet;
import java.util.Set;

public record TreatyProposal(int id, Kind kind, int groupId, int candidate, int noticeRounds,
                             Set<Integer> voters, Set<Integer> approvals) {
    public enum Kind { FOUND, JOIN, NOTICE }
    public TreatyProposal {
        java.util.Objects.requireNonNull(kind);
        voters = Set.copyOf(voters); approvals = Set.copyOf(approvals);
        if (id < 1 || groupId < 0 || candidate < 1 || noticeRounds < 0 || noticeRounds > 50
                || voters.size() < 2 || !voters.containsAll(approvals)
                || (kind == Kind.FOUND && (groupId != 0 || voters.size() != 2))
                || (kind != Kind.FOUND && groupId == 0)) {
            throw new IllegalArgumentException("Invalid treaty proposal");
        }
    }
    public TreatyProposal approve(int player) {
        var updated = new HashSet<>(approvals); updated.add(player);
        return new TreatyProposal(id, kind, groupId, candidate, noticeRounds, voters, updated);
    }
    public boolean unanimous() { return approvals.containsAll(voters); }
}
