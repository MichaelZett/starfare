package de.zettsystems.starfare.auth.application;

/** Resolves the stable player ID used by Starfare to its public display name. */
public interface PlayerDirectory {

    String displayName(String playerId);
}
