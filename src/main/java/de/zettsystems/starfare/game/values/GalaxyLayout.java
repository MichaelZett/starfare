package de.zettsystems.starfare.game.values;

/** Wie Systempositionen und Heimatsysteme über die Karte verteilt werden. */
public enum GalaxyLayout {
    /** Rein zufällige Positionen; erzeugt Ballungen und Leerräume. */
    RANDOM,
    /** Ein System je Rasterzelle mit Zufallsversatz, Heimatsysteme maximal auseinander. */
    EVEN
}
