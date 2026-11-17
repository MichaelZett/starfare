package de.zettsystems.starfare.auth.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks display names up in the platform's accounts (games-platform-api {@link PlatformAccounts}).
 * Names are cached briefly so that list-style UIs (invitations, waiting rooms) do not issue one
 * query per rendered row; a renamed player therefore shows up with the new name after at most
 * {@link #TTL}.
 */
@Service
class IdentityPlayerDirectory implements PlayerDirectory {

    static final Duration TTL = Duration.ofMinutes(1);

    private final PlatformAccounts accounts;
    private final Clock clock;
    private final Map<String, CachedName> cache = new ConcurrentHashMap<>();

    @Autowired
    IdentityPlayerDirectory(PlatformAccounts accounts) {
        this(accounts, Clock.systemUTC());
    }

    IdentityPlayerDirectory(PlatformAccounts accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Override
    public String displayName(String playerId) {
        Instant now = clock.instant();
        CachedName cached = cache.get(playerId);
        if (cached != null && cached.validUntil().isAfter(now)) {
            return cached.name();
        }
        String name = lookup(playerId);
        cache.put(playerId, new CachedName(name, now.plus(TTL)));
        return name;
    }

    @Override
    public Map<String, String> displayNames(Collection<String> playerIds) {
        Map<String, String> names = new LinkedHashMap<>();
        for (String playerId : playerIds) {
            names.put(playerId, displayName(playerId));
        }
        return names;
    }

    private String lookup(String playerId) {
        try {
            return accounts.displayName(AccountId.of(Long.parseLong(playerId))).orElse(playerId);
        } catch (IllegalArgumentException _) {
            // Not a number or not positive: no platform account, e.g. a computer player's id.
            return playerId;
        }
    }

    private record CachedName(String name, Instant validUntil) {
    }
}
