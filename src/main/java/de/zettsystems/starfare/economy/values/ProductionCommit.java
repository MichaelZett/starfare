package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.RelocationAdjustment;
import java.util.List;

/** Outcome of an immediately saved production change, including relocations it shortened. */
public record ProductionCommit(boolean accepted, List<RelocationAdjustment> relocations) {
    public static final ProductionCommit REJECTED = new ProductionCommit(false, List.of());
    public ProductionCommit { relocations = List.copyOf(relocations); }
}
