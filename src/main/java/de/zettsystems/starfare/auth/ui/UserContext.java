package de.zettsystems.starfare.auth.ui;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class UserContext {
    private UserContext() {
    }

    public static Optional<String> currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        String name = auth.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }
}
