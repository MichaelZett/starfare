package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.SpacewardPlanning;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.report.application.ReportService;
import org.springframework.stereotype.Service;

/** Former economy acceptance candidate W4; old checkpoints keep their complete references. */
@Service
public class EconomyPreviewSpacewardTurnEngine extends SpacewardTurnEngine {
    public EconomyPreviewSpacewardTurnEngine(RoundPipeline pipeline, EconomyService economy, SpacewardPlanning ai,
                                            FleetService fleets, ReportService reports) {
        super(pipeline, economy, ai, fleets, reports);
    }
    @Override public RulesetRef ruleset() { return RulesetRef.SPACEWARD_ECONOMY_PREVIEW; }
}
