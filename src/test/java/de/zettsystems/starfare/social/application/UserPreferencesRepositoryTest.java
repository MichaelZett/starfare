package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractRepositoryTest;
import de.zettsystems.starfare.social.domain.UserPreferencesEntity;
import de.zettsystems.starfare.social.values.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferencesRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserPreferencesRepository repository;

    private static final Instant NOW = Instant.parse("2026-04-22T11:00:00Z");

    @BeforeEach
    void clearPreferences() {
        repository.deleteAll();
    }

    @Test
    void findByUsernameReturnsStoredPreference() {
        repository.save(new UserPreferencesEntity("alice", Visibility.FRIENDS_ONLY, NOW));

        Optional<UserPreferencesEntity> found = repository.findById("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getVisibility()).isEqualTo(Visibility.FRIENDS_ONLY);
    }

    @Test
    void changeVisibilityIsPersistedOnSave() {
        UserPreferencesEntity stored = repository.saveAndFlush(
                new UserPreferencesEntity("alice", Visibility.ALL, NOW));
        Instant later = NOW.plusSeconds(60);

        stored.changeVisibility(Visibility.NONE, later);
        repository.saveAndFlush(stored);

        UserPreferencesEntity reloaded = repository.findById("alice").orElseThrow();
        assertThat(reloaded.getVisibility()).isEqualTo(Visibility.NONE);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(later);
    }
}
