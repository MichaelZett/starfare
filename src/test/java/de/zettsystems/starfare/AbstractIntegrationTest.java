package de.zettsystems.starfare;

import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.RoundDeadlineRunner;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Import(SyncAsyncTestConfig.class)
// Profil-Datei statt application.yaml: eine gleichnamige Test-Ressource würde die
// Haupt-Konfiguration komplett verdecken (nur die erste Datei auf dem Classpath zählt).
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Tests invoke timeout processing explicitly; a scheduler must not race fixture cleanup.
    @MockitoBean
    private RoundDeadlineRunner roundDeadlineRunner;

    @ServiceConnection
    @SuppressWarnings("rawtypes")
    static final PostgreSQLContainer POSTGRES = TestcontainersPostgres.INSTANCE;

    @Autowired
    protected GameRegistry registry;

    @BeforeEach
    void cleanSessions() {
        registry.listIds().forEach(registry::abortGame);
    }
}
