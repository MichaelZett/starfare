package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.config.GameArchiveCleanupProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class GameArchiveCleanupRunner {
    private final GameArchiveCleanupService cleanup; private final GameArchiveCleanupProperties properties;
    GameArchiveCleanupRunner(GameArchiveCleanupService cleanup, GameArchiveCleanupProperties properties) { this.cleanup = cleanup; this.properties = properties; }
    @Scheduled(cron = "${starfare.game.archive-cleanup.cron:0 15 3 * * *}")
    void removeExpiredGames() { if (properties.enabled()) cleanup.removeExpired(); }
}
