package de.zettsystems.starfare.auth.ui;

import de.zettsystems.identity.application.IdentityUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class UserContext {
    private UserContext() {
    }

    /** @deprecated use {@link #currentPlayerId()} to make the identifier's semantics explicit. */
    @Deprecated(forRemoval = true)
    public static Optional<String> currentUsername() {
        return currentPlayerId();
    }

    public static Optional<String> currentPlayerId() {
        return currentUserId().map(String::valueOf);
    }

    /** Stabile fachliche Kennung eines Spielers; E-Mail und Anzeigename bleiben privat bzw. änderbar. */
    public static Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof IdentityUserDetails identityUser) {
            return Optional.of(identityUser.userId());
        }
        return Optional.empty();
    }
}
