package de.zettsystems.starfare.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Der Spring-Kontext der Cucumber-Suite: die echte Anwendung auf einem
 * zufälligen Port, eigene Postgres-Instanz, aufgezeichnete statt verschickte
 * Mails. Profil {@code test} schaltet Docker Compose ab; alles andere kommt aus
 * der produktiven {@code application.yaml}.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RecordingMailConfiguration.class)
public class CucumberSpringConfiguration {

    @ServiceConnection
    @SuppressWarnings("rawtypes")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("starfare_e2e");

    static {
        POSTGRES.start();
    }
}
