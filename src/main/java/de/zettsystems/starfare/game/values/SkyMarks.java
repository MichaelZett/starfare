package de.zettsystems.starfare.game.values;

import java.util.Set;

/** What every empire knows about the sky of a SectorForces galaxy: destroyed systems and unstable suns. */
public record SkyMarks(Set<Integer> destroyed, Set<Integer> unstable) {
    public static final SkyMarks NONE = new SkyMarks(Set.of(), Set.of());

    public SkyMarks {
        destroyed = Set.copyOf(destroyed);
        unstable = Set.copyOf(unstable);
    }

    public static SkyMarks of(CosmicState cosmic) {
        return new SkyMarks(cosmic.destroyedSystems(), cosmic.unstableSystems().keySet());
    }
}