package de.zettsystems.starfare.testsupport;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformFriends;
import de.zettsystems.gamesplatformapi.PlatformPresence;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Friends and visibility as a platform host answers them, adjustable per test: accounts can be
 * friends, an account can hide from another or block it. Starfare's own start knows none of this
 * and answers with empty lists.
 */
public class TestPlatformSocial implements PlatformFriends, PlatformPresence {
    private final Set<List<AccountId>> hidden = ConcurrentHashMap.newKeySet();
    private final Set<Set<AccountId>> blocked = ConcurrentHashMap.newKeySet();
    private final Set<Set<AccountId>> friendships = ConcurrentHashMap.newKeySet();

    public void befriend(AccountId one, AccountId other) {
        friendships.add(Set.of(one, other));
    }

    /** {@code account} no longer shows itself to {@code viewer}. */
    public void hide(AccountId account, AccountId viewer) {
        hidden.add(List.of(account, viewer));
    }

    public void block(AccountId one, AccountId other) {
        blocked.add(Set.of(one, other));
    }

    public void reset() {
        hidden.clear();
        blocked.clear();
        friendships.clear();
    }

    @Override
    public List<AccountId> friendsOf(AccountId account) {
        return friendships.stream().filter(pair -> pair.contains(account))
                .flatMap(pair -> pair.stream().filter(member -> !member.equals(account)))
                .sorted(java.util.Comparator.comparingLong(AccountId::value)).toList();
    }

    @Override
    public boolean areFriends(AccountId one, AccountId other) {
        return !one.equals(other) && friendships.contains(Set.of(one, other));
    }

    @Override
    public boolean isBlockedBetween(AccountId one, AccountId other) {
        return blocked.contains(Set.of(one, other));
    }

    @Override
    public boolean canSee(AccountId viewer, AccountId other) {
        return !hidden.contains(List.of(other, viewer)) && !isBlockedBetween(viewer, other);
    }

    @Override
    public boolean isOnlineFor(AccountId viewer, AccountId other) {
        return false;
    }

    @Override
    public List<AccountId> onlineFor(AccountId viewer) {
        return List.of();
    }

    /** Replaces the own start's answers in every Spring test context. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Configuration {
        @Bean
        @Primary
        TestPlatformSocial testPlatformSocial() {
            return new TestPlatformSocial();
        }
    }
}
