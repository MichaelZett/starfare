package de.zettsystems.starfare.navigation.values;

import java.util.List;

/** Visible route names and elapsed rounds, including one round at each intermediate station. */
public record RoutePreview(List<Integer> systems, List<String> names, int rounds, double range, List<Integer> legRounds) {
    public RoutePreview(List<Integer> systems, List<String> names, int rounds, double range) {
        this(systems, names, rounds, range, List.of());
    }
    public RoutePreview { systems = List.copyOf(systems); names = List.copyOf(names); legRounds = List.copyOf(legRounds); }
    public String description() { return String.join(" → ", names); }
}
