package de.zettsystems.starfare.report.values;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;

/**
 * Structured event emitted during a turn advance. Each subtype captures
 * one atomic occurrence that can be displayed visually in the round report.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TurnEvent.Production.class,   name = "production"),
        @JsonSubTypes.Type(value = TurnEvent.Reinforcement.class, name = "reinforcement"),
        @JsonSubTypes.Type(value = TurnEvent.BattleWon.class,    name = "battleWon"),
        @JsonSubTypes.Type(value = TurnEvent.BattleLost.class,   name = "battleLost"),
        @JsonSubTypes.Type(value = TurnEvent.SystemLost.class,   name = "systemLost"),
        @JsonSubTypes.Type(value = TurnEvent.DefenseHeld.class,  name = "defenseHeld"),
        @JsonSubTypes.Type(value = TurnEvent.Victory.class,      name = "victory"),
        @JsonSubTypes.Type(value = TurnEvent.Defeat.class,       name = "defeat")
})
public sealed interface TurnEvent {

    /** Returns the system affected by this event, if it has one. */
    default OptionalInt affectedSystemId() {
        return switch (this) {
            case Production event -> OptionalInt.of(event.systemId());
            case Reinforcement event -> OptionalInt.of(event.systemId());
            case BattleWon event -> OptionalInt.of(event.systemId());
            case BattleLost event -> OptionalInt.of(event.systemId());
            case SystemLost event -> OptionalInt.of(event.systemId());
            case DefenseHeld event -> OptionalInt.of(event.systemId());
            case Victory _, Defeat _ -> OptionalInt.empty();
        };
    }

    /** Returns the system for events that deserve a map marker: resolved combat only. */
    default OptionalInt battleSystemId() {
        return switch (this) {
            case BattleWon event -> OptionalInt.of(event.systemId());
            case BattleLost event -> OptionalInt.of(event.systemId());
            case SystemLost event -> OptionalInt.of(event.systemId());
            case DefenseHeld event -> OptionalInt.of(event.systemId());
            case Production _, Reinforcement _, Victory _, Defeat _ -> OptionalInt.empty();
        };
    }

    record Production(int playerId, int systemId, String systemName, int amount)
            implements TurnEvent {}

    record Reinforcement(int playerId, int systemId, String systemName, int ships, int totalGarrison, String fleetLabel)
            implements TurnEvent {

        /**
         * Backwards-compatible factory: pre-{@code totalGarrison} JSON snapshots stored in
         * the DB lack the field, so Jackson would coerce {@code null} into the primitive
         * and fail. Map missing values to {@code 0} on read.
         */
        @JsonCreator
        public static Reinforcement of(
                @JsonProperty("playerId") int playerId,
                @JsonProperty("systemId") int systemId,
                @JsonProperty("systemName") String systemName,
                @JsonProperty("ships") int ships,
                @JsonProperty("totalGarrison") @Nullable Integer totalGarrison,
                @JsonProperty("fleetLabel") String fleetLabel) {
            return new Reinforcement(playerId, systemId, systemName, ships,
                    totalGarrison != null ? totalGarrison : 0, fleetLabel);
        }
    }

    record BattleWon(int attackerId, int systemId, String systemName,
                     int attacking, int defending, int remaining, boolean wasNeutral)
            implements TurnEvent {}

    record BattleLost(int attackerId, int systemId, String systemName,
                      int attacking, int defending, int defendersLeft)
            implements TurnEvent {}

    record SystemLost(int defenderId, int attackerId, int systemId, String systemName)
            implements TurnEvent {}

    record DefenseHeld(int defenderId, int systemId, String systemName,
                       int attacking, int defendersLeft)
            implements TurnEvent {}

    record Victory(int winnerId) implements TurnEvent {}

    /** End-of-game notice sent to every player other than the winner. */
    record Defeat(int winnerId, String winnerName) implements TurnEvent {}
}
