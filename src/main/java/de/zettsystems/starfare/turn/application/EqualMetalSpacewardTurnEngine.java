package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.SpacewardPlanning;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.report.application.ReportService;
import org.springframework.stereotype.Service;

/** Equal metal sharing is versioned independently of saved W5 games. */
@Service
public class EqualMetalSpacewardTurnEngine extends SpacewardTurnEngine {
    public EqualMetalSpacewardTurnEngine(RoundPipeline pipeline, EconomyService economy, SpacewardPlanning ai,
                                        FleetService fleets, ReportService reports) {
        super(pipeline, economy, ai, fleets, reports);
    }
    @Override public RulesetRef ruleset() { return RulesetRef.SPACEWARD_EQUAL_METAL; }
}
