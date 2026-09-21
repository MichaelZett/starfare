package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

/**
 * Identifies a player and their presentation color.
 */
public record Player(int id, String name, boolean ai, String colorHex, @Nullable String empireName) {
    public Player(int id, String name, boolean ai, String colorHex) {
        this(id, name, ai, colorHex, name);
    }

    public Player asAi() {
        return new Player(id, name, true, colorHex, empireName);
    }

    public Player asHuman() {
        return new Player(id, name, false, colorHex, empireName);
    }

    public Player identifiedAs(String playerName, String selectedEmpireName) {
        String resolvedPlayerName = playerName == null || playerName.isBlank() ? name : playerName.trim();
        String resolvedEmpireName = selectedEmpireName == null || selectedEmpireName.isBlank()
                ? empireNameOrName() : selectedEmpireName.trim();
        return new Player(id, resolvedPlayerName, false, colorHex, resolvedEmpireName);
    }

    public String empireNameOrName() {
        return empireName == null || empireName.isBlank() ? name : empireName;
    }

    public String label() {
        return empireNameOrName().equals(name) ? name : empireNameOrName() + " (" + name + ")";
    }
}
