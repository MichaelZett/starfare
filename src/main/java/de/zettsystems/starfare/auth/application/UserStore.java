package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.auth.values.User;

import java.util.Optional;

public interface UserStore {
    Optional<User> findByUsername(String username);

    void save(User user);

    boolean exists(String username);
}
