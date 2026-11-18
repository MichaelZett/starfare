package de.zettsystems.starfare.auth.ui;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The signed-in player for code that is not a Spring bean (views, dialogs). Reads the platform's
 * {@link PlatformAccounts} (games-platform-api), which {@link UserContextBinding} hands over when
 * the application starts: the host's in a platform host, Starfare's own in its own start. Without
 * a binding (plain unit tests) nobody is signed in.
 */
public final class UserContext {
    private static final AtomicReference<@Nullable PlatformAccounts> ACCOUNTS = new AtomicReference<>();

    private UserContext() {
    }

    /** Binds the platform's accounts and returns the previous binding (tests restore it). */
    static @Nullable PlatformAccounts bind(@Nullable PlatformAccounts platformAccounts) {
        return ACCOUNTS.getAndSet(platformAccounts);
    }

    public static Optional<String> currentPlayerId() {
        return currentUserId().map(String::valueOf);
    }

    /** Stabile fachliche Kennung eines Spielers; E-Mail und Anzeigename bleiben privat bzw. änderbar. */
    public static Optional<Long> currentUserId() {
        PlatformAccounts bound = ACCOUNTS.get();
        return bound == null ? Optional.empty() : bound.current().map(AccountId::value);
    }
}
