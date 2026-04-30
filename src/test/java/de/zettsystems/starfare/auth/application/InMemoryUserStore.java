package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.auth.values.User;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryUserStore implements UserStore {
    private final ConcurrentMap<String, User> byUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byUsername.get(username.toLowerCase()));
    }

    @Override
    public void save(User user) {
        byUsername.put(user.username().toLowerCase(), user);
    }

    @Override
    public boolean exists(String username) {
        if (username == null) {
            return false;
        }
        return byUsername.containsKey(username.toLowerCase());
    }
}
