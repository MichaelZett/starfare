package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.AbstractRepositoryTest;
import de.zettsystems.starfare.auth.domain.UserAccountEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserAccountRepository repository;

    @BeforeEach
    void clearAccounts() {
        // The @SpringBootTest context is shared across the JVM, so other test
        // classes can have committed user_accounts before us. Wipe the table
        // ourselves — @Transactional rollback can't reach those rows.
        repository.deleteAll();
    }

    @Test
    void findByUsernameReturnsSavedAccount() {
        repository.save(new UserAccountEntity("alice", "Alice", "{noop}secret"));

        Optional<UserAccountEntity> found = repository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void findByUsernameMissesUnknown() {
        assertThat(repository.findByUsername("nobody")).isEmpty();
    }
}
