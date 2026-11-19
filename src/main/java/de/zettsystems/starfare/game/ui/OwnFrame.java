package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.config.StandaloneStart;

/**
 * Whether Starfare runs on its own or inside a platform host, as decided by {@link StandaloneStart}.
 * The parts of the own frame themselves come from {@link OwnFrameParts}.
 */
public final class OwnFrame {
    private OwnFrame() {
    }

    public static boolean active() {
        return StandaloneStart.active();
    }
}
