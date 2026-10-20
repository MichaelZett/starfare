package de.zettsystems.starfare.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Retained launch-profile setting; W6/W7 selection is available regardless of this value. */
@ConfigurationProperties("starfare.rules")
public record RulesetProperties(boolean economyPreview, boolean researchPreview) { }
