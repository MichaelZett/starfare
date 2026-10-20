package de.zettsystems.starfare.game.config;

import de.zettsystems.starfare.game.values.RulesetCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(RulesetProperties.class)
public class RulesetConfiguration {
    @Bean
    RulesetCatalog rulesetCatalog(RulesetProperties properties) {
        return properties.researchPreview() ? RulesetCatalog.withShipResearch() : RulesetCatalog.builtIn();
    }
}
