package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.config.StandaloneStart;

/**
 * Starfare's own start shows the account menu, the language switch and the legal footer on
 * its pages. Inside a platform host the host's frame shows them instead. Which of the two
 * applies decides {@link StandaloneStart}.
 */
public final class OwnFrame {
    private OwnFrame() {
    }

    public static boolean active() {
        return StandaloneStart.active();
    }
}
