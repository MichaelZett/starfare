package de.zettsystems.starfare.auth.ui;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The signed-in player for code that is not a Spring bean (views, dialogs). Reads the platform's
 * {@link PlatformAccounts} (games-platform-api), which {@link UserContextBinding} hands over when
 * the application starts: the host's in a platform host, Starfare's own in its own start. Without
 * a binding (plain unit tests) nobody is signed in.
 */
public final class UserContext {
    private static volatile @Nullable PlatformAccounts accounts;

    private UserContext() {
    }

    /** Binds the platform's accounts and returns the previous binding (tests restore it). */
    static @Nullable PlatformAccounts bind(@Nullable PlatformAccounts platformAccounts) {
        PlatformAccounts previous = accounts;
        accounts = platformAccounts;
        return previous;
    }

    public static Optional<String> currentPlayerId() {
        return currentUserId().map(String::valueOf);
    }

    /** Stabile fachliche Kennung eines Spielers; E-Mail und Anzeigename bleiben privat bzw. änderbar. */
    public static Optional<Long> currentUserId() {
        PlatformAccounts bound = accounts;
        return bound == null ? Optional.empty() : bound.current().map(AccountId::value);
    }
}
