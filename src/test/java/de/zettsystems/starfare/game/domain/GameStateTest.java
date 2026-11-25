package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.economy.domain.ColonyGenesis;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.economy.domain.MetalGenesis;
import de.zettsystems.starfare.economy.values.MiningAutomation;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.AttackOrder;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.SystemOwnership;
import de.zettsystems.starfare.game.values.VictoryRules;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.testsupport.NavigationStates;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameStateTest {

    @Test
    void fullConquestIsRequiredOnlyOnceEveryVictoryPathNeedsAllSystems() {
        GameState lowerGroupThreshold = NavigationStates.running(RulesetRef.SPACEWARD_ALLIANCE,
                new VictoryRules(100, true, true, 70));
        GameState groupVictoryOff = NavigationStates.running(RulesetRef.SPACEWARD_ALLIANCE,
                new VictoryRules(100, true, false, 70));

        assertThat(lowerGroupThreshold.requiresFullConquest()).isFalse();
        assertThat(groupVictoryOff.requiresFullConquest()).isTrue();

        lowerGroupThreshold.resumeForFullConquest(Instant.now());

        assertThat(lowerGroupThreshold.requiresFullConquest()).isTrue();
    }

    @Test
    void fullConquestResumesWithFreshDeadlineAndNoOldSubmissions() {
        GameState state = new GameState();
        state.start();
        state.joinedHumanPlayerIds().addAll(Set.of(1, 2));
        state.submittedThisTurn().add(1);
        state.updateStragglerClock(state.turnStartedAt());
        state.endGame(1);
        int turn = state.turn();
        Instant resumedAt = state.turnStartedAt().plusSeconds(3600);

        state.resumeForFullConquest(resumedAt);

        assertThat(state.gameOver()).isFalse();
        assertThat(state.winnerId()).isNull();
        assertThat(state.finishedAt()).isNull();
        assertThat(state.victorySystemPercent()).isEqualTo(100);
        assertThat(state.turn()).isEqualTo(turn);
        assertThat(state.turnStartedAt()).isEqualTo(resumedAt);
        assertThat(state.stragglerSince()).isNull();
        assertThat(state.submittedThisTurn()).isEmpty();
        assertThat(state.roundDeadline()).isEqualTo(resumedAt.plus(state.roundRules().roundLimit()));
    }

    @Test
    void updateSystemReplacesEntry() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));

        state.updateSystem(1, current -> current.reinforce(4));

        assertThat(state.getSystem(1).garrison()).isEqualTo(9);
    }

    @Test
    void recordsOwnershipChangesWithTheirTurn() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "S1", 0, 0, null, 5, 2, true));

        state.updateSystem(1, system -> system.colonize(1, 5, 2));
        state.nextTurn();
        state.updateSystem(1, system -> system.captureBy(2, 3));

        assertThat(state.ownershipHistory().get(1))
                .containsExactly(new SystemOwnership(1, 1), new SystemOwnership(2, 2));
    }

    @Test
    void copyOfCreatesIndependentLists() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));

        GameState copy = GameState.copyOf(state);
        state.systems().add(new StarSystem(2, "S2", 1, 1, null, 3, 2, true));

        assertThat(copy.systems()).hasSize(1);
        assertThat(copy.systems()).size().isNotEqualTo(state.systems().size());
    }

    @Test
    void resetForNewGameClearsSubmittedThisTurn() {
        GameState state = new GameState();
        state.submittedThisTurn().add(1);
        state.submittedThisTurn().add(2);

        state.resetForNewGame();

        assertThat(state.submittedThisTurn()).isEmpty();
    }

    @Test
    void copyOfCreatesIndependentSubmittedThisTurn() {
        GameState state = new GameState();
        state.submittedThisTurn().add(1);

        GameState copy = GameState.copyOf(state);
        state.submittedThisTurn().add(2);

        assertThat(copy.submittedThisTurn()).contains(1);
        assertThat(copy.submittedThisTurn()).doesNotContain(2);
        assertThat(copy.submittedThisTurn()).hasSize(1);
    }

    @Test
    void copyOfCarriesObserversAndFlags() {
        GameState state = new GameState();
        state.observers().add("alice");
        state.configureLobby(true, true);

        GameState copy = GameState.copyOf(state);
        state.observers().add("bob");

        assertThat(copy.observers()).contains("alice");
        assertThat(copy.observers()).doesNotContain("bob");
        assertThat(copy.observersAllowed()).isTrue();
        assertThat(copy.reentryAllowed()).isTrue();
    }

    @Test
    void resetForNewGameClearsPendingOrders() {
        GameState state = new GameState();
        state.pendingOrders().put(1, List.of(new FleetOrder.Send(1, 1, 2, 3)));

        state.resetForNewGame();

        assertThat(state.pendingOrders()).isEmpty();
    }

    @Test
    void copyOfCreatesIndependentPendingOrders() {
        GameState state = new GameState();
        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Send(1, 1, 2, 3))));

        GameState copy = GameState.copyOf(state);
        state.pendingOrders().get(1).add(new FleetOrder.Disband(1, 99));

        assertThat(copy.pendingOrders().get(1)).hasSize(1);
        assertThat(state.pendingOrders().get(1)).hasSize(2);
    }

    @Test
    void resetForNewGameClearsObserversAndFlags() {
        GameState state = new GameState();
        state.observers().add("alice");
        state.configureLobby(true, true);

        state.resetForNewGame();

        assertThat(state.observers()).isEmpty();
        assertThat(state.observersAllowed()).isFalse();
        assertThat(state.reentryAllowed()).isFalse();
    }

    @Test
    void turnStartedAtSurvivesSnapshotRoundTrip() {
        GameState state = new GameState();
        state.start();
        state.nextTurn();
        Instant expected = state.turnStartedAt();

        GameState restored = GameState.fromSnapshot(GameState.toSnapshot(state));

        assertThat(restored.turnStartedAt()).isEqualTo(expected);
    }

    @Test
    void replayFramesSurviveSnapshotRoundTrip() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        state.start();
        state.updateSystem(1, system -> system.reinforce(3));
        state.captureReplayFrame();

        GameState restored = GameState.fromSnapshot(GameState.toSnapshot(state));

        assertThat(restored.replayFrames()).containsKey(1);
        assertThat(restored.replayFrames().get(1).systems().getFirst().garrison()).isEqualTo(8);
    }

    @Test
    void snapshotWithoutTurnStartedAtStartsClockOnRestore() {
        GameState state = new GameState();
        state.start();
        // Referenzzeit aus dem Produktionscode statt aus der Systemuhr im Test:
        // ein Default wie Instant.EPOCH faellt damit genauso auf.
        Instant startedInOriginal = state.turnStartedAt();
        GameStateSnapshot legacy = withoutTurnStartedAt(GameState.toSnapshot(state));

        GameState restored = GameState.fromSnapshot(legacy);

        assertThat(restored.turnStartedAt())
                .isNotNull()
                .isAfterOrEqualTo(startedInOriginal);
    }

    private static GameStateSnapshot withoutTurnStartedAt(GameStateSnapshot s) {
        return new GameStateSnapshot(s.turn(), s.nextGlobalFleetId(), s.nextLocalFleetNo(), s.players(), s.systems(),
                s.fleets(), s.reports(), s.intel(), s.waitThisTurn(), s.submittedThisTurn(), s.gameOver(), s.winnerId(),
                s.started(), s.joinedHumanPlayerIds(), s.originalHumanPlayerIds(), s.observers(),
                s.seatByUser(), s.invitedSeats(), s.pendingOrders(), s.standingOrders(), s.nextStandingOrderId(),
                s.observersAllowed(), s.reentryAllowed(), null, s.visibility(), s.finishedAt(), s.ownershipHistory(),
                s.replayFrames());
    }

    @Test
    void nextTurnRestartsTheTurnClock() {
        GameState state = new GameState();
        state.start();
        Instant first = state.turnStartedAt();

        state.nextTurn();

        assertThat(state.turnStartedAt()).isAfterOrEqualTo(first);
    }

    @Test
    void systemsWithinRoundsCoversNeighboursButNotDistantSystems() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 5, 1, false));
        state.systems().add(new StarSystem(2, "B", 80, 0, null, 5, 1, true));
        state.systems().add(new StarSystem(3, "C", 3200, 0, null, 5, 1, true));

        Set<Integer> reachable = state.systemsWithinRounds(List.of(1), 2);

        assertThat(reachable).contains(1, 2).doesNotContain(3);
        assertThat(state.systemsWithinRounds(List.of(), 2)).isEmpty();
    }

    @Test
    void copyAndSnapshotRoundTripKeepEveryField() {
        GameState state = populated();
        state.makePrivate();
        state.resignedSeats().add(2);
        state.timedOutPlaces().put(1, 3);
        GameStateSnapshot snapshot = GameState.toSnapshot(state);

        assertThat(snapshot.intel()).isEqualTo(state.intel());
        assertThat(snapshot.ownershipHistory()).isEqualTo(state.ownershipHistory()).containsOnlyKeys(4);
        assertThat(state.combatRandomnessPercent()).isEqualTo(25);
        assertThat(GameState.toSnapshot(GameState.copyOf(state))).isEqualTo(snapshot);
        GameState restored = GameState.fromSnapshot(snapshot);
        assertThat(GameState.toSnapshot(restored)).isEqualTo(snapshot);
        assertThat(restored.intel()).isEqualTo(state.intel());
        assertThat(restored.resignedSeats()).containsExactly(2);
        assertThat(restored.timedOutPlaces()).containsExactly(Map.entry(1, 3));
    }

    @Test
    void keepsThePlaceATimedOutSeatHeldByTheSystemsOfAllPlayers() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.players().add(new Player(2, "P2", false, "#222222"));
        state.players().add(new Player(3, "AI", true, "#333333"));
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 10, 2, false));
        state.systems().add(new StarSystem(2, "B", 100, 0, 3, 10, 2, false));
        state.systems().add(new StarSystem(3, "C", 200, 0, 3, 10, 2, false));
        state.systems().add(new StarSystem(4, "D", 300, 0, 2, 10, 2, false));

        state.keepTimedOutPlace(1);
        state.keepTimedOutPlace(2);
        state.systems().removeIf(system -> system.id() == 2 || system.id() == 3);
        state.keepTimedOutPlace(1);

        assertThat(state.timedOutPlaces()).as("the computer opponent counts; a tie shares the place; a kept place stays")
                .containsOnly(Map.entry(1, 2), Map.entry(2, 2));
    }

    @Test
    void resetForNewGameStartsAnEmptyPrivateLobby() {
        GameState state = populated();
        state.resignedSeats().add(1);
        state.timedOutPlaces().put(2, 1);

        state.resetForNewGame();

        assertThat(state.visibility()).isEqualTo(GameVisibility.PRIVATE);
        assertThat(state.gameOver()).isFalse();
        assertThat(state.ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(state.navigationSettings()).isEmpty();
        assertThat(state.players()).isEmpty();
        assertThat(state.systems()).isEmpty();
        assertThat(state.fleets()).isEmpty();
        assertThat(state.reports()).isEmpty();
        assertThat(state.intel()).isEmpty();
        assertThat(state.ownershipHistory()).isEmpty();
        assertThat(state.replayFrames()).isEmpty();
        assertThat(state.industries()).isEmpty();
        assertThat(state.miningAutomation()).isEmpty();
        assertThat(state.resignedSeats()).isEmpty();
        assertThat(state.timedOutPlaces()).isEmpty();
        assertPerMatchDataCleared(state);
    }

    private static void assertPerMatchDataCleared(GameState state) {
        assertThat(state.waitThisTurn()).isEmpty();
        assertThat(state.submittedThisTurn()).isEmpty();
        assertThat(state.joinedHumanPlayerIds()).isEmpty();
        assertThat(state.originalHumanPlayerIds()).isEmpty();
        assertThat(state.observers()).isEmpty();
        assertThat(state.seatByUser()).isEmpty();
        assertThat(state.invitedSeats()).isEmpty();
        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.undoOrders(1)).isFalse();
        assertThat(state.standingOrders()).isEmpty();
        assertThat(state.missedRounds()).isEmpty();
        assertThat(state.observersAllowed()).isFalse();
        assertThat(state.battlePresentationEnabled()).isTrue();
        assertThat(state.combatRandomnessPercent()).isEqualTo(10);
        assertThat(state.roundRules()).isEqualTo(RoundRules.defaults());
        assertThat(state.nextLocalNoFor(1)).isOne();
        assertThat(state.nextStandingOrderIdFor(1)).isOne();
    }

    /** A running alliance game with every per-match collection filled. */
    private static GameState populated() {
        GameState state = NavigationStates.running(RulesetRef.SPACEWARD_ALLIANCE, new VictoryRules(90, true, true, 80));
        var fleets = new DefaultFleetService();
        assertThat(fleets.sendFleet(state, 1, 1, 2, 5)).isTrue();
        assertThat(fleets.queueSend(state, 1, 1, 2, 3)).isTrue();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 2)).isOne();
        state.waitThisTurn().add(state.fleets().getFirst().globalId());
        state.submittedThisTurn().add(1);
        state.observers().add("watcher");
        state.seatByUser().put("alice", 1);
        state.invitedSeats().put("bob", 2);
        state.intel().get(1).put(4, new GameState.Intel(2, 1, 5));
        new DefaultReportService().appendReport(state, 1, "report line");
        state.updateSystem(4, system -> system.captureBy(1, 3));
        state.missedRounds().put(2, 1);
        state.miningAutomation().put(1, MiningAutomation.off("alice"));
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        state.configureLobby(true, true, false, 25);
        state.configureRoundRules(new RoundRules(Duration.ofMinutes(10), Duration.ofMinutes(2), AttackOrder.STRONGEST_FIRST));
        state.publishInLobby();
        state.endGame(1);
        return state;
    }

    @Test
    void stragglerClockRunsOnlyWhileExactlyOneOfSeveralHumansIsMissing() {
        GameState state = new GameState();
        state.start();
        state.joinedHumanPlayerIds().addAll(Set.of(1, 2));
        Instant start = state.turnStartedAt();
        Instant roundEnd = start.plus(state.roundRules().roundLimit());

        state.updateStragglerClock(start.plusSeconds(5));
        assertThat(state.stragglerSince()).isNull();
        assertThat(state.roundDeadline()).isEqualTo(roundEnd);

        state.submittedThisTurn().add(1);
        state.updateStragglerClock(start.plusSeconds(10));
        state.updateStragglerClock(start.plusSeconds(20));
        assertThat(state.pendingHumanPlayerIds()).containsExactly(2);
        assertThat(state.stragglerSince()).isEqualTo(start.plusSeconds(10));
        assertThat(state.roundDeadline()).isEqualTo(start.plusSeconds(10).plus(state.roundRules().stragglerLimit()));

        state.submittedThisTurn().remove(1);
        assertThat(state.roundDeadline()).isEqualTo(roundEnd);
        state.updateStragglerClock(start.plusSeconds(30));
        assertThat(state.stragglerSince()).isNull();
    }

    @Test
    void noDeadlineRunsBeforeStartOrForASingleHuman() {
        GameState state = new GameState();
        state.joinedHumanPlayerIds().addAll(Set.of(1, 2));
        state.submittedThisTurn().add(1);
        assertThat(state.roundDeadline()).isNull();

        state.start();
        state.joinedHumanPlayerIds().remove(2);
        state.updateStragglerClock(state.turnStartedAt());

        assertThat(state.roundDeadline()).isNull();
        assertThat(state.stragglerSince()).isNull();
    }

    @Test
    void repeatedClassicEndKeepsTheFirstFinishingTime() {
        GameState state = new GameState();
        Instant first = Instant.parse("2026-01-01T10:00:00Z");

        state.endGame(1, first);
        state.endGame(2, first.plusSeconds(60));

        assertThat(state.finishedAt()).isEqualTo(first);
        assertThat(state.winnerId()).isEqualTo(2);
    }

    @Test
    void allianceVictoryMustNameTheEffectiveGroup() {
        GameState state = NavigationStates.running(RulesetRef.SPACEWARD_ALLIANCE, new VictoryRules(100, true, true, 70));
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        Set<Integer> members = Set.of(1, 2);

        assertThatThrownBy(() -> state.winAsAlliance(9, members)).isInstanceOf(IllegalArgumentException.class);
        assertThat(state.gameOver()).isFalse();
        state.winAsAlliance(1, members);
        assertThat(state.outcome().winnerIds()).containsExactly(1, 2);
    }

    @Test
    void anAllianceVictoryNeedsAtLeastTwoMembers() {
        GameState state = NavigationStates.running(RulesetRef.SPACEWARD_ALLIANCE, new VictoryRules(100, true, true, 70));
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));

        Set<Integer> alone = Set.of(1);

        assertThatThrownBy(() -> state.winAsAlliance(1, alone)).isInstanceOf(IllegalArgumentException.class);
        assertThat(state.gameOver()).isFalse();
    }

    @Test
    void spacewardActionsAreRejectedInSectorForces() {
        GameState state = new GameState();
        state.players().addAll(List.of(new Player(1, "P1", false, "#fff"), new Player(2, "P2", false, "#000")));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        DiplomacyState alliance = new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of());
        Set<Integer> members = Set.of(1, 2);
        NavigationSettings navigation = new NavigationSettings(100);

        assertThatThrownBy(() -> state.agreeTreaties(alliance)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> state.winAsAlliance(1, members)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.configureNavigation(navigation)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> state.planRecycling(1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.planShipResearch(1, 50)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.establishColonies(Map.of())).isInstanceOf(IllegalArgumentException.class);

        assertThat(state.allied(1, 2)).isFalse();
        assertThat(state.diplomacy()).isEqualTo(DiplomacyState.EMPTY);
        assertThat(state.navigationSettings()).isEmpty();
        assertThat(state.colonies()).isEmpty();
    }

    @Test
    void creationSettingsAreFixedOnceEstablished() {
        GameState prepared = ColonyStates.ready(RulesetRef.SPACEWARD_METALS);
        var metals = prepared.metals().orElseThrow();
        var colonies = prepared.colonies();
        NavigationSettings navigation = new NavigationSettings(200);
        GameState started = NavigationStates.running();

        assertThatThrownBy(() -> prepared.establishMetals(metals)).hasMessage("Metal economy is fixed at creation");
        assertThatThrownBy(() -> prepared.establishColonies(colonies)).hasMessageContaining("established once");
        assertThatThrownBy(() -> prepared.configureNavigation(navigation))
                .as("navigation already configured").isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> started.configureNavigation(navigation))
                .as("game already started").isInstanceOf(IllegalStateException.class);
        assertThat(prepared.metals()).containsSame(metals);
    }

    @Test
    void industryChangesNeedAKnownSystem() {
        GameState state = ColonyStates.ready();
        var industry = state.industries().get(1);

        assertThatThrownBy(() -> state.planIndustrialExpansion(99, industry)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.completeIndustrialProduction(99, industry, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.usableIndustrialCapacity(99)).hasMessage("Unknown industry");
        assertThat(state.industries()).doesNotContainKey(99);
    }

    @Test
    void unknownIdsAreRejectedAsArgumentErrors() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        Fleet unknown = new Fleet(9, 1, 1, 1, 1, 1, 1, 3);

        assertThatThrownBy(() -> state.updateSystem(2, system -> system)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.updatePlayer(2, player -> player)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.replaceFleet(unknown)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addFleetReturnsTheNewGlobalFleetId() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        state.systems().add(new StarSystem(2, "S2", 100, 0, null, 5, 2, true));

        assertThat(state.addFleet(1, 1, 2, 2)).isOne();
        assertThat(state.addFleet(1, 1, 2, 2)).isEqualTo(2);
    }

    @Test
    void travelTimeDependsOnTheDistanceAtAFixedSpeedNotOnThePosition() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "A", 1000, 1000, 1, 5, 1, false));
        state.systems().add(new StarSystem(2, "B", 2440, 1000, null, 5, 1, true));
        state.systems().add(new StarSystem(3, "C", 1000, 3880, null, 5, 1, true));

        // One round covers 160 map pixels on every map: 1440 take 9 rounds, 2880 take 18.
        assertThat(state.travelRounds(1, 2)).isEqualTo(9);
        assertThat(state.travelRounds(1, 3)).isEqualTo(18);
        assertThat(state.systemsWithinRounds(List.of(1), 9)).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void startCalibratesTheSpacewardRangeWhenNoneWasConfigured() {
        GameState state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 5, 2, false));
        state.systems().add(new StarSystem(2, "B", 30, 40, null, 5, 2, true));

        state.start();

        assertThat(state.navigationSettings()).contains(new NavigationSettings(50));
    }

    @Test
    void metalRulesRequireACompleteStoredEconomy() {
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_METALS));
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 5, 4, false));
        state.systems().add(new StarSystem(2, "B", 30, 40, null, 5, 2, true));
        state.initializeIndustry();
        state.establishColonies(ColonyGenesis.forGalaxy(state.systems(), 42));

        assertThatThrownBy(state::start).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Missing stored metal");
        assertThat(state.started()).isFalse();
        assertThatThrownBy(() -> GameState.toSnapshot(state)).hasMessageContaining("Missing stored metal");
        var genesis = MetalGenesis.forGalaxy(state.systems(), state.players(), state.colonies());
        var strangerStock = new MetalEconomy(genesis.deposits(), Map.of(1, 0L, 9, 0L), genesis.mining());
        assertThatThrownBy(() -> state.establishMetals(strangerStock)).hasMessageContaining("Incomplete stored metal");
    }

    @Test
    void miningAndExpansionShareTheUsableCapacity() {
        GameState state = ColonyStates.ready(RulesetRef.SPACEWARD_METALS);
        int usable = state.usableIndustrialCapacity(1);
        state.planIndustrialExpansion(1, state.industries().get(1).allocateExpansion(2));

        assertThatThrownBy(() -> state.planMining(1, usable - 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(state.metals().orElseThrow().mining()).containsEntry(1, 0);
        state.planMining(1, usable - 2);
        var moreExpansion = state.industries().get(1).allocateExpansion(3);
        assertThatThrownBy(() -> state.planIndustrialExpansion(1, moreExpansion)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void researchReservationIsTakenFromShipbuildingAndRebalancedAfterACapture() {
        GameState state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        state.start();
        int before = state.getSystem(1).productionPerTurn();

        state.planShipResearch(1, 50);

        assertThat(state.researchReservations()).containsEntry(1, 5).containsEntry(2, 2);
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(before - 5);
        // Keeping the garrison keeps the recycling ship balance intact.
        state.updateSystem(2, system -> system.captureBy(2, system.garrison()));
        assertThat(state.researchReservations()).containsEntry(2, 0);
        assertThatCode(() -> GameState.toSnapshot(state)).doesNotThrowAnyException();
    }

    @Test
    void storedItinerariesNeedShipsAndMayArriveExactlyAtTheirFinalRound() {
        GameState state = NavigationStates.running();
        assertThat(new DefaultFleetService().sendFleet(state, 1, 1, 2, 10)).isTrue();
        Fleet direct = state.fleets().getFirst();
        assertThat(direct.journey().finalArrivalTurn()).isEqualTo(direct.arrivalTurn());
        assertThatCode(() -> GameState.toSnapshot(state)).doesNotThrowAnyException();

        state.replaceFleet(new Fleet(direct.globalId(), 1, direct.localNo(), 1, 2, 0, direct.launchTurn(),
                direct.arrivalTurn(), direct.journey()));

        assertThatThrownBy(() -> GameState.toSnapshot(state)).hasMessage("Fleet and itinerary disagree");
    }

    @Test
    void systemsWithinRoundsMatchesTravelRounds() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 5, 1, false));
        state.systems().add(new StarSystem(2, "B", 60, 0, null, 5, 1, true));
        state.systems().add(new StarSystem(3, "C", 200, 0, null, 5, 1, true));

        for (int target : List.of(1, 2, 3)) {
            assertThat(state.systemsWithinRounds(List.of(1), 4).contains(target))
                    .as("System %d", target)
                    .isEqualTo(state.travelRounds(1, target) <= 4);
        }
    }

    @Test
    void closedSeatsAreResignedOrTimedOutButNotMerelyLate() {
        GameState state = new GameState();
        state.resignedSeats().add(1);
        state.missedRounds().put(2, de.zettsystems.starfare.game.values.GameConfig.MAX_MISSED_ROUNDS);
        state.missedRounds().put(3, de.zettsystems.starfare.game.values.GameConfig.MAX_MISSED_ROUNDS - 1);

        assertThat(state.closedSeats()).containsExactlyInAnyOrder(1, 2);
        assertThat(state.timedOut(2)).isTrue();
        assertThat(state.timedOut(3)).isFalse();
        assertThat(state.timedOut(1)).isFalse();
    }
}