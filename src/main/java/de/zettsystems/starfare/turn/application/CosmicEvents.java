package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.combat.domain.CombatResolver;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.CosmicEvent;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PirateFleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Random events of SectorForces after Galactic Empire (1983). Runs after the arrivals of a round:
 * due suns explode first (refugees, pirates, panic jumps), landing pirates fight, then every switched-on
 * event gets its roll. Fleets already resolved this round are gone, so every fleet left is in hyperspace.
 */
@org.springframework.stereotype.Component
final class CosmicEvents {
    private final ReportService reports;
    private final Supplier<Random> random;
    /** Learns every ship an event destroys; only the ship accounting of the tests listens. */
    private final IntConsumer losses;

    @org.springframework.beans.factory.annotation.Autowired
    CosmicEvents(ReportService reports) {
        this(reports, java.util.concurrent.ThreadLocalRandom::current);
    }

    CosmicEvents(ReportService reports, Supplier<Random> random) {
        this(reports, random, _ -> { });
    }

    CosmicEvents(ReportService reports, Supplier<Random> random, IntConsumer losses) {
        this.reports = reports;
        this.random = random;
        this.losses = losses;
    }

    void resolve(GameState state) {
        SectorOptions options = state.sectorOptions();
        if (!state.ruleset().usesSectorOptions()
                || (options.isDefault() && state.cosmic().unstableSystems().isEmpty() && state.cosmic().pirates().isEmpty())) {
            return;
        }
        Random rng = random.get();
        explodeDueSuns(state, options, rng);
        for (int destroyed : state.cosmic().destroyedSystems().stream().sorted().toList()) {
            panicJumps(state, rng, destroyed);
        }
        landPirates(state, rng);
        if (strikes(rng, options, CosmicEvent.COSMIC_STORM)) { storm(state); }
        if (strikes(rng, options, CosmicEvent.SPACETIME_QUAKE)) { spacetimeQuake(state); }
        if (strikes(rng, options, CosmicEvent.DISAPPEARANCE)) { disappearance(state, rng); }
        if (strikes(rng, options, CosmicEvent.SPACEPORT_QUAKE)) { spaceportQuake(state, rng); }
        if (strikes(rng, options, CosmicEvent.SUPERNOVA)) { destabilizeSun(state, rng); }
    }

    private static boolean strikes(Random rng, SectorOptions options, CosmicEvent event) {
        double chance = options.chance(event);
        return chance > 0 && rng.nextDouble() < chance;
    }

    private void storm(GameState state) {
        if (state.fleets().isEmpty() && state.cosmic().pirates().isEmpty()) { return; }
        state.fleets().replaceAll(Fleet::delayedByOneTurn);
        state.updateCosmic(cosmic -> cosmic.movePirates(p -> p.rescheduled(p.arrivalTurn() + 1)));
        tellEveryone(state, TurnEvent.Cosmic.news(CosmicNews.STORM));
    }

    private void spacetimeQuake(GameState state) {
        if (state.fleets().isEmpty() && state.cosmic().pirates().isEmpty()) { return; }
        int next = state.turn() + 1;
        state.fleets().replaceAll(fleet -> fleet.acceleratedTo(halved(next, fleet.arrivalTurn())));
        state.updateCosmic(cosmic -> cosmic.movePirates(p -> p.rescheduled(halved(next, p.arrivalTurn()))));
        tellEveryone(state, TurnEvent.Cosmic.news(CosmicNews.SPACETIME_QUAKE));
    }

    /** Halves the rounds still to fly after the coming one; one remaining round stays one round. */
    static int halved(int nextTurn, int arrivalTurn) {
        int remaining = Math.max(0, arrivalTurn - nextTurn);
        return nextTurn + (remaining + 1) / 2;
    }

