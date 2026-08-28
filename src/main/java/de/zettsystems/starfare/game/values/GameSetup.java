package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Wizard input used to create a new game session.
 */
public record GameSetup(
        int systemCount,
        int humanPlayers,
        int aiPlayers,
        List<Integer> startProductionPerPlayer,
        int neutralMinProduction,
        int neutralMaxProduction,
        int startGarrison,
        boolean observersAllowed,
        boolean reentryAllowed,
        List<String> seatColorHexes
) {
    public static GameSetup defaults() {
        int totalPlayers = GameConfig.DEFAULT_HUMAN_PLAYERS + GameConfig.DEFAULT_AI_PLAYERS;
        List<Integer> startProductions = new ArrayList<>();
        for (int i = 0; i < totalPlayers; i++) {
            startProductions.add(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION);
        }
        return new GameSetup(
                GameConfig.DEFAULT_SYSTEM_COUNT,
                GameConfig.DEFAULT_HUMAN_PLAYERS,
                GameConfig.DEFAULT_AI_PLAYERS,
                List.copyOf(startProductions),
                GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION,
                GameConfig.DEFAULT_NEUTRAL_MAX_PRODUCTION,
                GameConfig.DEFAULT_START_GARRISON,
                GameConfig.DEFAULT_OBSERVERS_ALLOWED,
                GameConfig.DEFAULT_REENTRY_ALLOWED,
                GameConfig.PLAYER_PALETTE
        );
    }

    public GameSetup normalized() {
        int humanLowerBound = observersAllowed ? 0 : GameConfig.MIN_HUMAN_PLAYERS;
        int humans = clamp(humanPlayers, humanLowerBound, GameConfig.MAX_HUMAN_PLAYERS);
        int ai = clamp(aiPlayers, GameConfig.MIN_AI_PLAYERS, GameConfig.MAX_AI_PLAYERS);
        if (humans + ai > GameConfig.MAX_TOTAL_PLAYERS) {
            ai = Math.max(GameConfig.MIN_AI_PLAYERS, GameConfig.MAX_TOTAL_PLAYERS - humans);
        }
        int systems = clamp(systemCount, GameConfig.MIN_SYSTEM_COUNT, GameConfig.MAX_SYSTEM_COUNT);
        int totalPlayers = humans + ai;
        List<Integer> startProductions = normalizeStartProductions(startProductionPerPlayer, totalPlayers);
        int neutralMin = Math.max(1, neutralMinProduction);
        int neutralMax = Math.max(1, neutralMaxProduction);
        if (neutralMin > neutralMax) {
            int tmp = neutralMin;
            neutralMin = neutralMax;
            neutralMax = tmp;
        }
        int garrison = Math.max(1, startGarrison);
        List<String> seatColors = normalizeSeatColors(seatColorHexes, totalPlayers);
        return new GameSetup(systems, humans, ai, startProductions, neutralMin, neutralMax, garrison,
                observersAllowed, reentryAllowed, seatColors);
    }

    public int totalPlayers() {
        return humanPlayers + aiPlayers;
    }

    public int startProductionForSeat(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= startProductionPerPlayer.size()) {
            return GameConfig.DEFAULT_START_SYSTEM_PRODUCTION;
        }
        return Math.max(1, startProductionPerPlayer.get(seatIndex));
    }

    public String colorForSeat(int seatIndex) {
        if (seatIndex >= 0 && seatIndex < seatColorHexes.size()) {
            return seatColorHexes.get(seatIndex);
        }
        return GameConfig.PLAYER_PALETTE.get(Math.floorMod(seatIndex, GameConfig.PLAYER_PALETTE.size()));
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static List<Integer> normalizeStartProductions(List<Integer> values, int totalPlayers) {
        if (totalPlayers <= 0) {
            return List.of();
        }
        List<Integer> source = values == null ? Collections.emptyList() : values;
        List<Integer> out = new ArrayList<>(totalPlayers);
        for (int i = 0; i < totalPlayers; i++) {
            int value = i < source.size() ? source.get(i) : GameConfig.DEFAULT_START_SYSTEM_PRODUCTION;
            out.add(Math.max(1, value));
        }
        return List.copyOf(out);
    }

    private static String normalizeColor(@Nullable String color) {
        Set<String> palette = new HashSet<>(GameConfig.PLAYER_PALETTE);
        if (color != null && palette.contains(color)) {
            return color;
        }
        return GameConfig.PLAYER_PALETTE.getFirst();
    }

    private static List<String> normalizeSeatColors(@Nullable List<String> values, int totalPlayers) {
        List<String> requested = values == null ? List.of() : values;
        List<String> result = new ArrayList<>(totalPlayers);
        Set<String> used = new HashSet<>();
        for (int i = 0; i < totalPlayers; i++) {
            String color = normalizeColor(i < requested.size() ? requested.get(i) : null);
            if (used.contains(color)) {
                color = GameConfig.PLAYER_PALETTE.stream().filter(candidate -> !used.contains(candidate))
                        .findFirst().orElse(color);
            }
            result.add(color);
            used.add(color);
        }
        return List.copyOf(result);
    }
}
