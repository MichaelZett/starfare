package de.zettsystems.starfare.game.values;

import java.util.Map;

/** Reusable initial rules and seat reservations; never includes an old map. */
public record GameTemplate(GameSetup setup, Map<String, Integer> participants) {
    public GameTemplate { participants = Map.copyOf(participants); }
}
