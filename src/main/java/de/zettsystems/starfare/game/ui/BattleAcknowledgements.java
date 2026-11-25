package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.report.values.TurnReport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which battles and other located events ({@link RoundWalkthrough}) of the current round report the player has
 * confirmed. Stored per account through {@link BattleAcknowledgementService}, so a reload, a new session or another
 * device neither skips nor repeats them.
 *
 * <p>Eine Instanz gehört zu einer View. Sie lädt den Stand je Partie und Runde einmal und
 * hält ihn bis {@link #reload()}; die Views rufen das zu Beginn jedes Neuaufbaus, damit
 * die vielen Abfragen je Event nicht jedes Mal die Datenbank treffen.
 */
final class BattleAcknowledgements {

    private final BattleAcknowledgementService service;
    private final Map<String, Set<Integer>> acknowledgedByRound = new HashMap<>();

    BattleAcknowledgements(BattleAcknowledgementService service) {
        this.service = service;
    }

    /** Verwirft den geladenen Stand; der nächste Zugriff liest ihn neu. */
    void reload() {
        acknowledgedByRound.clear();
    }

    Set<Integer> pending(GameId gameId, TurnReport report) {
        return pending(report, acknowledged(gameId, report.turn()));
    }

    /** Ob gerade diese Schlacht noch offen ist — nicht bloß eine andere am selben System. */
    boolean isPending(GameId gameId, TurnReport report, int eventIndex) {
        return isPending(report, eventIndex, acknowledged(gameId, report.turn()));
    }

    /** The first located event of the round the player has not gone through yet. */
    Optional<RoundWalkthrough.Step> walkthrough(GameId gameId, TurnReport report) {
        return RoundWalkthrough.current(report, acknowledged(gameId, report.turn()));
    }

    /** Confirms a battle or another located event; any other event, or one already confirmed, is left alone. */
    void acknowledge(GameId gameId, TurnReport report, int eventIndex) {
        Set<Integer> acknowledged = acknowledged(gameId, report.turn());
        if (!RoundWalkthrough.isOpen(report, eventIndex, acknowledged)) {
            return;
        }
        UserContext.currentPlayerId()
                .ifPresent(playerId -> service.acknowledge(playerId, gameId, report.turn(), eventIndex));
        Set<Integer> updated = new HashSet<>(acknowledged);
        updated.add(eventIndex);
        acknowledgedByRound.put(key(gameId, report.turn()), Set.copyOf(updated));
    }

    /** Systeme mit mindestens einer noch nicht bestätigten Schlacht. */
    static Set<Integer> pending(TurnReport report, Set<Integer> acknowledgedIndices) {
        Set<Integer> pending = new HashSet<>();
        for (int eventIndex = 0; eventIndex < report.events().size(); eventIndex++) {
            if (!acknowledgedIndices.contains(eventIndex)) {
                report.events().get(eventIndex).battleSystemId().ifPresent(pending::add);
            }
        }
        return Set.copyOf(pending);
    }

    /** Original indices remain stable when cards are filtered or sorted. */
    static boolean isPending(TurnReport report, int eventIndex, Set<Integer> acknowledgedIndices) {
        return eventIndex >= 0 && eventIndex < report.events().size()
                && report.events().get(eventIndex).battleSystemId().isPresent()
                && !acknowledgedIndices.contains(eventIndex);
    }

    private Set<Integer> acknowledged(GameId gameId, int turn) {
        return acknowledgedByRound.computeIfAbsent(key(gameId, turn), _ -> UserContext.currentPlayerId()
                .map(playerId -> service.acknowledgedEventIndices(playerId, gameId, turn))
                .orElse(Set.of()));
    }

    private static String key(GameId gameId, int turn) {
        return gameId.value() + ":" + turn;
    }
}
