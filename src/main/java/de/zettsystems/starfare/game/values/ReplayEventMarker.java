package de.zettsystems.starfare.game.values;

/** A navigable combat or conquest event in one replay frame. */
public record ReplayEventMarker(int turn, int systemId, String systemName, int eventIndex, boolean conquest) {}
