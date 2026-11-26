package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.diplomacy.application.DiplomacyService;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.diplomacy.values.TreatyProposal;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.fleet.domain.OrderPlanning;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.fleet.values.ScheduledDeparture;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * A careless person at the map: every command a person can give in a round, in random order and with random amounts,
 * also impossible ones the rules must refuse. Uses the same entry points as the game service.
 */
final class HumanOrders {
    private final FleetService fleets;
    private final EconomyService economy;
    private final DiplomacyService diplomacy;
    private final Random random;
    private int jointAttacks;

    HumanOrders(FleetService fleets, EconomyService economy, DiplomacyService diplomacy, Random random) {
        this.fleets = fleets;
        this.economy = economy;
        this.diplomacy = diplomacy;
        this.random = random;
    }

    int jointAttacks() { return jointAttacks; }

    void plan(GameState state, Player player) {
        List<StarSystem> own = own(state, player.id());
        if (own.isEmpty()) { return; }
        int actions = 2 + random.nextInt(6);
        for (int action = 0; action < actions; action++) {
            switch (random.nextInt(14)) {
                case 0, 1, 2 -> send(state, player.id(), own);
                case 3 -> relocate(state, player.id(), own);
                case 4 -> dropRelocation(state, player.id());
                case 5, 6 -> jointAttack(state, player.id(), own);
                case 7 -> cancelDeparture(state, player.id());
                case 8 -> ShipLedger.see("undo " + (state.undoOrders(player.id()) ? "accepted" : "refused"), 1);
                case 9 -> editOrSplit(state, player.id());
                case 10 -> cancelOrder(state, player.id());
                case 11 -> waitOrDisband(state, player.id());
                case 12 -> industry(state, player.id(), own);
                default -> diplomacy(state, player.id());
            }
        }
    }

    private void send(GameState state, int player, List<StarSystem> own) {
        StarSystem from = pick(own);
        int ships = 1 + random.nextInt(Math.max(1, from.garrison() + 3));
        Integer beneficiary = random.nextInt(4) == 0 ? ally(state, player) : null;
        boolean accepted = fleets.queueSend(state, player, from.id(), pick(state.systems()).id(), ships, beneficiary);
        ShipLedger.see("send " + (accepted ? "accepted" : "refused"), 1);
    }

    private void relocate(GameState state, int player, List<StarSystem> own) {
        StarSystem from = pick(own);
        StarSystem to = random.nextBoolean() ? pick(own) : pick(state.systems());
        int headroom = fleets.routingHeadroom(state, player, from.id(), to.id());
        boolean accepted = fleets.addStandingOrder(state, player, from.id(), to.id(), 1 + random.nextInt(Math.max(1, headroom + 2))) > 0;
        ShipLedger.see("relocation " + (accepted ? "accepted" : "refused"), 1);
    }

    private void dropRelocation(GameState state, int player) {
        List<StandingOrder> standing = state.standingOrders().getOrDefault(player, List.of());
        if (!standing.isEmpty()) { fleets.removeStandingOrder(state, player, pick(standing).id()); }
    }

    /** Two or three sources; now and then the same attack twice, which must not book the same ships twice. */
    private void jointAttack(GameState state, int player, List<StarSystem> own) {
        StarSystem target = pick(state.systems());
        List<StarSystem> sources = new ArrayList<>(own);
        java.util.Collections.shuffle(sources, random);
        List<FleetDispatch> dispatches = sources.stream().limit(2 + random.nextInt(2))
                .map(source -> new FleetDispatch(source.id(), 1 + random.nextInt(Math.max(1, source.garrison() + 2))))
                .toList();
        int repeats = random.nextInt(5) == 0 ? 2 : 1;
        for (int attempt = 0; attempt < repeats; attempt++) {
            jointAttacks++;
            boolean accepted = OrderPlanning.jointAttack(state, player, target.id(), dispatches);
            ShipLedger.see("joint attack " + (attempt == 0 ? "" : "repeated ") + (accepted ? "accepted" : "refused"), 1);
        }
    }

    private void cancelDeparture(GameState state, int player) {
        List<ScheduledDeparture> departures = state.departuresOf(player);
        if (!departures.isEmpty() && OrderPlanning.cancelDeparture(state, player, pick(departures))) {
            ShipLedger.see("planned start cancelled", 1);
        }
    }

