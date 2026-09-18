package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Zeitregeln und Kampfreihenfolge einer Partie.
 *
 * <p>{@code roundLimit} läuft ab Rundenbeginn. {@code stragglerLimit} startet, sobald nur noch
 * ein menschlicher Spieler fehlt. Die Runde endet mit der ersten der beiden Fristen.
 */
public record RoundRules(Duration roundLimit, Duration stragglerLimit, AttackOrder attackOrder) {

    public static final Duration DEFAULT_ROUND_LIMIT = Duration.ofMinutes(5);
    public static final Duration DEFAULT_STRAGGLER_LIMIT = Duration.ofMinutes(1);
    public static final AttackOrder DEFAULT_ATTACK_ORDER = AttackOrder.RANDOM;
    public static final Duration MIN_LIMIT = Duration.ofSeconds(30);
    public static final Duration MAX_LIMIT = Duration.ofDays(7);

    public static final List<Duration> ROUND_LIMIT_CHOICES = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(5), Duration.ofMinutes(10),
            Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2),
            Duration.ofHours(6), Duration.ofHours(12), Duration.ofDays(1), Duration.ofDays(2),
            Duration.ofDays(3), Duration.ofDays(7));
    public static final List<Duration> STRAGGLER_LIMIT_CHOICES = List.of(
            Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(5),
            Duration.ofMinutes(10), Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(6),
            Duration.ofHours(12), Duration.ofDays(1), Duration.ofDays(2));

    public RoundRules(@Nullable Duration roundLimit, @Nullable Duration stragglerLimit,
                      @Nullable AttackOrder attackOrder) {
        this.roundLimit = bounded(roundLimit, DEFAULT_ROUND_LIMIT);
        this.stragglerLimit = bounded(stragglerLimit, DEFAULT_STRAGGLER_LIMIT);
        this.attackOrder = attackOrder == null ? DEFAULT_ATTACK_ORDER : attackOrder;
    }

    public static RoundRules defaults() {
        return new RoundRules(DEFAULT_ROUND_LIMIT, DEFAULT_STRAGGLER_LIMIT, DEFAULT_ATTACK_ORDER);
    }

    private static Duration bounded(@Nullable Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
            return fallback;
        }
        if (value.compareTo(MIN_LIMIT) < 0) {
            return MIN_LIMIT;
        }
        return value.compareTo(MAX_LIMIT) > 0 ? MAX_LIMIT : value;
    }
}
