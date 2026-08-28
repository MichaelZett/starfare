package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Timing settings for real-time matches.
 *
 * <p>Das Prüfintervall des Schedulers steht bewusst nicht hier: {@code @Scheduled}
 * braucht einen Konstantenausdruck und liest
 * {@code starfare.game.inactivity-check-interval} direkt aus der Umgebung.
 */
@ConfigurationProperties("starfare.game")
public record GameTimingProperties(Duration inactivityTimeout) {

    public GameTimingProperties {
        if (inactivityTimeout == null || inactivityTimeout.isNegative() || inactivityTimeout.isZero()) {
            inactivityTimeout = Duration.ofMinutes(5);
        }
    }
}
