package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Welche Schlachten des aktuellen Rundenberichts der Spieler schon ausgewertet hat.
 * Gespeichert wird je Konto über {@link BattleAcknowledgementService}, damit Neuladen,
 * eine neue Sitzung oder ein anderes Gerät die Auswertung weder umgeht noch wiederholt.
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
    boolean isPending(GameId gameId, TurnReport report, TurnEvent event) {
        return event.battleSystemId().isPresent()
                && firstPendingIndexOf(report, event, acknowledged(gameId, report.turn())) >= 0;
    }

    void acknowledge(GameId gameId, TurnReport report, TurnEvent event) {
        Set<Integer> acknowledged = acknowledged(gameId, report.turn());
        int eventIndex = firstPendingIndexOf(report, event, acknowledged);
        if (eventIndex < 0) {
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

    /**
     * Gleiche Events (etwa zwei identische Angriffe einer Runde) sind nicht
     * unterscheidbar; bestätigt wird jeweils das erste noch offene. Mit indexOf
     * bliebe das zweite für immer offen.
     */
    static int firstPendingIndexOf(TurnReport report, TurnEvent event, Set<Integer> acknowledgedIndices) {
        for (int eventIndex = 0; eventIndex < report.events().size(); eventIndex++) {
            if (report.events().get(eventIndex).equals(event) && !acknowledgedIndices.contains(eventIndex)) {
                return eventIndex;
            }
        }
        return -1;
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
