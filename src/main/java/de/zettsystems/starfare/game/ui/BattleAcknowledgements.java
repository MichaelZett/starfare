package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;

import java.util.HashSet;
import java.util.Set;

/** Keeps battle-result acknowledgement local to the player's current browser session. */
final class BattleAcknowledgements {
    private static final String SESSION_KEY = "starfare.acknowledgedBattles";

    private BattleAcknowledgements() {
    }

    static Set<Integer> pending(GameId gameId, TurnReport report) {
        return pending(gameId, report, acknowledged());
    }

    static Set<Integer> pending(GameId gameId, TurnReport report, Set<String> acknowledged) {
        Set<Integer> pending = new HashSet<>();
        for (int eventIndex = 0; eventIndex < report.events().size(); eventIndex++) {
            TurnEvent event = report.events().get(eventIndex);
            int currentIndex = eventIndex;
            event.battleSystemId().ifPresent(systemId -> {
                if (!acknowledged.contains(key(gameId, report.turn(), currentIndex))) {
                    pending.add(systemId);
                }
            });
        }
        return Set.copyOf(pending);
    }

    static void acknowledge(GameId gameId, TurnReport report, TurnEvent event) {
        VaadinSession.getCurrent().setAttribute(SESSION_KEY,
                acknowledge(gameId, report, event, acknowledged()));
    }

    static Set<String> acknowledge(GameId gameId, TurnReport report, TurnEvent event, Set<String> acknowledged) {
        Set<String> updated = new HashSet<>(acknowledged);
        int eventIndex = firstPendingIndexOf(gameId, report, event, acknowledged);
        if (eventIndex >= 0) {
            updated.add(key(gameId, report.turn(), eventIndex));
        }
        return Set.copyOf(updated);
    }

    /** Ob gerade diese Schlacht noch offen ist — nicht bloß eine andere am selben System. */
    static boolean isPending(GameId gameId, TurnReport report, TurnEvent event) {
        return event.battleSystemId().isPresent() && firstPendingIndexOf(gameId, report, event, acknowledged()) >= 0;
    }

    /**
     * Gleiche Events (etwa zwei identische Angriffe einer Runde) sind nicht
     * unterscheidbar; quittiert wird jeweils das erste noch offene. Mit indexOf
     * bliebe das zweite für immer offen.
     */
    private static int firstPendingIndexOf(GameId gameId, TurnReport report, TurnEvent event, Set<String> acknowledged) {
        for (int eventIndex = 0; eventIndex < report.events().size(); eventIndex++) {
            if (report.events().get(eventIndex).equals(event)
                    && !acknowledged.contains(key(gameId, report.turn(), eventIndex))) {
                return eventIndex;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked") // rationale: the key is private and only stores a Set<String>.
    private static Set<String> acknowledged() {
        Object value = VaadinSession.getCurrent().getAttribute(SESSION_KEY);
        if (value instanceof Set<?> set) {
            return new HashSet<>((Set<String>) set);
        }
        return new HashSet<>();
    }

    private static String key(GameId gameId, int turn, int eventIndex) {
        return gameId.value() + ":" + turn + ":" + eventIndex;
    }
}
