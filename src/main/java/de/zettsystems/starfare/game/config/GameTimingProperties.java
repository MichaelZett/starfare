package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Memory-management settings for single-human matches.
 */
@ConfigurationProperties("starfare.game")
public record GameTimingProperties(Duration singlePlayerUnloadAfter) {

    public GameTimingProperties {
        if (singlePlayerUnloadAfter == null || singlePlayerUnloadAfter.isNegative() || singlePlayerUnloadAfter.isZero()) {
            singlePlayerUnloadAfter = Duration.ofMinutes(30);
        }
    }
}
