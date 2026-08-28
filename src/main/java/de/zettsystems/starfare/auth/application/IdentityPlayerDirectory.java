package de.zettsystems.starfare.auth.application;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.UserAccountDto;
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
 * Looks display names up in the identity building block. Names are cached briefly so that
 * list-style UIs (presence, chat, invitations) do not issue one query per rendered row;
 * a renamed player therefore shows up with the new name after at most {@link #TTL}.
 */
@Service
class IdentityPlayerDirectory implements PlayerDirectory {

    static final Duration TTL = Duration.ofMinutes(1);

    private final UserAccountService accounts;
    private final Clock clock;
    private final Map<String, CachedName> cache = new ConcurrentHashMap<>();

    @Autowired
    IdentityPlayerDirectory(UserAccountService accounts) {
        this(accounts, Clock.systemUTC());
    }

    IdentityPlayerDirectory(UserAccountService accounts, Clock clock) {
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
            return accounts.findById(Long.valueOf(playerId))
                    .map(UserAccountDto::displayName)
                    .orElse(playerId);
        } catch (NumberFormatException _) {
            return playerId;
        }
    }

    private record CachedName(String name, Instant validUntil) {
    }
}
