package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

/** A known ownership change of a system, recorded at the beginning of a turn. */
public record SystemOwnership(@Nullable Integer ownerId, int sinceTurn) {
}
