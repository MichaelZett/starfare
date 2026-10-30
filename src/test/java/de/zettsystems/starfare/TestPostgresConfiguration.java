package de.zettsystems.starfare;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** One connection definition so service and Browserless tests can share their Spring context. */
@TestConfiguration(proxyBeanMethods = false)
public class TestPostgresConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings("rawtypes") // rationale: Testcontainers exposes a self-referential generic type.
    PostgreSQLContainer postgres() {
        return TestcontainersPostgres.INSTANCE;
    }
}
