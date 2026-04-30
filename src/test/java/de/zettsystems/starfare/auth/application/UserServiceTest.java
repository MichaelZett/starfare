package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.auth.values.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService service;

    @Autowired
    private UserStore store;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void cleanUsers() {
        userAccountRepository.deleteAll();
    }

    @Test
    void registerStoresHashedPasswordAndDisplayName() {
        User user = service.register("alice", "secret123", "Alice Anderson");

        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.displayName()).isEqualTo("Alice Anderson");
        assertThat(user.passwordHash()).isNotEqualTo("secret123");
        assertThat(new BCryptPasswordEncoder().matches("secret123", user.passwordHash())).isTrue();
    }

    @Test
    void registerWithBlankDisplayNameFallsBackToUsername() {
        User user = service.register("bob", "secret123", "   ");

        assertThat(user.displayName()).isEqualTo("bob");
    }

    @Test
    void registerRejectsShortUsername() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.register("ab", "secret123", "A"));
    }

    @Test
    void registerRejectsInvalidCharactersInUsername() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.register("alice bob", "secret123", "A"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.register("alice@home", "secret123", "A"));
    }

    @Test
    void registerRejectsShortPassword() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.register("alice", "short", "A"));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        service.register("alice", "secret123", "A");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.register("alice", "othersecret", "A"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> service.register("ALICE", "othersecret", "A"));
    }

    @Test
    void registeredUserIsFindableInStore() {
        service.register("alice", "secret123", "Alice");

        Optional<User> found = store.findByUsername("alice");
        assertThat(found).isPresent();
    }
}
