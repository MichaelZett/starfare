package de.zettsystems.starfare.auth.config;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Creates the known demo account for local manual testing only.
 */
@Configuration
@Profile("local")
public class LocalDemoAccountConfiguration {

    static final String DEMO_EMAIL = "demo@starfare.local";
    static final String DEMO_PASSWORD = "starfare-demo-2026";

    @Bean
    CommandLineRunner createLocalDemoAccount(UserAccountService accountService) {
        return _ -> {
            if (accountService.findByEmail(DEMO_EMAIL).isEmpty()) {
                accountService.createAccount(
                        DEMO_EMAIL,
                        DEMO_PASSWORD,
                        AccountName.display("Starfare Demo"),
                        true);
            }
        };
    }
}
