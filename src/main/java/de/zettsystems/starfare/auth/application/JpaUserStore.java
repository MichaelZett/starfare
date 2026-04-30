package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.auth.domain.UserAccountEntity;
import de.zettsystems.starfare.auth.values.User;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class JpaUserStore implements UserStore {

    private final UserAccountRepository repository;

    public JpaUserStore(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return repository.findByUsername(username.toLowerCase(Locale.ROOT)).map(this::toUser);
    }

    @Override
    public void save(User user) {
        repository.findByUsername(user.username().toLowerCase(Locale.ROOT)).ifPresentOrElse(
                _ -> {
                    // username is @NaturalId — only mutable fields may change
                },
                () -> repository.save(
                        new UserAccountEntity(user.username().toLowerCase(Locale.ROOT), user.displayName(), user.passwordHash()))
        );
    }

    @Override
    public boolean exists(String username) {
        if (username == null) {
            return false;
        }
        return repository.findByUsername(username.toLowerCase(Locale.ROOT)).isPresent();
    }

    private User toUser(UserAccountEntity entity) {
        return new User(entity.getUsername(), entity.getPasswordHash(), entity.getDisplayName());
    }
}
