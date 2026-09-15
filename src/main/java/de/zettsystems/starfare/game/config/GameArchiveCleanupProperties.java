package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("starfare.game.archive-cleanup")
public record GameArchiveCleanupProperties(boolean enabled, Duration retention) {
    public GameArchiveCleanupProperties { if (retention == null || retention.isNegative() || retention.isZero()) retention = Duration.ofDays(90); }
}
