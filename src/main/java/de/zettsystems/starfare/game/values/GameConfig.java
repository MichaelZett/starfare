package de.zettsystems.starfare.game.values;

import java.util.List;

/**
 * Centralized game configuration values for default setup and tuning knobs.
 */
public final class GameConfig {
    /** Map extent in pixels; star systems are placed within [0, MAX_X) x [0, MAX_Y). */
    public static final float MAX_X = 3200.0F;
    public static final float MAX_Y = 2000.0F;
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
    public static final ProductionDistribution DEFAULT_PRODUCTION_DISTRIBUTION = ProductionDistribution.UNIFORM;
    public static final GalaxyLayout DEFAULT_GALAXY_LAYOUT = GalaxyLayout.RANDOM;

    /** Anteil aller Systeme (neutrale eingeschlossen), ab dem eine Partie gewonnen ist. */
    public static final int VICTORY_SYSTEM_PERCENT = 70;
    /**
     * Abstand, den Systemmittelpunkte zum Kartenrand halten. Die Punkte werden per
     * CSS um den halben Durchmesser zentriert (86-94 px), ohne Rand ragt ein System
     * am Rand also sichtbar aus der Karte heraus.
     */
    public static final double SYSTEM_MARGIN = 60.0;
    public static final int SPACEOUT_ITERATIONS = 60;
    public static final double SPACEOUT_MIN_DIST = 110.0;
    public static final List<String> PLAYER_PALETTE = List.of(
            "#0072B2", "#E69F00", "#009E73", "#D55E00", "#CC79A7",
            "#56B4E9", "#F0E442", "#C0392B", "#00A6A6", "#7A4EAB"
    );

    private GameConfig() {}
}
