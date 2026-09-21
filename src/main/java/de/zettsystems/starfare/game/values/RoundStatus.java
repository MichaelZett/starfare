package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * Wer in der laufenden Runde schon abgegeben hat und wann die Runde spätestens endet.
 * {@code deadline} ist {@code null}, solange keine Frist läuft (Partie mit nur einem Menschen);
 * {@code stragglerId} ist der letzte Mensch, der noch fehlt, sobald die Nachzügler-Uhr läuft.
 */
public record RoundStatus(List<Seat> seats, @Nullable Instant deadline, @Nullable Integer stragglerId) {

    public static final RoundStatus NONE = new RoundStatus(List.of(), null, null);

    public RoundStatus {
        seats = List.copyOf(seats);
    }

    public record Seat(int playerId, String label, String colorHex, boolean submitted) {
    }
}
