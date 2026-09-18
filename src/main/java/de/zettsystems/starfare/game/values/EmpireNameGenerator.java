package de.zettsystems.starfare.game.values;

import java.util.List;

/** Names used for computer-controlled empires and as an initial suggestion for human players. */
public final class EmpireNameGenerator {
    private static final List<String> AI_EMPIRE_NAMES = List.of(
            "Aurelian Concord", "Vesper Syndicate", "Helios Compact", "Orion Directorate",
            "Lyra Commonwealth", "Novan Hegemony", "Atlas Combine", "Kepler Ascendancy",
            "Selene Coalition", "Mira Assembly", "Vega Republic", "Sagan Collective");

    private EmpireNameGenerator() {
    }

    public static String forAi(int index) {
        String name = AI_EMPIRE_NAMES.get(Math.floorMod(index - 1, AI_EMPIRE_NAMES.size()));
        int series = Math.floorDiv(index - 1, AI_EMPIRE_NAMES.size());
        return series == 0 ? name : name + " " + (series + 1);
    }

    public static String forHuman(String playerName) {
        return playerName == null || playerName.isBlank() ? "New Empire" : playerName.trim() + " Empire";
    }
}
