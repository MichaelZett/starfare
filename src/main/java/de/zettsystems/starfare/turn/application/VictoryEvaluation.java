package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Individual victory first, effective alliance victory second; no rounded thresholds. */
@org.springframework.stereotype.Component
final class VictoryEvaluation {
    private final ReportService reports;
    VictoryEvaluation(ReportService reports) { this.reports = reports; }
    void resolve(GameState state) {
        if (state.gameOver() || state.systems().isEmpty()) { return; }
        var counts = state.systems().stream().filter(s -> s.ownerId() != null)
                .collect(Collectors.groupingBy(StarSystem::ownerId, Collectors.counting()));
        if (!state.ruleset().usesConfigurableVictory()) {
            // Preserve the historical evaluation of simultaneous low-threshold individual wins.
            counts.forEach((player, count) -> {
                if (count * 100 >= (long) state.systems().size() * state.victorySystemPercent()) {
                    state.endGame(player);
                    announce(state, reports, List.of(player), state.victorySystemPercent());
                }
            });
            return;
        }
        var individual = counts.entrySet().stream()
                .filter(entry -> entry.getValue() * 100 >= (long) state.systems().size() * state.victorySystemPercent())
                .sorted(java.util.Map.Entry.comparingByKey()).findFirst();
        if (individual.isPresent()) {
            int winner = individual.orElseThrow().getKey(); state.endGame(winner);
            announce(state, reports, List.of(winner), state.victorySystemPercent()); return;
        }
        if (!state.victoryRules().allianceVictoryAllowed()) { return; }
        var group = state.diplomacy().groups().stream()
                .filter(g -> g.members().size() >= 2 && controlled(g.members(), counts) * 100
                        >= (long) state.systems().size() * state.victoryRules().allianceSystemPercent())
                .sorted(Comparator.comparingLong((de.zettsystems.starfare.diplomacy.values.AllianceGroup g) -> controlled(g.members(), counts))
                        .reversed().thenComparingInt(de.zettsystems.starfare.diplomacy.values.AllianceGroup::id)).findFirst();
        group.ifPresent(g -> {
            state.winAsAlliance(g.id(), g.members());
            announce(state, reports, state.outcome().winnerIds(), state.victoryRules().allianceSystemPercent());
        });
    }
    private static long controlled(java.util.Set<Integer> members, java.util.Map<Integer, Long> counts) {
        return members.stream().mapToLong(id -> counts.getOrDefault(id, 0L)).sum();
    }
    private static void announce(GameState state, ReportService reports, List<Integer> winners, int threshold) {
        var names = state.players().stream().filter(p -> winners.contains(p.id())).map(p -> p.label()).toList();
        for (var player : state.players()) {
            if (winners.contains(player.id())) {
                reports.appendEvent(state, player.id(), state.outcome().allianceVictory() ? new TurnEvent.Victory(player.id(), threshold,
                        state.outcome().allianceId(), names) : new TurnEvent.Victory(player.id(), threshold));
            } else {
                reports.appendEvent(state, player.id(), new TurnEvent.Defeat(winners.getFirst(), String.join(", ", names), winners, state.outcome().allianceId()));
            }
        }
    }
}
