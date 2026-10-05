package de.zettsystems.starfare.diplomacy.application;

import de.zettsystems.starfare.diplomacy.domain.Treaties;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.navigation.domain.Routes;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DefaultDiplomacyService implements DiplomacyService {
    @Override public boolean act(GameState state, int player, DiplomacyOrder order) {
        if (!state.victoryRules().alliancesAllowed() || !Routes.limited(state) || !state.started() || state.gameOver()) { return false; }
        var result = Treaties.apply(state.diplomacy(), player, order, state.turn(), state.players().stream().map(Player::id).collect(Collectors.toSet()));
        result.ifPresent(state::agreeTreaties);
        return result.isPresent();
    }
    @Override public void beginRound(GameState state, int turn) {
        if (!Routes.limited(state) || !state.victoryRules().alliancesAllowed()) { return; }
        var previous = state.diplomacy();
        state.agreeTreaties(Treaties.beginRound(previous, turn));
        for (var fleet : java.util.List.copyOf(state.fleets())) {
            Integer owner = state.getSystem(fleet.toSystemId()).ownerId();
            if (owner != null && previous.allied(fleet.ownerId(), owner) && !state.allied(fleet.ownerId(), owner)) {
                var journey = fleet.journey();
                if (journey == null) { continue; }
                state.replaceFleet(fleet.inFlight() ? fleet.beginEvacuation() : fleet.requireReturn());
                state.waitThisTurn().remove(fleet.globalId());
            }
        }
    }
    @Override public void answerAiProposals(GameState state) {
        for (var player : state.players().stream().filter(Player::ai).toList()) {
            for (var proposal : java.util.List.copyOf(state.diplomacy().proposals())) {
                if (proposal.voters().contains(player.id()) && !proposal.approvals().contains(player.id())) {
                    boolean leaving = state.diplomacy().groupFor(player.id()).map(group -> group.departures().containsKey(player.id())).orElse(false);
                    act(state, player.id(), new DiplomacyOrder(leaving ? DiplomacyOrder.Action.REJECT : DiplomacyOrder.Action.APPROVE, proposal.id(), 0));
                }
            }
        }
    }
}
