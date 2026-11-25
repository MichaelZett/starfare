package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.SpacewardPlanning;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.economy.values.IndustrialProduction;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;

/** Extended rules 2.0.0: industry expansion, limited range, alliances and coalition combat. */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are retained for the lifetime of this stateless bean.")
public class SpacewardTurnEngine implements RulesetRoundImplementation {
    private final RoundPipeline pipeline;
    private final EconomyService economy;
    private final SpacewardPlanning ai;
    private final FleetService fleets;
    private final ReportService reports;
    public SpacewardTurnEngine(RoundPipeline pipeline, EconomyService economy, SpacewardPlanning ai,
                               FleetService fleets, ReportService reports) {
        this.pipeline = pipeline; this.economy = economy; this.ai = ai; this.fleets = fleets; this.reports = reports;
    }
    @Override public RulesetRef ruleset() { return RulesetRef.EXTENDED; }
    @Override public void advanceTurn(GameState state) {
        if (!ruleset().equals(state.ruleset())) { throw new IllegalArgumentException("Extended rules cannot resolve " + state.ruleset()); }
        pipeline.advanceTurn(state, () -> ai.plan(state, fleets), routed -> {
            var results = economy.produce(state, routed);
            results.forEach(result -> reports.appendEvent(state, result.ownerId(),
                    new TurnEvent.Production(result.ownerId(), result.systemId(), result.systemName(), result.ships())));
            recordEconomy(state, results);
        });
    }

    /** One summary per participant and resolved round, zero included, captured before combat. */
    private void recordEconomy(GameState state, List<IndustrialProduction> results) {
        for (Player player : state.players()) {
            int gained = results.stream().filter(result -> result.ownerId() == player.id())
                    .mapToInt(IndustrialProduction::capacityGained).sum();
            reports.appendEvent(state, player.id(), new TurnEvent.EconomySummary(player.id(), gained));
        }
    }
}
