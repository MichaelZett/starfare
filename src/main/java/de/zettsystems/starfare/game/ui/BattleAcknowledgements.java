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
        for (TurnEvent event : report.events()) {
            event.battleSystemId().ifPresent(systemId -> {
                if (!acknowledged.contains(key(gameId, report.turn(), systemId))) {
                    pending.add(systemId);
                }
            });
        }
        return Set.copyOf(pending);
    }

    static void acknowledge(GameId gameId, TurnReport report, int systemId) {
        VaadinSession.getCurrent().setAttribute(SESSION_KEY,
                acknowledge(gameId, report, systemId, acknowledged()));
    }

    static Set<String> acknowledge(GameId gameId, TurnReport report, int systemId, Set<String> acknowledged) {
        Set<String> updated = new HashSet<>(acknowledged);
        updated.add(key(gameId, report.turn(), systemId));
        return Set.copyOf(updated);
    }

    @SuppressWarnings("unchecked") // rationale: the key is private and only stores a Set<String>.
    private static Set<String> acknowledged() {
        Object value = VaadinSession.getCurrent().getAttribute(SESSION_KEY);
        if (value instanceof Set<?> set) {
            return new HashSet<>((Set<String>) set);
        }
        return new HashSet<>();
    }

    private static String key(GameId gameId, int turn, int systemId) {
        return gameId.value() + ":" + turn + ":" + systemId;
    }
}
