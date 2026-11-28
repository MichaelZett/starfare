package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * Optional SectorForces rules after Galactic Empire (1983): random events, a start garrison that
 * balances the position of each home world, and a round limit with a score.
 *
 * @param eventFrequency    master switch and scale of the random events
 * @param enabledEvents     the events the host left switched on
 * @param positionalBalance whether the start garrison depends on the neighbourhood of the home world
 * @param roundLimit        last round of the game, scored when nobody has won before; 0 means no limit
 */
public record SectorOptions(EventFrequency eventFrequency, Set<CosmicEvent> enabledEvents,
                            boolean positionalBalance, int roundLimit) {

    public static final int MIN_ROUND_LIMIT = 10;
    public static final int MAX_ROUND_LIMIT = 500;
    /** Rounds before the limit from which the report warns about the end. */
    public static final int ROUND_LIMIT_WARNING = 5;

    public SectorOptions {
        eventFrequency = eventFrequency == null ? EventFrequency.OFF : eventFrequency;
        enabledEvents = enabledEvents == null || enabledEvents.isEmpty()
                ? Set.of() : Set.copyOf(EnumSet.copyOf(enabledEvents));
        roundLimit = roundLimit <= 0 ? 0 : Math.clamp(roundLimit, MIN_ROUND_LIMIT, MAX_ROUND_LIMIT);
    }

    /** Everything off, as in games created before these options existed. */
    public static SectorOptions none() {
        return new SectorOptions(EventFrequency.OFF, EnumSet.allOf(CosmicEvent.class), false, 0);
    }

    public static SectorOptions orNone(@Nullable SectorOptions options) {
        return options == null ? none() : options;
    }

    /** Whether the event strikes at all: the master switch is on and the event itself too. */
    public boolean enabled(CosmicEvent event) {
        if (eventFrequency == EventFrequency.OFF || !enabledEvents.contains(event)) {
            return false;
        }
        return event != CosmicEvent.PIRATES || enabledEvents.contains(CosmicEvent.SUPERNOVA);
    }

    /** The events that can strike, in their declaration order. */
    public Set<CosmicEvent> activeEvents() {
        EnumSet<CosmicEvent> active = EnumSet.noneOf(CosmicEvent.class);
        for (CosmicEvent event : CosmicEvent.values()) {
            if (enabled(event)) {
                active.add(event);
            }
        }
        return active;
    }

    /** Chance of the event in one round, 0 when it is switched off. */
    public double chance(CosmicEvent event) {
        return enabled(event) ? eventFrequency.scale(event.baseChance()) : 0.0;
    }

    /**
     * Whether another sun may turn unstable after {@code supernovaeSoFar} unstable or exploded ones. With rare
     * events a game has at most one supernova (User 25.11.2026); otherwise there is no limit.
     */
    public boolean allowsSupernova(int supernovaeSoFar) {
        return eventFrequency != EventFrequency.RARE || supernovaeSoFar < 1;
    }

    public boolean hasRoundLimit() {
        return roundLimit > 0;
    }

    public boolean isDefault() {
        return activeEvents().isEmpty() && !positionalBalance && roundLimit == 0;
    }
}
