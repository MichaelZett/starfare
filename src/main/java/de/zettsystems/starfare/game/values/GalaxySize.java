package de.zettsystems.starfare.game.values;

import java.util.Arrays;
import java.util.Optional;

/** The galaxy sizes offered for a new game. */
public enum GalaxySize {
    SMALL(16), MEDIUM(24), LARGE(30), HUGE(40);

    public static final GalaxySize DEFAULT = MEDIUM;
    /** A size needs at least this many systems per empire, or the empires start next to each other. */
    public static final int MIN_SYSTEMS_PER_PLAYER = 2;

    private final int systems;

    GalaxySize(int systems) {
        this.systems = systems;
    }

    public int systems() {
        return systems;
    }

    /** Whether this galaxy leaves room for that many empires. */
    public boolean fits(int players) {
        return systems >= MIN_SYSTEMS_PER_PLAYER * players;
    }

    /** The offered size with exactly this many systems, if there is one. */
    public static Optional<GalaxySize> of(int systemCount) {
        return Arrays.stream(values()).filter(size -> size.systems == systemCount).findFirst();
    }

    /** The given size if it fits, otherwise the smallest one that does. */
    public static GalaxySize fitting(GalaxySize wanted, int players) {
        if (wanted.fits(players)) {
            return wanted;
        }
        return Arrays.stream(values()).filter(size -> size.fits(players)).findFirst().orElse(HUGE);
    }
}
