package de.zettsystems.starfare.auth.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Looks display names up in the platform's accounts (games-platform-api {@link PlatformAccounts}).
 * Lists ask {@link #displayNames} so the platform answers with one lookup; Starfare keeps no cache
 * of its own, so a renamed player shows the new name at once.
 */
@Service
class IdentityPlayerDirectory implements PlayerDirectory {

    private final PlatformAccounts accounts;

    IdentityPlayerDirectory(PlatformAccounts accounts) {
        this.accounts = accounts;
    }

    @Override
    public String displayName(String playerId) {
        return account(playerId).flatMap(accounts::displayName).orElse(playerId);
    }

    @Override
    public Map<String, String> displayNames(Collection<String> playerIds) {
        Map<String, AccountId> known = new LinkedHashMap<>();
        playerIds.forEach(playerId -> account(playerId).ifPresent(account -> known.put(playerId, account)));
        Map<AccountId, String> found = known.isEmpty() ? Map.of() : accounts.displayNames(List.copyOf(known.values()));
        Map<String, String> names = new LinkedHashMap<>();
        for (String playerId : playerIds) {
            AccountId account = known.get(playerId);
            names.put(playerId, account == null ? playerId : found.getOrDefault(account, playerId));
        }
        return names;
    }

    /** A computer player's id or anything else that is not a positive number has no platform account. */
    private static Optional<AccountId> account(String playerId) {
        try {
            return Optional.of(AccountId.of(Long.parseLong(playerId)));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}