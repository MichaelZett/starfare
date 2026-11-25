package de.zettsystems.starfare.game.values;

/**
 * How much luck decides a game: one choice for the combat deviation, the frequency of cosmic events and the
 * order of several attackers on one system.
 */
public enum RandomnessLevel {
    /** Battles go exactly by strength, no events, the strongest attacker fights first. */
    NONE(RandomnessLevel.NONE_COMBAT_PERCENT, EventFrequency.OFF, AttackOrder.STRONGEST_FIRST),
    LOW(RandomnessLevel.LOW_COMBAT_PERCENT, EventFrequency.RARE, AttackOrder.RANDOM),
    MEDIUM(RandomnessLevel.MEDIUM_COMBAT_PERCENT, EventFrequency.NORMAL, AttackOrder.RANDOM),
    HIGH(RandomnessLevel.HIGH_COMBAT_PERCENT, EventFrequency.FREQUENT, AttackOrder.RANDOM);

    public static final RandomnessLevel DEFAULT = LOW;
    static final int NONE_COMBAT_PERCENT = 0;
    static final int LOW_COMBAT_PERCENT = 5;
    static final int MEDIUM_COMBAT_PERCENT = 10;
    static final int HIGH_COMBAT_PERCENT = 20;

    private final int combatRandomnessPercent;
    private final EventFrequency eventFrequency;
    private final AttackOrder attackOrder;

    RandomnessLevel(int combatRandomnessPercent, EventFrequency eventFrequency, AttackOrder attackOrder) {
        this.combatRandomnessPercent = combatRandomnessPercent;
        this.eventFrequency = eventFrequency;
        this.attackOrder = attackOrder;
    }

    /** Largest deviation of a side's strength before a battle, in percent. */
    public int combatRandomnessPercent() {
        return combatRandomnessPercent;
    }

    public EventFrequency eventFrequency() {
        return eventFrequency;
    }

    public AttackOrder attackOrder() {
        return attackOrder;
    }

    /** Whether cosmic events can strike at all. */
    public boolean hasEvents() {
        return eventFrequency != EventFrequency.OFF;
    }
}
