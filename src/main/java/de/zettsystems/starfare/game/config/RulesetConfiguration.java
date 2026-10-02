package de.zettsystems.starfare.game.config;

import de.zettsystems.starfare.game.values.RulesetCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RulesetConfiguration {
    @Bean
    RulesetCatalog rulesetCatalog() {
        return RulesetCatalog.builtIn();
    }
}
