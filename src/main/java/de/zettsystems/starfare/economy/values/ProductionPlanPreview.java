package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.EmpireStats;
import de.zettsystems.starfare.game.values.RelocationAdjustment;
import java.util.List;

/** Authorized local results alongside the consequences for the whole empire and its relocations. */
public record ProductionPlanPreview(IndustryView industry, EmpireStats empire, List<RelocationAdjustment> relocations) {
    public ProductionPlanPreview { relocations = List.copyOf(relocations); }
    public ProductionPlanPreview(IndustryView industry, EmpireStats empire) { this(industry, empire, List.of()); }
}
