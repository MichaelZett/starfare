package de.zettsystems.starfare.game.values;

import java.util.List;

/**
 * Centralized game configuration values for default setup and tuning knobs.
 */
public final class GameConfig {
    /**
     * Map pixels a fleet covers per round, the same on every map: with the fixed density of {@link GalaxyExtent}
     * most jumps to the nearest neighbour take three rounds.
     */
    public static final double TRAVEL_SPEED = 160.0;
    /** Shortest journey; even an adjacent system is never reached in the round of departure or the next. */
    public static final int MIN_TRAVEL_ROUNDS = 2;
    /** Longest journey; the diagonal of the largest offered galaxy (64 systems) takes 37 rounds. */
    public static final int MAX_TRAVEL_ROUNDS = 40;
    public static final int DEFAULT_SYSTEM_COUNT = GalaxySize.DEFAULT.systems();
    public static final int DEFAULT_HUMAN_PLAYERS = 1;
    public static final int DEFAULT_AI_PLAYERS = 2;
    /** Production of every person's home system; computers start with a share of it by their strength. */
    public static final int START_PRODUCTION = 4;
    /** Ships in every person's home system, at least its production; the start balance adjusts it. */
    public static final int START_GARRISON = 8;
    /** Highest production of a neutral system offered for a new game; the lowest is always 1. */
    public static final List<Integer> MAX_PRODUCTION_CHOICES = List.of(6, 10, 15, 20);
    public static final int DEFAULT_MAX_PRODUCTION = 10;
    public static final int MIN_SYSTEM_COUNT = 8;
    public static final int MAX_SYSTEM_COUNT = 120;
    public static final int MIN_PRODUCTION = 1;
    public static final int MAX_PRODUCTION = 20;
    /** Empires of a new game, people and computers together; at least one of them is a person. */
    public static final int MIN_TOTAL_PLAYERS = 2;
    public static final int MAX_TOTAL_PLAYERS = 10;
    public static final boolean DEFAULT_BATTLE_PRESENTATION_ENABLED = true;
    public static final int MIN_COMBAT_RANDOMNESS_PERCENT = 0;
    public static final int MAX_COMBAT_RANDOMNESS_PERCENT = 30;
    public static final int DEFAULT_COMBAT_RANDOMNESS_PERCENT = 10;
    public static final String DEFAULT_GAME_NAME = "Standardspiel";
    public static final GalaxyLayout DEFAULT_GALAXY_LAYOUT = GalaxyLayout.EVEN;

    /** Anteil aller Systeme (neutrale eingeschlossen), ab dem eine Partie gewonnen ist. */
    public static final int VICTORY_SYSTEM_PERCENT = 70;
    public static final int MIN_VICTORY_SYSTEM_PERCENT = 10;
    public static final int MAX_VICTORY_SYSTEM_PERCENT = 100;
    /**
     * Verpasste Rundenfristen in einer Partie (nicht nur in Folge), nach denen der Sitz endgültig an die KI geht
     * (TIMED_OUT, kein Wiedereinstieg). Das zweite Verpassen wirft hinaus (Plattformentscheidung 19.11.2026).
     */
    public static final int MAX_MISSED_ROUNDS = 2;
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
