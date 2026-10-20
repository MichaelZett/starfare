package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCoalitionCombatService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.turn.application.EqualMetalSpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Paired economics with a fixed CONCENTRATION military heuristic for the investing seat. */
final class InvestmentMatch {
    record Result(String outcome, int winner, int rounds, long invested, int grown, int acquired,
                  int firstInvestment, int firstCompletion, String digest) { }
    private InvestmentMatch() { }

    static Result play(Scenario scenario, InvestmentModel model, InvestmentPolicy policy, AiStrategy opponent,
                       int investor, int id, StringBuilder metrics) {
        var state = scenario.create(2);
        var economy = new InvestmentEconomy(model);
        var fleets = new DefaultFleetService();
        var reports = new DefaultReportService();
        var navigation = new DefaultNavigationService();
        var combat = new Random();
        var order = new Random();
        var pipeline = new RoundPipeline(new DefaultCombatService(reports, combat::nextDouble), reports, fleets, navigation,
                new DefaultDiplomacyService(), new DefaultCoalitionCombatService(reports, navigation, _ -> combat.nextDouble()), () -> order);
        var engine = new EqualMetalSpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports);
        var planning = new EconomyProfiles();
        var own = new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, EconomyProfiles.Policy.STANDARD);
        var other = new EconomyProfiles.Profile(opponent, EconomyProfiles.Policy.STANDARD);
        var profiles = investor == 1 ? List.of(own, other) : List.of(other, own);
        var targets = new HashMap<Integer, Integer>();
        long invested = 0;
        int gained = 0;
        int acquired = 0;
        int firstInvestment = 0;
        int firstCompletion = 0;
        int resolved = 0;
        var digest = digest();
        while (!state.gameOver() && resolved < scenario.roundLimit()) {
            planning.plan(state, profiles);
            policy.plan(state, investor, model, targets);
            EconomyProfiles.validate(state);
            var forecast = state.metalPreview();
            var planned = state;
            for (int player = 1; player <= 2; player++) {
                int owner = player;
                var owned = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), owner)).toList();
                int expansion = owned.stream().mapToInt(system -> stateIndustry(planned, system.id()).expansionAllocation()).sum();
                int capacity = owned.stream().mapToInt(system -> stateIndustry(planned, system.id()).capacity()).sum();
                int built = owned.stream().mapToInt(system -> forecast.ships().get(system.id())).sum();
                long ships = owned.stream().mapToLong(system -> system.garrison()).sum()
                        + state.fleets().stream().filter(fleet -> fleet.ownerId() == owner).mapToLong(fleet -> fleet.ships()).sum();
                if (player == investor) {
                    invested += expansion;
                    if (expansion > 0 && firstInvestment == 0) { firstInvestment = state.turn(); }
                }
                metrics.append(id).append(',').append(state.turn()).append(',').append(player).append(',').append(owned.size())
                        .append(',').append(capacity).append(',').append(ships).append(',').append(expansion).append(',').append(built)
                        .append(',').append(state.metals().orElseThrow().stocks().get(player)).append('\n');
            }
            combat.setSeed(SimulationEngine.mix(scenario.combatSeed(), state.turn()));
            order.setSeed(SimulationEngine.mix(scenario.combatSeed() ^ 0x6a09e667f3bcc909L, state.turn()));
            int turn = state.turn();
            engine.advanceTurn(state);
            state = economy.finish(state);
            int completed = economy.built.getOrDefault(investor, 0);
            gained += completed;
            acquired += economy.acquired.getOrDefault(investor, 0);
            if (completed > 0 && firstCompletion == 0) { firstCompletion = turn; }
            if (RecyclingOrders.balanceTenths(state) != Objects.requireNonNull(state.metals().orElseThrow().recycling()).initialTenths()) {
                throw new IllegalStateException("Counterfactual violated metal conservation");
            }
            digest.update(EconomyBenchmark.canonical(state).getBytes(StandardCharsets.UTF_8));
            resolved++;
            if (resolved % 25 == 0) { state = GameState.fromSnapshot(GameState.toSnapshot(state)); }
        }
        return new Result(state.gameOver() ? "WIN" : "ROUND_LIMIT", state.winnerId() == null ? 0 : state.winnerId(),
                resolved, invested, gained, acquired, firstInvestment, firstCompletion, HexFormat.of().formatHex(digest.digest()));
    }

    private static de.zettsystems.starfare.economy.domain.Industry stateIndustry(GameState state, int id) {
        return state.industries().get(id);
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
