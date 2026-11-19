package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Starfare's own settings, so a platform host needs no property scan of Starfare's
 * packages (same as Frontier Lords). The imprint contact belongs to the own start only.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GameArchiveCleanupProperties.class, GameTimingProperties.class})
public class GamePropertiesConfiguration {
}