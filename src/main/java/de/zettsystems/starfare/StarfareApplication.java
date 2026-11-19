package de.zettsystems.starfare;

import com.vaadin.flow.i18n.I18NProvider;
import de.zettsystems.starfare.i18n.StarfareI18NProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import de.zettsystems.starfare.legal.values.LegalContact;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the Starfare application.
 */
@SpringBootApplication
// The game registers its own settings (GamePropertiesConfiguration); only the imprint is the own start's.
@EnableConfigurationProperties(LegalContact.class)
@EnableAsync
@EnableScheduling
public class StarfareApplication {
  static void main(String[] args) {
    SpringApplication.run(StarfareApplication.class, args);
  }

  /** Vaadin's global provider belongs to the starting application; a host brings its own. */
  @Bean
  I18NProvider starfareI18NProvider() {
    return new StarfareI18NProvider();
  }
}
