package de.zettsystems.starfare.game.values;

/**
 * Identifies a player and their presentation color.
 */
public record Player(int id, String name, boolean ai, String colorHex) {
    public Player asAi() {
        return new Player(id, name, true, colorHex);
    }

    public Player asHuman() {
        return new Player(id, name, false, colorHex);
    }
}
