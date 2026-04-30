package de.zettsystems.starfare;

/**
 * Base class for repository integration tests. Spring Boot 4 dropped the
 * {@code @DataJpaTest} slice (and most other slices), so we ride on the
 * full {@link AbstractIntegrationTest} context — that's already shared
 * across the JVM via a single {@link TestcontainersPostgres} instance, so
 * the only "cost" is the one application context that boots anyway.
 *
 * <p>Deliberately <strong>not</strong> {@code @Transactional}: other test
 * classes (e.g. {@code UserServiceTest}) commit data through the service
 * layer that wouldn't be rolled back by a test-side transaction. Subclasses
 * therefore wipe the tables they own in {@code @BeforeEach}, mirroring the
 * pattern those service tests already use.
 */
public abstract class AbstractRepositoryTest extends AbstractIntegrationTest {
}