    private void disappearance(GameState state, Random rng) {
        if (state.fleets().isEmpty()) { return; }
        Fleet fleet = state.fleets().get(rng.nextInt(state.fleets().size()));
        int lost = 1 + rng.nextInt(Math.max(1, fleet.ships() / 2));
        int remaining = Math.max(0, fleet.ships() - lost);
        state.fleets().remove(fleet);
        if (remaining > 0) {
            state.fleets().add(fleet.surviveBattle(remaining));
        }
        losses.accept(fleet.ships() - remaining);
        reports.appendEvent(state, fleet.ownerId(), new TurnEvent.Cosmic(CosmicNews.DISAPPEARANCE, fleet.toSystemId(),
                name(state, fleet.toSystemId()), fleet.localNo(), fleet.ships() - remaining, remaining, 0, ""));
    }

    private void spaceportQuake(GameState state, Random rng) {
        List<StarSystem> candidates = state.systems().stream()
                .filter(system -> !state.destroyed(system.id()) && system.garrison() > 0).toList();
        if (candidates.isEmpty()) { return; }
        StarSystem system = candidates.get(rng.nextInt(candidates.size()));
        int lost = 1 + rng.nextInt(system.garrison());
        state.updateSystem(system.id(), current -> current.loseShips(lost));
        losses.accept(lost);
        Integer owner = system.ownerId();
        if (owner != null) {
            CosmicNews news = lost * 2 < system.garrison() ? CosmicNews.SPACEPORT_QUAKE_LIGHT : CosmicNews.SPACEPORT_QUAKE_HEAVY;
            reports.appendEvent(state, owner, new TurnEvent.Cosmic(news, system.id(), system.name(), 0, lost,
                    system.garrison() - lost, 0, ""));
        }
    }

    private void destabilizeSun(GameState state, Random rng) {
        var cosmic = state.cosmic();
        List<StarSystem> candidates = state.systems().stream()
                .filter(system -> !cosmic.home(system.id()) && !cosmic.destroyed(system.id()) && !cosmic.unstable(system.id()))
                .toList();
        if (candidates.isEmpty()) { return; }
        StarSystem system = candidates.get(rng.nextInt(candidates.size()));
        int explosion = state.turn() + 1;
        state.updateCosmic(current -> current.destabilize(system.id(), explosion));
        tellEveryone(state, new TurnEvent.Cosmic(CosmicNews.SUN_UNSTABLE, system.id(), system.name(), 0, 0, 0, explosion, ""));
    }

    private void explodeDueSuns(GameState state, SectorOptions options, Random rng) {
        for (int id : state.cosmic().dueToExplode(state.turn())) {
            StarSystem system = state.getSystem(id);
            state.updateCosmic(cosmic -> cosmic.explode(id));
            state.updateSystem(id, StarSystem::explode);
            tellEveryone(state, new TurnEvent.Cosmic(CosmicNews.SUPERNOVA, id, system.name(), 0, 0, 0, 0, ""));
            if (system.garrison() > 0) {
                boolean fled = randomTarget(state, rng, id).map(target -> flee(state, options, system, target)).orElse(false);
                if (!fled) { losses.accept(system.garrison()); }
            }
        }
    }

    /** Whether the garrison escaped the supernova, as refugees of its owner or as pirates. */
    private boolean flee(GameState state, SectorOptions options, StarSystem system, int target) {
        int arrival = state.turn() + 1 + state.travelRounds(system.id(), target);
        Integer owner = system.ownerId();
        if (owner != null) {
            int localNo = state.launchEventFleet(owner, system.id(), target, system.garrison(), arrival);
            reports.appendEvent(state, owner, new TurnEvent.Cosmic(CosmicNews.REFUGEES, system.id(), system.name(),
                    localNo, system.garrison(), 0, arrival, name(state, target)));
            return true;
        }
        if (options.enabled(CosmicEvent.PIRATES)) {
            state.updateCosmic(cosmic -> cosmic.launchPirates(system.id(), target, system.garrison(), arrival));
            tellEveryone(state, new TurnEvent.Cosmic(CosmicNews.PIRATES_FLEE, system.id(), system.name(), 0,
                    system.garrison(), 0, arrival, name(state, target)));
            return true;
        }
        return false;
    }

