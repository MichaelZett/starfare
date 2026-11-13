package de.zettsystems.starfare.game.ui;

import org.springframework.util.ClassUtils;

/**
 * Starfare's own start shows the account menu, the language switch and the legal footer on
 * its pages. Inside a platform host the host's frame shows them instead. The package
 * {@code standalone} exists only in the own start (gradle/game-module.gradle leaves it out
 * of the game artifact), so its presence tells the two apart without any setting.
 */
public final class OwnFrame {
    static final String STANDALONE_MARKER = "de.zettsystems.starfare.standalone.ui.StandaloneEntryView";
    private static final boolean ACTIVE = ClassUtils.isPresent(STANDALONE_MARKER, OwnFrame.class.getClassLoader());

    private OwnFrame() {
    }

    public static boolean active() {
        return ACTIVE;
    }
}
