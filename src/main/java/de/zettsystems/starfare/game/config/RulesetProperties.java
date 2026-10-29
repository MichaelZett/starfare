package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Retained launch-profile settings; new games always use the current rules. */
@ConfigurationProperties("starfare.rules")
public record RulesetProperties(boolean economyPreview, boolean researchPreview) { }
