package de.zettsystems.starfare.standalone.social.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformFriends;
import de.zettsystems.starfare.game.platform.StarfareMatches;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Starfare's own friendships behind the platform's interface, so that the game asks the same way
 * in its own start as in a host (plan M13). Players are account ids as text.
 */
@Component
public class StandalonePlatformFriends implements PlatformFriends {
    private final FriendshipService friendships;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected repositories and collaborators are shared Spring beans") // rationale: dependency injection shares collaborators.
    public StandalonePlatformFriends(FriendshipService friendships) {
        this.friendships = friendships;
    }

    @Override
    public List<AccountId> friendsOf(AccountId account) {
        String self = StarfareMatches.accountName(account);
        return friendships.friendsOf(self).stream()
                .map(friendship -> friendship.otherSide(self).flatMap(StarfareMatches::account))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public boolean areFriends(AccountId first, AccountId second) {
        return friendships.areFriends(StarfareMatches.accountName(first), StarfareMatches.accountName(second));
    }

    @Override
    public boolean isBlockedBetween(AccountId first, AccountId second) {
        return friendships.isBlockedBetween(StarfareMatches.accountName(first), StarfareMatches.accountName(second));
    }
}
