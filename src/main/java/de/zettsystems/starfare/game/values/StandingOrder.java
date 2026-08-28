package de.zettsystems.starfare.game.values;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Production-routed standing order: each turn {@code ships} are shipped from
 * {@code fromSystemId} to {@code toSystemId} instead of staying in the source's
 * garrison. A source system may hold several orders to different targets; their
 * combined size is capped by the source's own production plus everything routed
 * into it. Re-adding the same source/target pair replaces the previous size.
 * Auto-cancelled when the source changes owner.
 */
public record StandingOrder(int id, int ownerId, int fromSystemId, int toSystemId, int ships) {

    /**
     * Backwards-compatible factory: snapshots written before {@code ships} existed
     * routed a system's whole production and carry no number. Jackson would coerce
     * the missing value into the primitive and fail the whole game session, so map
     * it to {@code 0} — the order stays visible and the player can resize it.
     */
    @JsonCreator
    public static StandingOrder of(
            @JsonProperty("id") int id,
            @JsonProperty("ownerId") int ownerId,
            @JsonProperty("fromSystemId") int fromSystemId,
            @JsonProperty("toSystemId") int toSystemId,
            @JsonProperty("ships") @Nullable Integer ships) {
        return new StandingOrder(id, ownerId, fromSystemId, toSystemId, ships != null ? ships : 0);
    }
}
