package de.zettsystems.starfare.navigation.values;

import java.util.List;

/** Visible route names and elapsed rounds, including one round at each intermediate station. */
public record RoutePreview(List<Integer> systems, List<String> names, int rounds, double range) {
    public RoutePreview { systems = List.copyOf(systems); names = List.copyOf(names); }
    public String description() { return String.join(" → ", names); }
}
