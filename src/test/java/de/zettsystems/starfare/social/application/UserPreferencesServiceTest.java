package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.social.values.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferencesServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserPreferencesService service;

    @Autowired
    private UserPreferencesRepository repository;

    @BeforeEach
    void cleanPreferences() {
        repository.deleteAll();
    }

    @Test
    void defaultVisibilityIsAll() {
        assertThat(service.getVisibility("alice")).isEqualTo(Visibility.ALL);
    }

    @Test
    void setVisibilityPersistsAndNormalisesUsername() {
        service.setVisibility("Alice", Visibility.FRIENDS_ONLY);

        assertThat(service.getVisibility("alice")).isEqualTo(Visibility.FRIENDS_ONLY);
        assertThat(service.getVisibility("ALICE")).isEqualTo(Visibility.FRIENDS_ONLY);
    }

    @Test
    void setVisibilityOverwritesExisting() {
        service.setVisibility("alice", Visibility.FRIENDS_ONLY);
        service.setVisibility("alice", Visibility.NONE);

        assertThat(service.getVisibility("alice")).isEqualTo(Visibility.NONE);
    }
}
