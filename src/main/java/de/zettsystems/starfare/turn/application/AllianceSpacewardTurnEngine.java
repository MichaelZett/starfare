package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.SpacewardPlanning;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.report.application.ReportService;
import org.springframework.stereotype.Service;

/** Versioned entry point for configurable diplomacy and group victory. */
@Service
public class AllianceSpacewardTurnEngine extends SpacewardTurnEngine {
    public AllianceSpacewardTurnEngine(RoundPipeline pipeline, EconomyService economy, SpacewardPlanning ai,
                                      FleetService fleets, ReportService reports) {
        super(pipeline, economy, ai, fleets, reports);
    }
    @Override public RulesetRef ruleset() { return RulesetRef.SPACEWARD_ALLIANCE; }
}
