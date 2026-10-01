package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.social.values.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferencesServiceTest extends AbstractIntegrationTest {
    @Test
    void displayChoicesPersistIndependentlyPerAccountAndKeepVisibility() {
        var speed = de.zettsystems.starfare.social.values.DisplaySetting.SPEED;
        var sound = de.zettsystems.starfare.social.values.DisplaySetting.SOUND;
        service.setVisibility("alice", Visibility.FRIENDS_ONLY);
        service.chooseDisplaySetting("alice", speed, "0.0");
        service.chooseDisplaySetting("alice", sound, "false");
        service.chooseDisplaySetting("bob", speed, "2.0");
        assertThat(service.displaySettings("alice")).containsEntry(speed, "0.0").containsEntry(sound, "false");
        assertThat(service.displaySettings("bob")).containsOnlyKeys(speed).containsEntry(speed, "2.0");
        assertThat(service.getVisibility("alice")).isEqualTo(Visibility.FRIENDS_ONLY);
    }

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
    void setVisibilityPersistsAndTrimsPlayerId() {
        service.setVisibility(" alice ", Visibility.FRIENDS_ONLY);

        assertThat(service.getVisibility("alice")).isEqualTo(Visibility.FRIENDS_ONLY);
        assertThat(service.getVisibility(" alice ")).isEqualTo(Visibility.FRIENDS_ONLY);
    }

    @Test
    void setVisibilityOverwritesExisting() {
        service.setVisibility("alice", Visibility.FRIENDS_ONLY);
        service.setVisibility("alice", Visibility.NONE);

        assertThat(service.getVisibility("alice")).isEqualTo(Visibility.NONE);
    }
}
