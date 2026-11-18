package de.zettsystems.starfare.standalone.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoloPlatformSocialTest {
    private static final AccountId ONE = AccountId.of(1);
    private static final AccountId TWO = AccountId.of(2);

    @Test
    void theOwnStartHasNoFriendsAndNobodyOnline() {
        SoloPlatformFriends friends = new SoloPlatformFriends();
        SoloPlatformPresence presence = new SoloPlatformPresence();

        assertThat(friends.friendsOf(ONE)).isEmpty();
        assertThat(friends.areFriends(ONE, TWO)).isFalse();
        assertThat(friends.isBlockedBetween(ONE, TWO)).isFalse();
        assertThat(presence.canSee(ONE, TWO)).isTrue();
        assertThat(presence.isOnlineFor(ONE, TWO)).isFalse();
        assertThat(presence.onlineFor(ONE)).isEmpty();
    }
}
