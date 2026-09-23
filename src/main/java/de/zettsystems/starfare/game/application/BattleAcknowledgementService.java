package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;

import java.util.Set;

/**
 * Merkt sich je Konto, welche Schlachten eines Rundenberichts ausgewertet sind — über
 * Sitzungen und Geräte hinweg, damit das Archiv nicht erneut durch die Auswertung führt.
 */
public interface BattleAcknowledgementService {

    /** Indizes der bestätigten Events im Bericht der Runde {@code turn}. */
    Set<Integer> acknowledgedEventIndices(String playerId, GameId gameId, int turn);

    /** Bestätigt ein Event; Bestätigungen früherer Runden derselben Partie entfallen. */
    void acknowledge(String playerId, GameId gameId, int turn, int eventIndex);

    /** Entfernt alle Bestätigungen einer abgebrochenen Partie. */
    void forgetGame(GameId gameId);
}
