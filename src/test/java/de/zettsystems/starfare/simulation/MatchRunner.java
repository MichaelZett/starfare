package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.report.values.TurnEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

final class MatchRunner {
    private MatchRunner() { }

    static MatchResult play(Scenario scenario, List<Strategy> strategies) {
        return continueFrom(scenario, strategies, scenario.create(strategies.size()), "normal", SimulationEngine.Intervention.NONE);
    }

    static MatchResult continueFrom(Scenario scenario, List<Strategy> strategies, GameState checkpoint,
                                    String experiment, SimulationEngine.Intervention intervention) {
        if (checkpoint.gameOver() || !scenario.rules().equals(checkpoint.ruleset())) {
            throw new IllegalArgumentException("Checkpoint must be running with matching rules");
        }
        var state = GameState.fromSnapshot(GameState.toSnapshot(checkpoint));
        var engine = new SimulationEngine();
        var metrics = new ArrayList<MatchResult.RoundMetric>();
        var orders = new ArrayList<String>();
        long[] produced = new long[strategies.size()];
        long[] losses = new long[strategies.size()];
        long[] invested = new long[strategies.size()];
        var digest = sha256();
        recordState(state, metrics, produced, losses, invested, digest);
        boolean first = true;
        while (!state.gameOver() && state.turn() <= scenario.roundLimit()) {
            long[] before = state.players().stream().mapToLong(p -> ships(state, p.id())).toArray();
            int round = state.turn();
            var plans = engine.advance(state, strategies, scenario.combatSeed(), first ? intervention : SimulationEngine.Intervention.NONE);
            first = false;
            for (int seat = 0; seat < strategies.size(); seat++) {
                int player = seat + 1;
                var report = state.reports().get(player);
                long built = report == null || report.turn() != round ? 0 : report.events().stream()
                        .filter(TurnEvent.Production.class::isInstance).map(TurnEvent.Production.class::cast)
                        .mapToLong(TurnEvent.Production::amount).sum();
                produced[seat] += built;
                losses[seat] += before[seat] + built - ships(state, player);
                invested[seat] += plans.get(seat).expansion().values().stream().mapToLong(Integer::longValue).sum();
                logOrders(orders, round, player, plans.get(seat));
            }
            recordState(state, metrics, produced, losses, invested, digest);
            // The harness keeps its own trace; archived UI replay history is unnecessary here.
            state.replayFrames().clear();
        }
        String outcome = state.gameOver() ? (state.outcome().hasWinner() ? "WIN" : "DRAW") : "ROUND_LIMIT";
        return new MatchResult(scenario, strategies, experiment, outcome,
                state.winnerId() == null ? 0 : state.winnerId(), metrics, orders, HexFormat.of().formatHex(digest.digest()), state.outcome().winnerIds(), state.outcome().allianceId());
    }

    static GameState checkpoint(Scenario scenario, List<Strategy> strategies, int round) {
        if (round < 1 || round > scenario.roundLimit()) { throw new IllegalArgumentException("Invalid fork round"); }
        var state = scenario.create(strategies.size());
        var engine = new SimulationEngine();
        while (state.turn() < round && !state.gameOver()) {
            engine.advance(state, strategies, scenario.combatSeed(), SimulationEngine.Intervention.NONE);
            state.replayFrames().clear();
        }
        return state;
    }

    private static void logOrders(List<String> rows, int round, int player, StrategyOrders orders) {
        orders.sends().forEach(send -> rows.add(round + "," + player + ",send," + send.fromSystemId()
                + "," + send.toSystemId() + "," + send.ships()));
        new TreeMap<>(orders.expansion()).forEach((system, amount) -> rows.add(round + "," + player + ",expand," + system + ",," + amount));
    }

    private static void recordState(GameState state, List<MatchResult.RoundMetric> metrics, long[] produced,
                                    long[] losses, long[] invested, MessageDigest digest) {
        for (var player : state.players()) {
            var owned = state.systems().stream().filter(s -> Objects.equals(s.ownerId(), player.id())).toList();
            long capacity = owned.stream().mapToLong(s -> state.industries().containsKey(s.id())
                    ? state.industries().get(s.id()).capacity() : s.productionPerTurn()).sum();
            int seat = player.id() - 1;
            metrics.add(new MatchResult.RoundMetric(state.turn() - 1, player.id(), owned.size(), ships(state, player.id()),
                    capacity, produced[seat], losses[seat], invested[seat]));
        }
        String canonical = state.turn() + "|" + state.systems() + "|" + state.fleets() + "|"
                + new TreeMap<>(state.industries()) + "|" + new TreeMap<>(state.reports()) + "|"
                + diplomacyFingerprint(state) + "|" + state.outcome().winnerIds() + "|" + state.outcome().allianceId() + "\n";
        digest.update(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private static long ships(GameState state, int player) {
        return state.systems().stream().filter(s -> Objects.equals(s.ownerId(), player)).mapToLong(s -> s.garrison()).sum()
                + state.fleets().stream().filter(f -> f.ownerId() == player).mapToLong(f -> f.ships()).sum();
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private static String diplomacyFingerprint(GameState state) {
        var value = new StringBuilder().append(state.diplomacy().nextId());
        state.diplomacy().groups().stream().sorted(java.util.Comparator.comparingInt(group -> group.id())).forEach(group ->
                value.append('|').append(group.id()).append(':').append(new java.util.TreeSet<>(group.members()))
                        .append(':').append(group.noticeRounds()).append(':').append(new TreeMap<>(group.departures())));
        state.diplomacy().proposals().stream().sorted(java.util.Comparator.comparingInt(proposal -> proposal.id())).forEach(proposal ->
                value.append('|').append(proposal.id()).append(':').append(proposal.kind()).append(':').append(proposal.groupId())
                        .append(':').append(proposal.candidate()).append(':').append(proposal.noticeRounds())
                        .append(':').append(new java.util.TreeSet<>(proposal.voters())).append(':').append(new java.util.TreeSet<>(proposal.approvals())));
        return value.toString();
    }
}
