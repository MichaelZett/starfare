package de.zettsystems.starfare.diplomacy.domain;

import de.zettsystems.starfare.diplomacy.values.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Pure unanimous voting; stale proposals are discarded whenever the electorate changes. */
public final class Treaties {
    private Treaties() { }
    public static Optional<DiplomacyState> apply(DiplomacyState state, int player, DiplomacyOrder order, int turn, Set<Integer> players) {
        if (!players.contains(player) || order.noticeRounds() < 0 || order.noticeRounds() > 50) { return Optional.empty(); }
        return switch (order.action()) {
            case FOUND, JOIN, NOTICE -> propose(state, player, order, players);
            case APPROVE, REJECT -> vote(state, player, order.subject(), order.action() == DiplomacyOrder.Action.APPROVE);
            case LEAVE -> leave(state, player, turn);
        };
    }
    private static Optional<DiplomacyState> propose(DiplomacyState state, int player, DiplomacyOrder order, Set<Integer> players) {
        var own = state.groupFor(player);
        Set<Integer> voters = new HashSet<>(); voters.add(player);
        int groupId = 0;
        TreatyProposal.Kind kind;
        if (order.action() == DiplomacyOrder.Action.FOUND) {
            if (own.isPresent() || player == order.subject() || !players.contains(order.subject()) || state.groupFor(order.subject()).isPresent()) { return Optional.empty(); }
            voters.add(order.subject()); kind = TreatyProposal.Kind.FOUND;
        } else {
            var group = order.action() == DiplomacyOrder.Action.NOTICE ? own
                    : state.groups().stream().filter(candidate -> candidate.id() == order.subject()).findFirst();
            if (group.isEmpty() || (order.action() == DiplomacyOrder.Action.JOIN && own.isPresent())) { return Optional.empty(); }
            voters.addAll(group.orElseThrow().members()); groupId = group.orElseThrow().id();
            kind = order.action() == DiplomacyOrder.Action.NOTICE ? TreatyProposal.Kind.NOTICE : TreatyProposal.Kind.JOIN;
        }
        if (state.proposals().stream().anyMatch(p -> p.voters().stream().anyMatch(voters::contains))) { return Optional.empty(); }
        var pending = new ArrayList<>(state.proposals());
        pending.add(new TreatyProposal(state.nextId(), kind, groupId, player, order.noticeRounds(), voters, Set.of(player)));
        return Optional.of(new DiplomacyState(state.nextId() + 1, state.groups(), pending));
    }
    private static Optional<DiplomacyState> vote(DiplomacyState state, int player, int proposalId, boolean approve) {
        var found = state.proposals().stream().filter(p -> p.id() == proposalId).findFirst();
        if (found.isEmpty() || !found.orElseThrow().voters().contains(player)) { return Optional.empty(); }
        var proposal = found.orElseThrow();
        if (approve && proposal.approvals().contains(player)) { return Optional.empty(); }
        var pending = new ArrayList<>(state.proposals()); pending.remove(proposal);
        var groups = new ArrayList<>(state.groups());
        if (approve) {
            proposal = proposal.approve(player);
            if (!proposal.unanimous()) { pending.add(proposal); }
            else if (proposal.kind() == TreatyProposal.Kind.FOUND) {
                groups.add(new AllianceGroup(state.nextId(), proposal.voters(), proposal.noticeRounds(), java.util.Map.of()));
                return Optional.of(new DiplomacyState(state.nextId() + 1, groups, pending));
            } else {
                var completed = proposal;
                groups.replaceAll(group -> completeConsent(group, completed));
            }
        }
        return Optional.of(new DiplomacyState(state.nextId(), groups, pending));
    }
    private static AllianceGroup completeConsent(AllianceGroup group, TreatyProposal proposal) {
        if (group.id() != proposal.groupId()) { return group; }
        if (proposal.kind() == TreatyProposal.Kind.JOIN) { return group.admit(proposal.candidate()); }
        return group.agreeNotice(proposal.noticeRounds());
    }
    private static Optional<DiplomacyState> leave(DiplomacyState state, int player, int turn) {
        var own = state.groupFor(player);
        if (own.isEmpty() || own.orElseThrow().departures().containsKey(player)) { return Optional.empty(); }
        var groups = state.groups().stream().map(group -> group.id() == own.orElseThrow().id() ? group.announceDeparture(player, turn) : group).toList();
        return Optional.of(new DiplomacyState(state.nextId(), groups, state.proposals()));
    }
    public static DiplomacyState beginRound(DiplomacyState state, int turn) {
        var groups = new ArrayList<AllianceGroup>();
        var changed = new HashSet<Integer>();
        for (var group : state.groups()) {
            var remaining = new HashSet<>(group.members());
            remaining.removeIf(player -> group.departures().getOrDefault(player, Integer.MAX_VALUE) <= turn);
            if (!remaining.equals(group.members())) { changed.addAll(group.members()); }
            if (remaining.size() < 2) { continue; }
            var departures = new java.util.HashMap<>(group.departures()); departures.keySet().retainAll(remaining);
            groups.add(new AllianceGroup(group.id(), remaining, group.noticeRounds(), departures));
        }
        return new DiplomacyState(state.nextId(), groups, state.proposals().stream()
                .filter(proposal -> proposal.voters().stream().noneMatch(changed::contains)).toList());
    }
}
