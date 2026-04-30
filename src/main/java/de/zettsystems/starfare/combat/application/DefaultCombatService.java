package de.zettsystems.starfare.combat.application;

import de.zettsystems.starfare.combat.domain.CombatResolver;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected ReportService is kept by reference for the bean's lifetime by design.")
public class DefaultCombatService implements CombatService {
    private final ReportService reportService;

    public DefaultCombatService(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public void resolveAttack(GameState state, int attackerId, int toSystemId, int ships, List<Integer> localNos) {
        StarSystem target = state.getSystem(toSystemId);
        intelFor(state, attackerId).put(toSystemId, new GameState.Intel(target.ownerId(), state.turn()));

        String fleetLabel = "F" + listNos(localNos);

        if (Objects.equals(target.ownerId(), attackerId)) {
            state.updateSystem(target.id(), current -> current.reinforce(ships));
            int totalAfter = state.getSystem(target.id()).garrison();
            reportService.appendEvent(state, attackerId,
                    new TurnEvent.Reinforcement(attackerId, toSystemId, target.name(), ships, totalAfter, fleetLabel));
            return;
        }

        Integer oldOwner = target.ownerId();
        int defendersBefore = target.garrison();
        boolean neutral = oldOwner == null;

        var res = CombatResolver.resolve(ships, defendersBefore);
        if (res.attackerWon()) {
            state.updateSystem(target.id(), current -> current.captureBy(attackerId, res.attackersLeft()));

            reportService.appendEvent(state, attackerId,
                    new TurnEvent.BattleWon(attackerId, toSystemId, target.name(),
                            ships, defendersBefore, res.attackersLeft(), neutral));

            if (oldOwner != null && !Objects.equals(oldOwner, attackerId)) {
                reportService.appendEvent(state, oldOwner,
                        new TurnEvent.SystemLost(oldOwner, attackerId, toSystemId, target.name()));
                intelFor(state, oldOwner).put(toSystemId, new GameState.Intel(attackerId, state.turn()));
            }
            intelFor(state, attackerId).put(toSystemId, new GameState.Intel(attackerId, state.turn()));
        } else {
            state.updateSystem(target.id(), current -> current.afterDefense(res.defendersLeft()));
            reportService.appendEvent(state, attackerId,
                    new TurnEvent.BattleLost(attackerId, toSystemId, target.name(),
                            ships, defendersBefore, res.defendersLeft()));
            if (oldOwner != null) {
                reportService.appendEvent(state, oldOwner,
                        new TurnEvent.DefenseHeld(oldOwner, toSystemId, target.name(),
                                ships, res.defendersLeft()));
            }
        }
    }

    private static String listNos(List<Integer> nos) {
        return nos.stream().sorted().map(String::valueOf).reduce((x, y) -> x + "," + y).orElse("");
    }

    private static Map<Integer, GameState.Intel> intelFor(GameState state, int playerId) {
        return Objects.requireNonNull(state.intel().get(playerId),
                () -> "Intel map missing for player " + playerId + " (invariant: initializeState populates all players)");
    }
}
