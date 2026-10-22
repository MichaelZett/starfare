package de.zettsystems.starfare.report.values;

import java.util.List;

/** Stored combat facts, independent of later treaties and ownership changes. */
public record CoalitionSide(int id, double strength, List<Member> members, double baseStrength) {
    public CoalitionSide(int id, double strength, List<Member> members) {
        this(id, strength, members, members.stream().mapToDouble(Member::ships).sum());
    }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static CoalitionSide restore(@com.fasterxml.jackson.annotation.JsonProperty("id") int id,
            @com.fasterxml.jackson.annotation.JsonProperty("strength") double strength,
            @com.fasterxml.jackson.annotation.JsonProperty("members") List<Member> members,
            @com.fasterxml.jackson.annotation.JsonProperty("baseStrength") @org.jspecify.annotations.Nullable Double baseStrength) {
        return new CoalitionSide(id, strength, members,
                baseStrength == null ? members.stream().mapToDouble(Member::ships).sum() : baseStrength);
    }
    public CoalitionSide {
        members = List.copyOf(members);
        if (id < 0 || !Double.isFinite(strength) || strength < 0 || !Double.isFinite(baseStrength) || baseStrength < 0) { throw new IllegalArgumentException("Invalid combat side"); }
    }
    public int ships() { return Math.toIntExact(members.stream().mapToLong(Member::ships).sum()); }
    public int remaining() { return members.stream().mapToInt(Member::remaining).sum(); }
    public boolean contains(int player) { return members.stream().anyMatch(member -> member.playerId() == player); }
    public record Member(int playerId, String name, int ships, int remaining, int destroyed) {
        public Member {
            if (playerId < 0 || ships < 0 || remaining < 0 || remaining > ships || destroyed < 0) {
                throw new IllegalArgumentException("Invalid combat member");
            }
        }
    }
}
