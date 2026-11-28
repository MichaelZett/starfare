package de.zettsystems.starfare.game.values;

import java.time.Duration;

/**
 * How fast a game with several people runs, chosen when it is created and fixed from then on
 * (platform tempo plan, user decision 20.11.2026). Each tempo sets the round limit and the straggler
 * limit, which starts once only one person is still missing; neither is chosen freely. The names match
 * the platform's tempos in {@code games/starfare/game.properties}.
 */
public enum GameTempo {
    LIVE(Duration.ofMinutes(5), Duration.ofMinutes(1)),
    BRISK(Duration.ofHours(1), Duration.ofMinutes(15)),
    DAILY(Duration.ofHours(24), Duration.ofHours(6)),
    /** One round every other day (API 0.6.0, all platform games, 28.11.2026). */
    EVERY_OTHER_DAY(Duration.ofHours(48), Duration.ofHours(12)),
    RELAXED(Duration.ofHours(72), Duration.ofHours(12));

    /** The tempo a new game with several people starts with. */
    public static final GameTempo DEFAULT = LIVE;

    private final Duration roundLimit;
    private final Duration stragglerLimit;

    GameTempo(Duration roundLimit, Duration stragglerLimit) {
        this.roundLimit = roundLimit;
        this.stragglerLimit = stragglerLimit;
    }

    public Duration roundLimit() {
        return roundLimit;
    }

    public Duration stragglerLimit() {
        return stragglerLimit;
    }
}
