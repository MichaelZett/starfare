package de.zettsystems.starfare;

import de.zettsystems.starfare.testsupport.PostgresTestImage;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Single JVM-wide PostgreSQL Testcontainer shared by both the full
 * {@link AbstractIntegrationTest} and the lightweight
 * {@link AbstractRepositoryTest} slice — keeps Docker usage to one
 * container per build instead of one per test base.
 */
final class TestcontainersPostgres {

    @SuppressWarnings("rawtypes")
    static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer(PostgresTestImage.dockerImage());

    static {
        INSTANCE.start();
    }

    private TestcontainersPostgres() {
    }
}
