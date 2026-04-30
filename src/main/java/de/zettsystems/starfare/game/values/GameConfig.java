package de.zettsystems.starfare.game.values;

import java.util.List;

/**
 * Centralized game configuration values for default setup and tuning knobs.
 */
public final class GameConfig {
    public static final int DEFAULT_SYSTEM_COUNT = 24;
    public static final int DEFAULT_HUMAN_PLAYERS = 1;
    public static final int DEFAULT_AI_PLAYERS = 2;
    public static final int DEFAULT_START_SYSTEM_PRODUCTION = 4;
    public static final int DEFAULT_NEUTRAL_MIN_PRODUCTION = 2;
    public static final int DEFAULT_NEUTRAL_MAX_PRODUCTION = 6;
    public static final int DEFAULT_START_GARRISON = 8;
    public static final int MIN_SYSTEM_COUNT = 8;
    public static final int MAX_SYSTEM_COUNT = 120;
    public static final int MIN_AI_PLAYERS = 0;
    public static final int MAX_AI_PLAYERS = 7;
    public static final int MIN_HUMAN_PLAYERS = 1;
    public static final int MAX_HUMAN_PLAYERS = 8;
    public static final int MAX_TOTAL_PLAYERS = 10;
    public static final boolean DEFAULT_OBSERVERS_ALLOWED = true;
    public static final boolean DEFAULT_REENTRY_ALLOWED = true;
    public static final String DEFAULT_GAME_NAME = "Standardspiel";
    public static final int SPACEOUT_ITERATIONS = 60;
    public static final double SPACEOUT_MIN_DIST = 110.0;
    public static final List<String> PLAYER_PALETTE = List.of(
            "#0072B2", "#E69F00", "#009E73", "#D55E00", "#CC79A7",
            "#56B4E9", "#F0E442", "#C0392B", "#00A6A6", "#7A4EAB"
    );

    private GameConfig() {}
}
