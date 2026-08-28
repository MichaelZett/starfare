package de.zettsystems.starfare.auth.application;

import java.util.Collection;
import java.util.Map;

/** Resolves the stable player ID used by Starfare to its public display name. */
public interface PlayerDirectory {

    /** Display name of the player, or the ID itself if no account is known for it. */
    String displayName(String playerId);

    /** Display names for several players at once; every requested ID is present in the result. */
    Map<String, String> displayNames(Collection<String> playerIds);
}
