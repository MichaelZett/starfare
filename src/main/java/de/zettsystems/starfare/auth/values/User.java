package de.zettsystems.starfare.auth.values;

public record User(String username, String passwordHash, String displayName) {
}
