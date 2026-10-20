package de.zettsystems.starfare.research.values;

/** Authorized own-empire research status; never returned for another player's research. */
public record ResearchView(int turn, boolean editable, ShipResearch research, int pointsPerTurn) { }
