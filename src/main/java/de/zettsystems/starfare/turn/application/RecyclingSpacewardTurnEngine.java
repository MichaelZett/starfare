package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.SpacewardPlanning;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.report.application.ReportService;
import org.springframework.stereotype.Service;

/** Internal W3 checkpoint; W2 keeps its original resource rules. */
@Service
public class RecyclingSpacewardTurnEngine extends SpacewardTurnEngine {
    public RecyclingSpacewardTurnEngine(RoundPipeline pipeline, EconomyService economy, SpacewardPlanning ai,
                                        FleetService fleets, ReportService reports) {
        super(pipeline, economy, ai, fleets, reports);
    }
    @Override public RulesetRef ruleset() { return RulesetRef.SPACEWARD_RECYCLING; }
}
