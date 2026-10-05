package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.AiService;
import de.zettsystems.starfare.combat.application.CombatService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import org.springframework.stereotype.Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are retained for the lifetime of this stateless bean.")
public class DefaultTurnEngine implements RulesetRoundImplementation {
    private final AiService ai;
    private final FleetService fleets;
    private final ReportService reportService;
    private final RoundPipeline pipeline;
    @org.springframework.beans.factory.annotation.Autowired
    public DefaultTurnEngine(CombatService combat, AiService ai, ReportService reports, FleetService fleets) {
        this(new RoundPipeline(combat, reports, fleets), ai, reports, fleets);
    }

    public DefaultTurnEngine(RoundPipeline pipeline, AiService ai, ReportService reports, FleetService fleets) {
        this.ai = ai; this.fleets = fleets; this.reportService = reports;
        this.pipeline = pipeline;
    }
    @Override public RulesetRef ruleset() { return RulesetRef.SECTOR_FORCES; }
    @Override public void advanceTurn(GameState state) {
        if (!ruleset().equals(state.ruleset())) { throw new IllegalArgumentException("SectorForces cannot resolve " + state.ruleset()); }
        pipeline.advanceTurn(state, () -> ai.planAiTurns(state, fleets), routed -> applyProduction(state, routed));
    }

    private void applyProduction(GameState state, Map<Integer, Integer> routedShips) {
        for (StarSystem s : state.systems()) {
            Integer ownerId = s.ownerId();
            if (ownerId == null || s.neutral()) {
                continue;
            }
            // Verlegungen sind bereits abgeflossen; was uebrig bleibt, geht in die Garnison.
            int routed = routedShips.getOrDefault(s.id(), 0);
            if (routed > 0) {
                state.updateSystem(s.id(), current -> current.produceAndRoute(routed));
            } else {
                state.updateSystem(s.id(), StarSystem::produce);
            }
            reportService.appendEvent(state, ownerId,
                    new TurnEvent.Production(ownerId, s.id(), s.name(), s.productionPerTurn()));
        }
    }

}
