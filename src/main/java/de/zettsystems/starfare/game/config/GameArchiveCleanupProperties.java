package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Deleting finished games: a finished game stays reviewable for {@code retention} (90 days) after its end,
 * then the stored game and its archive entry go together. On unless switched off.
 */
@ConfigurationProperties("starfare.game.archive-cleanup")
public record GameArchiveCleanupProperties(@DefaultValue("true") boolean enabled, Duration retention) {
    public GameArchiveCleanupProperties {
        if (retention == null || retention.isNegative() || retention.isZero()) {
            retention = Duration.ofDays(90);
        }
    }
}
