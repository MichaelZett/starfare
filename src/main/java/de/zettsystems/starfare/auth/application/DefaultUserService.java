package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.auth.values.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DefaultUserService implements UserService {

    private final UserStore store;
    private final PasswordEncoder encoder;

    public DefaultUserService(UserStore store, PasswordEncoder encoder) {
        this.store = store;
        this.encoder = encoder;
    }

    @Override
    public User register(String username, String rawPassword, String displayName) {
        String u = username == null ? "" : username.trim();
        String p = rawPassword == null ? "" : rawPassword;
        String d = displayName == null || displayName.isBlank() ? u : displayName.trim();

        if (u.length() < USERNAME_MIN || u.length() > USERNAME_MAX) {
            throw new IllegalArgumentException("Benutzername muss " + USERNAME_MIN + "-" + USERNAME_MAX + " Zeichen haben");
        }
        if (!u.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Benutzername darf nur Buchstaben, Ziffern, _.- enthalten");
        }
        if (p.length() < PASSWORD_MIN) {
            throw new IllegalArgumentException("Passwort muss mindestens " + PASSWORD_MIN + " Zeichen haben");
        }
        if (store.exists(u)) {
            throw new IllegalArgumentException("Benutzername bereits vergeben");
        }

        User user = new User(u, Objects.requireNonNull(encoder.encode(p)), d);
        store.save(user);
        return user;
    }
}
