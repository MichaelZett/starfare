package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.auth.values.User;

public interface UserService {
    int USERNAME_MIN = 3;
    int USERNAME_MAX = 30;
    int PASSWORD_MIN = 6;

    User register(String username, String rawPassword, String displayName);
}