    /** Fleets bound for the destroyed system find nothing there and jump on to a random system. */
    private void panicJumps(GameState state, Random rng, int destroyed) {
        List<Fleet> bound = state.fleets().stream().filter(fleet -> fleet.toSystemId() == destroyed).toList();
        for (Fleet fleet : bound) {
            randomTarget(state, rng, destroyed).ifPresent(target -> {
                int arrival = fleet.arrivalTurn() + state.travelRounds(destroyed, target);
                state.fleets().set(state.fleets().indexOf(fleet), fleet.panicJump(target, arrival));
                reports.appendEvent(state, fleet.ownerId(), new TurnEvent.Cosmic(CosmicNews.PANIC_JUMP, destroyed,
                        name(state, destroyed), fleet.localNo(), fleet.ships(), 0, arrival, name(state, target)));
            });
        }
        if (state.systems().stream().noneMatch(system -> system.id() != destroyed && !state.destroyed(system.id()))) {
            losses.accept(state.cosmic().pirates().stream().filter(pirates -> pirates.toSystemId() == destroyed)
                    .mapToInt(PirateFleet::ships).sum());
            state.updateCosmic(cosmic -> cosmic.dropPirates(pirates -> pirates.toSystemId() == destroyed));
            return;
        }
        state.updateCosmic(cosmic -> cosmic.movePirates(pirates -> pirates.toSystemId() == destroyed
                ? jumpOn(state, rng, destroyed, pirates) : pirates));
    }

    private static PirateFleet jumpOn(GameState state, Random rng, int destroyed, PirateFleet pirates) {
        int target = randomTarget(state, rng, destroyed).orElseThrow();
        return pirates.panicJump(target, pirates.arrivalTurn() + state.travelRounds(destroyed, target));
    }

    private void landPirates(GameState state, Random rng) {
        int next = state.turn() + 1;
        List<PirateFleet> landing = new ArrayList<>(state.cosmic().pirates().stream()
                .filter(pirates -> pirates.arrivalTurn() <= next).toList());
        state.updateCosmic(cosmic -> cosmic.dropPirates(pirates -> pirates.arrivalTurn() <= next));
        landing.forEach(pirates -> raid(state, rng, pirates));
    }

    private void raid(GameState state, Random rng, PirateFleet pirates) {
        int target = pirates.toSystemId();
        if (state.destroyed(target)) {
            losses.accept(pirates.ships());
            return;
        }
        StarSystem system = state.getSystem(target);
        var result = CombatResolver.resolve(pirates.ships(), system.garrison(), state.combatRandomnessPercent(), rng::nextDouble);
        Integer owner = system.ownerId();
        if (result.attackerWon()) {
            state.updateSystem(target, current -> current.fallToPirates(result.attackersLeft()));
        } else {
            state.updateSystem(target, current -> current.afterDefense(result.defendersLeft()));
        }
        losses.accept(pirates.ships() + system.garrison() - (result.attackerWon() ? result.attackersLeft() : result.defendersLeft()));
        if (owner != null) {
            reports.appendEvent(state, owner, new TurnEvent.Cosmic(
                    result.attackerWon() ? CosmicNews.PIRATE_RAID_WON : CosmicNews.PIRATE_RAID_REPELLED, target,
                    system.name(), 0, pirates.ships(),
                    result.attackerWon() ? result.attackersLeft() : result.defendersLeft(), 0, ""));
        }
    }

    private static Optional<Integer> randomTarget(GameState state, Random rng, int except) {
        List<StarSystem> candidates = state.systems().stream()
                .filter(system -> system.id() != except && !state.destroyed(system.id())).toList();
        return candidates.isEmpty() ? Optional.empty()
                : Optional.of(candidates.get(rng.nextInt(candidates.size())).id());
    }

    private void tellEveryone(GameState state, TurnEvent event) {
        for (Player player : state.players()) {
            reports.appendEvent(state, player.id(), event);
        }
    }

    private static String name(GameState state, int systemId) {
        return state.getSystem(systemId).name();
    }
}
