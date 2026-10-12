package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.EmpireStats;

/** Authorized local results alongside the consequences for the whole empire. */
public record ProductionPlanPreview(IndustryView industry, EmpireStats empire) { }
