package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.StarSystem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Start balance by position after Galactic Empire (1983): an empire that starts close to its opponents
 * gets more ships, one with many neutral systems nearby gets fewer. Only the home garrisons change, and
 * the adjustments add up to roughly zero.
 */
public final class StartBalance {
    /** Weight and distance offset of a nearby opponent: threat raises the garrison. */
    static final double ENEMY_WEIGHT = 300;
    static final double ENEMY_OFFSET = 6;
    /** Weight and distance offset of a nearby neutral system: easy growth lowers the garrison. */
    static final double NEUTRAL_WEIGHT = 100;
    static final double NEUTRAL_OFFSET = 3;
    /** Galactic Empire measured distance in light years; one travel round counts as three. */
    static final double DISTANCE_PER_ROUND = 3;

    private StartBalance() {
    }

    public static void apply(GameState state, List<Integer> homes) {
        if (homes.size() < 2) { return; }
        Map<Integer, Double> pressure = new LinkedHashMap<>();
        homes.forEach(home -> pressure.put(home, pressure(state, home, homes)));
        double mean = pressure.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        pressure.forEach((home, value) -> state.updateSystem(home, system -> balanced(system, value - mean)));
    }

    /** Threat minus easy growth around a home system. */
    static double pressure(GameState state, int home, List<Integer> homes) {
        double value = 0;
        for (StarSystem other : state.systems()) {
            if (other.id() == home) { continue; }
            double distance = DISTANCE_PER_ROUND * state.travelRounds(home, other.id());
            if (homes.contains(other.id())) {
                value += ENEMY_WEIGHT / (distance + ENEMY_OFFSET);
            } else if (other.ownerId() == null) {
                value -= NEUTRAL_WEIGHT / (distance + NEUTRAL_OFFSET);
            }
        }
        return value;
    }

    /** Keeps every garrison between half and twice the regular start so no seat is decided at once. */
    private static StarSystem balanced(StarSystem home, double bonus) {
        int base = home.garrison();
        int garrison = Math.clamp(Math.round(base + bonus), Math.max(1, (base + 1) / 2), Math.max(1, base * 2));
        return home.colonize(Objects.requireNonNull(home.ownerId()), garrison, home.productionPerTurn());
    }
}
