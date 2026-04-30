package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.social.values.Visibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class VisibilityFilterTest extends AbstractIntegrationTest {

    @Autowired
    private VisibilityFilter filter;

    @Autowired
    private FriendshipService friendships;

    @Autowired
    private UserPreferencesService preferences;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserPreferencesRepository preferencesRepository;

    @BeforeEach
    void cleanAll() {
        friendshipRepository.deleteAll();
        preferencesRepository.deleteAll();
    }

    @Test
    void defaultVisibilityIsSymmetricOpen() {
        assertThat(filter.canSee("alice", "bob")).isTrue();
    }

    @Test
    void observerWithNoneCannotSeeAnyone() {
        preferences.setVisibility("alice", Visibility.NONE);

        assertThat(filter.canSee("alice", "bob")).isFalse();
    }

    @Test
    void targetWithNoneIsHiddenFromOpenObserver() {
        preferences.setVisibility("bob", Visibility.NONE);

        assertThat(filter.canSee("alice", "bob")).isFalse();
    }

    @Test
    void friendsOnlySeesOnlyFriends() {
        preferences.setVisibility("alice", Visibility.FRIENDS_ONLY);
        assertThat(filter.canSee("alice", "bob")).isFalse();

        friendships.request("alice", "bob");
        friendships.accept("bob", "alice");

        assertThat(filter.canSee("alice", "bob")).isTrue();
    }

    @Test
    void blockHidesInBothDirections() {
        friendships.block("alice", "bob");

        assertThat(filter.canSee("alice", "bob")).isFalse();
        assertThat(filter.canSee("bob", "alice")).isFalse();
    }

    @Test
    void selfIsAlwaysVisible() {
        preferences.setVisibility("alice", Visibility.NONE);

        assertThat(filter.canSee("alice", "alice")).isTrue();
    }
}