    private void editOrSplit(GameState state, int player) {
        List<FleetOrder> orders = state.pendingOrders().getOrDefault(player, List.of());
        if (orders.isEmpty()) { return; }
        int index = random.nextInt(orders.size());
        if (!(orders.get(index) instanceof FleetOrder.Send send)) { return; }
        int target = pick(state.systems()).id();
        if (random.nextBoolean()) {
            var expected = new PlannedOrder(index, "map.orderType.send", send.fromSystemId(), send.toSystemId(), "", "",
                    send.ships(), false, null, null, send.beneficiaryId());
            boolean edited = OrderPlanning.edit(state, player, expected, target, 1 + random.nextInt(send.ships() + 3));
            ShipLedger.see("edit " + (edited ? "accepted" : "refused"), 1);
            return;
        }
        // A split: part of the send goes elsewhere, sometimes more than the original had.
        int part = 1 + random.nextInt(send.ships() + 1);
        List<FleetOrder> replacement = new ArrayList<>(orders);
        replacement.set(index, new FleetOrder.Send(player, send.fromSystemId(), send.toSystemId(),
                Math.max(1, send.ships() - part), send.beneficiaryId()));
        replacement.add(new FleetOrder.Send(player, send.fromSystemId(), target, part, send.beneficiaryId()));
        ShipLedger.see("split " + (OrderPlanning.replace(state, player, replacement) ? "accepted" : "refused"), 1);
    }

    /** As the game service's cancel: remembers the step for an undo, then drops one order. */
    private void cancelOrder(GameState state, int player) {
        List<FleetOrder> orders = state.pendingOrders().get(player);
        if (orders == null || orders.isEmpty()) { return; }
        state.rememberOrders(player);
        orders.remove(random.nextInt(orders.size()));
    }

    private void waitOrDisband(GameState state, int player) {
        List<Fleet> own = state.fleets().stream().filter(fleet -> fleet.ownerId() == player).toList();
        if (own.isEmpty()) { return; }
        Fleet fleet = pick(own);
        if (random.nextBoolean()) { fleets.queueWait(state, player, fleet.globalId()); }
        else { fleets.queueDisband(state, player, fleet.globalId()); }
    }

    /** Extended rules: a new split between industry and ships, shortening relocations like the game service. */
    private void industry(GameState state, int player, List<StarSystem> own) {
        StarSystem system = pick(own);
        var industry = state.industries().get(system.id());
        if (industry == null) { return; }
        int before = state.expectedShipProduction(system.id());
        if (economy.allocateExpansion(state, player, system.id(), random.nextInt(industry.capacity() + 1))
                && state.expectedShipProduction(system.id()) < before) {
            ShipLedger.see("relocations trimmed", fleets.trimRelocations(state, player, system.id()).size());
        }
    }

    private void diplomacy(GameState state, int player) {
        List<TreatyProposal> open = state.diplomacy().proposals().stream()
                .filter(proposal -> proposal.voters().contains(player)).toList();
        int other = pick(state.players()).id();
        DiplomacyOrder order = switch (random.nextInt(4)) {
            case 0 -> new DiplomacyOrder(DiplomacyOrder.Action.FOUND, other);
            case 1 -> state.diplomacy().groups().isEmpty() ? new DiplomacyOrder(DiplomacyOrder.Action.FOUND, other)
                    : new DiplomacyOrder(DiplomacyOrder.Action.JOIN, pick(state.diplomacy().groups()).id());
            case 2 -> open.isEmpty() ? new DiplomacyOrder(DiplomacyOrder.Action.FOUND, other)
                    : new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, pick(open).id());
            default -> new DiplomacyOrder(DiplomacyOrder.Action.LEAVE,
                    state.diplomacy().groupFor(player).map(de.zettsystems.starfare.diplomacy.values.AllianceGroup::id).orElse(0));
        };
        if (diplomacy.act(state, player, order)) { ShipLedger.see("treaty " + order.action(), 1); }
    }

    private static Integer ally(GameState state, int player) {
        return state.players().stream().map(Player::id).filter(other -> state.allied(player, other)).findFirst().orElse(null);
    }

    private static List<StarSystem> own(GameState state, int player) {
        return state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player)).toList();
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }
}
