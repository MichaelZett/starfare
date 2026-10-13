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
        if (!state.victoryRules().aiAlliancesAllowed() && bringsInAi(state, player, order)) { return false; }
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
                    boolean refused = !state.victoryRules().aiAlliancesAllowed() && newAiParty(state, proposal);
                    act(state, player.id(), new DiplomacyOrder(leaving || refused ? DiplomacyOrder.Action.REJECT : DiplomacyOrder.Action.APPROVE, proposal.id(), 0));
                }
            }
        }
    }

    /**
     * Founding or joining that would make an AI empire a new alliance party. Treaty
     * management of existing groups (notice changes, rejections, departures) stays open.
     */
    private static boolean bringsInAi(GameState state, int player, DiplomacyOrder order) {
        return switch (order.action()) {
            case FOUND -> isAi(state, player) || isAi(state, order.subject());
            case JOIN -> isAi(state, player);
            case APPROVE -> state.diplomacy().proposals().stream().filter(proposal -> proposal.id() == order.subject())
                    .anyMatch(proposal -> newAiParty(state, proposal));
            case NOTICE, REJECT, LEAVE -> false;
        };
    }

    private static boolean newAiParty(GameState state, de.zettsystems.starfare.diplomacy.values.TreatyProposal proposal) {
        return switch (proposal.kind()) {
            case FOUND -> proposal.voters().stream().anyMatch(voter -> isAi(state, voter));
            case JOIN -> isAi(state, proposal.candidate());
            case NOTICE -> false;
        };
    }

    private static boolean isAi(GameState state, int player) {
        return state.players().stream().anyMatch(candidate -> candidate.id() == player && candidate.ai());
    }
}
