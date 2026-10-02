package de.zettsystems.starfare.diplomacy.values;

/** Subject is another empire, a group or a proposal according to the action. */
public record DiplomacyOrder(Action action, int subject, int noticeRounds) {
    public enum Action { FOUND, JOIN, NOTICE, APPROVE, REJECT, LEAVE }
    public DiplomacyOrder { java.util.Objects.requireNonNull(action); }
}
