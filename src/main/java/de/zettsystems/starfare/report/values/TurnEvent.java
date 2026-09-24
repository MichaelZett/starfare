package de.zettsystems.starfare.report.values;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Structured event emitted during a turn advance. Each subtype captures
 * one atomic occurrence that can be displayed visually in the round report.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TurnEvent.Production.class, name = "production"),
        @JsonSubTypes.Type(value = TurnEvent.Reinforcement.class, name = "reinforcement"),
        @JsonSubTypes.Type(value = TurnEvent.BattleWon.class, name = "battleWon"),
        @JsonSubTypes.Type(value = TurnEvent.BattleLost.class, name = "battleLost"),
        @JsonSubTypes.Type(value = TurnEvent.SystemLost.class, name = "systemLost"),
        @JsonSubTypes.Type(value = TurnEvent.DefenseHeld.class, name = "defenseHeld"),
        @JsonSubTypes.Type(value = TurnEvent.Victory.class, name = "victory"),
        @JsonSubTypes.Type(value = TurnEvent.Defeat.class, name = "defeat")
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
                     int attacking, int defending, int remaining, boolean wasNeutral,
                     double attackerStrength, double defenderStrength, String attackerName, String defenderName) implements TurnEvent {
        public BattleWon(int attackerId, int systemId, String systemName,
                         int attacking, int defending, int remaining, boolean wasNeutral) {
            this(attackerId, systemId, systemName, attacking, defending, remaining, wasNeutral, attacking, defending, "", "");
        }

        public BattleWon(int attackerId, int systemId, String systemName, int attacking, int defending, int remaining,
                         boolean wasNeutral, double attackerStrength, double defenderStrength) {
            this(attackerId, systemId, systemName, attacking, defending, remaining, wasNeutral,
                    attackerStrength, defenderStrength, "", "");
        }

        @JsonCreator
        public static BattleWon of(
                @JsonProperty("attackerId") int attackerId, @JsonProperty("systemId") int systemId,
                @JsonProperty("systemName") String systemName, @JsonProperty("attacking") int attacking,
                @JsonProperty("defending") int defending, @JsonProperty("remaining") int remaining,
                @JsonProperty("wasNeutral") boolean wasNeutral,
                @JsonProperty("attackerStrength") @Nullable Double attackerStrength,
                @JsonProperty("defenderStrength") @Nullable Double defenderStrength,
                @JsonProperty("attackerName") @Nullable String attackerName,
                @JsonProperty("defenderName") @Nullable String defenderName) {
            return new BattleWon(attackerId, systemId, systemName, attacking, defending, remaining, wasNeutral,
                    attackerStrength != null ? attackerStrength : attacking,
                    defenderStrength != null ? defenderStrength : defending,
                    Objects.requireNonNullElse(attackerName, ""), Objects.requireNonNullElse(defenderName, ""));
        }
    }

    record BattleLost(int attackerId, int systemId, String systemName,
                      int attacking, int defending, int defendersLeft,
                      double attackerStrength, double defenderStrength, String attackerName, String defenderName) implements TurnEvent {
        public BattleLost(int attackerId, int systemId, String systemName,
                          int attacking, int defending, int defendersLeft) {
            this(attackerId, systemId, systemName, attacking, defending, defendersLeft, attacking, defending, "", "");
        }

        public BattleLost(int attackerId, int systemId, String systemName, int attacking, int defending, int defendersLeft,
                          double attackerStrength, double defenderStrength) {
            this(attackerId, systemId, systemName, attacking, defending, defendersLeft,
                    attackerStrength, defenderStrength, "", "");
        }

        @JsonCreator
        public static BattleLost of(
                @JsonProperty("attackerId") int attackerId, @JsonProperty("systemId") int systemId,
                @JsonProperty("systemName") String systemName, @JsonProperty("attacking") int attacking,
                @JsonProperty("defending") int defending, @JsonProperty("defendersLeft") int defendersLeft,
                @JsonProperty("attackerStrength") @Nullable Double attackerStrength,
                @JsonProperty("defenderStrength") @Nullable Double defenderStrength,
                @JsonProperty("attackerName") @Nullable String attackerName,
                @JsonProperty("defenderName") @Nullable String defenderName) {
            return new BattleLost(attackerId, systemId, systemName, attacking, defending, defendersLeft,
                    attackerStrength != null ? attackerStrength : attacking,
                    defenderStrength != null ? defenderStrength : defending,
                    Objects.requireNonNullElse(attackerName, ""), Objects.requireNonNullElse(defenderName, ""));
        }
    }

    record SystemLost(int defenderId, int attackerId, int systemId, String systemName,
                      int attacking, int defending, int attackersRemaining,
                      double attackerStrength, double defenderStrength, String attackerName, String defenderName)
            implements TurnEvent {
        public SystemLost(int defenderId, int attackerId, int systemId, String systemName) {
            this(defenderId, attackerId, systemId, systemName, 0, 0, 0, 0, 0, "", "");
        }

        public SystemLost(int defenderId, int attackerId, int systemId, String systemName,
                          int attacking, int defending, int attackersRemaining) {
            this(defenderId, attackerId, systemId, systemName, attacking, defending, attackersRemaining,
                    attacking, defending, "", "");
        }

        public SystemLost(int defenderId, int attackerId, int systemId, String systemName, int attacking, int defending,
                          int attackersRemaining, double attackerStrength, double defenderStrength) {
            this(defenderId, attackerId, systemId, systemName, attacking, defending, attackersRemaining,
                    attackerStrength, defenderStrength, "", "");
        }

        @JsonCreator
        public static SystemLost of(
                @JsonProperty("defenderId") int defenderId, @JsonProperty("attackerId") int attackerId,
                @JsonProperty("systemId") int systemId, @JsonProperty("systemName") String systemName,
                @JsonProperty("attacking") @Nullable Integer attacking,
                @JsonProperty("defending") @Nullable Integer defending,
                @JsonProperty("attackersRemaining") @Nullable Integer attackersRemaining,
                @JsonProperty("attackerStrength") @Nullable Double attackerStrength,
                @JsonProperty("defenderStrength") @Nullable Double defenderStrength,
                @JsonProperty("attackerName") @Nullable String attackerName,
                @JsonProperty("defenderName") @Nullable String defenderName) {
            int actualAttacking = Objects.requireNonNullElse(attacking, 0);
            int actualDefending = Objects.requireNonNullElse(defending, 0);
            return new SystemLost(defenderId, attackerId, systemId, systemName,
                    actualAttacking, actualDefending, Objects.requireNonNullElse(attackersRemaining, 0),
                    attackerStrength != null ? attackerStrength : actualAttacking,
                    defenderStrength != null ? defenderStrength : actualDefending,
                    Objects.requireNonNullElse(attackerName, ""), Objects.requireNonNullElse(defenderName, ""));
        }
    }

    record DefenseHeld(int defenderId, int systemId, String systemName,
                       int attacking, int defending, int defendersLeft,
                       double attackerStrength, double defenderStrength, String attackerName, String defenderName)
            implements TurnEvent {
        public DefenseHeld(int defenderId, int systemId, String systemName,
                           int attacking, int defendersLeft) {
            this(defenderId, systemId, systemName, attacking, 0, defendersLeft, attacking, 0, "", "");
        }

        public DefenseHeld(int defenderId, int systemId, String systemName,
                           int attacking, int defending, int defendersLeft) {
            this(defenderId, systemId, systemName, attacking, defending, defendersLeft, attacking, defending, "", "");
        }

        public DefenseHeld(int defenderId, int systemId, String systemName, int attacking, int defending,
                           int defendersLeft, double attackerStrength, double defenderStrength) {
            this(defenderId, systemId, systemName, attacking, defending, defendersLeft,
                    attackerStrength, defenderStrength, "", "");
        }

        @JsonCreator
        public static DefenseHeld of(
                @JsonProperty("defenderId") int defenderId, @JsonProperty("systemId") int systemId,
                @JsonProperty("systemName") String systemName, @JsonProperty("attacking") @Nullable Integer attacking,
                @JsonProperty("defending") @Nullable Integer defending,
                @JsonProperty("defendersLeft") @Nullable Integer defendersLeft,
                @JsonProperty("attackerStrength") @Nullable Double attackerStrength,
                @JsonProperty("defenderStrength") @Nullable Double defenderStrength,
                @JsonProperty("attackerName") @Nullable String attackerName,
                @JsonProperty("defenderName") @Nullable String defenderName) {
            int actualAttacking = Objects.requireNonNullElse(attacking, 0);
            int actualDefending = Objects.requireNonNullElse(defending, 0);
            return new DefenseHeld(defenderId, systemId, systemName, actualAttacking, actualDefending,
                    Objects.requireNonNullElse(defendersLeft, 0),
                    attackerStrength != null ? attackerStrength : actualAttacking,
                    defenderStrength != null ? defenderStrength : actualDefending,
                    Objects.requireNonNullElse(attackerName, ""), Objects.requireNonNullElse(defenderName, ""));
        }
    }

    record Victory(int winnerId) implements TurnEvent {}

    /** End-of-game notice sent to every player other than the winner. */
    record Defeat(int winnerId, String winnerName) implements TurnEvent {}
}
