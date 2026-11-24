package de.zettsystems.starfare.game.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** A host binds Starfare's settings through this configuration alone, without scanning Starfare's packages. */
class GamePropertiesConfigurationTest {
    private final ApplicationContextRunner host = new ApplicationContextRunner()
            .withUserConfiguration(GamePropertiesConfiguration.class);

    @Test
    void bindsTheGameSettingsWithoutAPropertyScan() {
        host.withPropertyValues("starfare.game.single-player-unload-after=45m",
                        "starfare.game.archive-cleanup.enabled=false", "starfare.game.archive-cleanup.retention=10d")
                .run(context -> {
                    assertThat(context.getBean(GameTimingProperties.class).singlePlayerUnloadAfter())
                            .isEqualTo(Duration.ofMinutes(45));
                    GameArchiveCleanupProperties cleanup = context.getBean(GameArchiveCleanupProperties.class);
                    assertThat(cleanup.enabled()).isFalse();
                    assertThat(cleanup.retention()).isEqualTo(Duration.ofDays(10));
                });
    }

    @Test
    void fallsBackToDefaultsWithoutStarfareLinesInTheHostConfiguration() {
        host.run(context -> {
            assertThat(context.getBean(GameTimingProperties.class).singlePlayerUnloadAfter()).isEqualTo(Duration.ofMinutes(30));
            GameArchiveCleanupProperties cleanup = context.getBean(GameArchiveCleanupProperties.class);
            assertThat(cleanup.enabled()).as("finished games are deleted after their retention in the host").isTrue();
            assertThat(cleanup.retention()).isEqualTo(Duration.ofDays(90));
        });
    }
}
