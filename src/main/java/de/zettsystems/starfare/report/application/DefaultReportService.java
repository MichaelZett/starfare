package de.zettsystems.starfare.report.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultReportService implements ReportService {

    @Override
    public void appendEvent(GameState state, int playerId, TurnEvent event) {
        appendRaw(state, playerId, textFor(event), event);
    }

    @Override
    public void appendReport(GameState state, int playerId, String line) {
        appendRaw(state, playerId, line, null);
    }

    @Override
    public boolean hasSignificantEventsFor(GameState state, int playerId) {
        var rep = state.reports().get(playerId);
        return rep != null && rep.turn() == state.turn() - 1 && !rep.lines().isEmpty();
    }

    private void appendRaw(GameState state, int playerId, String line, @Nullable TurnEvent event) {
        var prev = state.reports().get(playerId);
        var lines = new ArrayList<String>();
        var events = new ArrayList<TurnEvent>();
        if (prev != null && prev.turn() == state.turn()) {
            lines.addAll(prev.lines());
            events.addAll(prev.events());
        }
        lines.add(line);
        if (event != null) {
            events.add(event);
        }
        state.reports().put(playerId, new TurnReport(state.turn(), List.copyOf(lines), List.copyOf(events)));
    }

    private static String textFor(TurnEvent e) {
        return switch (e) {
            case TurnEvent.Production p -> "Produktion: %s +%d".formatted(p.systemName(), p.amount());
            case TurnEvent.Reinforcement r ->
                    "Verstärkung: %s +%d (%s)".formatted(r.systemName(), r.ships(), r.fleetLabel());
            case TurnEvent.BattleWon b -> "Sieg bei %s: %d eigene, %d %s. %d verbleiben.".formatted(
                    b.systemName(), b.attacking(), b.defending(),
                    b.wasNeutral() ? "neutrale" : "feindliche", b.remaining());
            case TurnEvent.BattleLost b -> "Niederlage bei %s: %d eigene, %d Verteidiger. %d verbleiben.".formatted(
                    b.systemName(), b.attacking(), b.defending(), b.defendersLeft());
            case TurnEvent.SystemLost l -> "Verloren: %s fällt.".formatted(l.systemName());
            case TurnEvent.DefenseHeld d ->
                    "Gehalten: %s. %d Verteidiger verbleiben.".formatted(d.systemName(), d.defendersLeft());
            case TurnEvent.Victory _ -> "Sieg: >50%% Systeme unter Kontrolle.";
        };
    }
}
