package de.zettsystems.starfare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot entry point for the Starfare application.
 */
@SpringBootApplication
@EnableAsync
public class StarfareApplication {
  static void main(String[] args) {
    SpringApplication.run(StarfareApplication.class, args);
  }
}
