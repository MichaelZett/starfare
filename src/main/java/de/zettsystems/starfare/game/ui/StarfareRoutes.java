package de.zettsystems.starfare.game.ui;

/**
 * Starfare's routes. All of them start with the game key, because a host shares one router
 * between several games and owns {@code /} itself.
 */
public final class StarfareRoutes {
    public static final String KEY = "starfare";
    public static final String LOBBY = KEY;
    public static final String MAP = KEY + "/map/:gameId";
    /** Waiting room of a game that has not started. */
    public static final String MATCH = KEY + "/match/:gameId";
    /** Opens the wizard for a new game. */
    public static final String NEW_MATCH = KEY + "/new";
    /** Friends, invitations, who is online and chat. */
    public static final String SOCIAL = KEY + "/social";
    public static final String ARCHIVE = KEY + "/archive";
    public static final String STATISTICS = KEY + "/statistics";

    private StarfareRoutes() {
    }
}
