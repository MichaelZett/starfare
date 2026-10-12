package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Local acceptance switch; ordinary creation keeps the released rules. */
@ConfigurationProperties("starfare.rules")
public record RulesetProperties(boolean economyPreview) { }
