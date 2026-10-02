package de.zettsystems.starfare.diplomacy.values;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** All members share one treaty; departures keep their originally agreed deadline. */
public record AllianceGroup(int id, Set<Integer> members, int noticeRounds, Map<Integer, Integer> departures) {
    public AllianceGroup {
        members = Set.copyOf(members);
        departures = Map.copyOf(departures);
        if (id < 1 || members.size() < 2 || noticeRounds < 0 || noticeRounds > 50
                || !members.containsAll(departures.keySet()) || departures.values().stream().anyMatch(turn -> turn < 1)) {
            throw new IllegalArgumentException("Invalid alliance group");
        }
    }
    public AllianceGroup admit(int player) {
        var updated = new HashSet<>(members); updated.add(player);
        return new AllianceGroup(id, updated, noticeRounds, departures);
    }
    public AllianceGroup agreeNotice(int rounds) { return new AllianceGroup(id, members, rounds, departures); }
    public AllianceGroup announceDeparture(int player, int turn) {
        var updated = new HashMap<>(departures); updated.put(player, turn + Math.max(1, noticeRounds));
        return new AllianceGroup(id, members, noticeRounds, updated);
    }
}
