package de.zettsystems.starfare.report.values;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TurnEventJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void allBattleEventsPreserveFractionalStrengths() {
        List<TurnEvent> events = List.of(
                new TurnEvent.BattleWon(1, 5, "Vega", 3, 3, 0, false, 3.24, 2.76),
                new TurnEvent.BattleLost(1, 5, "Vega", 3, 3, 0, 2.76, 3.24),
                new TurnEvent.SystemLost(2, 1, 5, "Vega", 3, 3, 0, 3.24, 2.76),
                new TurnEvent.DefenseHeld(2, 5, "Vega", 3, 3, 0, 2.76, 3.24));
        assertThat(events).allSatisfy(event -> {
            TurnEvent restored = mapper.readValue(mapper.writeValueAsString(event), TurnEvent.class);
            assertThat(restored).isEqualTo(event);
            assertThat(BattleReplay.from(restored)).isEqualTo(BattleReplay.from(event));
        });
    }

    @Test
    void integerStrengthsFromExistingSnapshotsRemainReadable() {
        String json = """
                {"type":"battleWon","attackerId":1,"systemId":5,"systemName":"Vega",
                 "attacking":3,"defending":3,"remaining":0,"wasNeutral":false,
                 "attackerStrength":3,"defenderStrength":3}
                """;
        assertThat(mapper.readValue(json, TurnEvent.class))
                .isEqualTo(new TurnEvent.BattleWon(1, 5, "Vega", 3, 3, 0, false, 3, 3));
    }

    @Test
    void reinforcementWithoutTotalGarrisonDeserializesAsZero() {
        String legacyJson = """
                {
                  "type": "reinforcement",
                  "playerId": 1,
                  "systemId": 5,
                  "systemName": "Sirius",
                  "ships": 7,
                  "fleetLabel": "F1"
                }
                """;

        TurnEvent event = mapper.readValue(legacyJson, TurnEvent.class);

        assertThat(event).isInstanceOf(TurnEvent.Reinforcement.class);
        TurnEvent.Reinforcement r = (TurnEvent.Reinforcement) event;
        assertThat(r.ships()).isEqualTo(7);
        assertThat(r.totalGarrison()).as("missing totalGarrison falls back to 0").isZero();
    }

    @Test
    void reinforcementRoundTripsTotalGarrison() {
        TurnEvent.Reinforcement original = new TurnEvent.Reinforcement(1, 5, "Sirius", 7, 12, "F1");

        String json = mapper.writeValueAsString(original);
        TurnEvent decoded = mapper.readValue(json, TurnEvent.class);

        assertThat(decoded).isEqualTo(original);
        assertThat(((TurnEvent.Reinforcement) decoded).totalGarrison()).isEqualTo(12);
    }

    @Test
    void systemLostWithoutStrengthsDeserializesAsZero() {
        String legacyJson = """
                {"type": "systemLost", "defenderId": 1, "attackerId": 2, "systemId": 5, "systemName": "Sirius"}
                """;

        TurnEvent event = mapper.readValue(legacyJson, TurnEvent.class);

        assertThat(event).isEqualTo(new TurnEvent.SystemLost(1, 2, 5, "Sirius"))
                .isEqualTo(new TurnEvent.SystemLost(1, 2, 5, "Sirius", 0, 0, 0));
    }

    @Test
    void defenseHeldWithoutDefendingDeserializesAsZero() {
        String legacyJson = """
                {"type": "defenseHeld", "defenderId": 1, "systemId": 5, "systemName": "Sirius",
                 "attacking": 4, "defendersLeft": 3}
                """;

        TurnEvent event = mapper.readValue(legacyJson, TurnEvent.class);

        assertThat(event).isEqualTo(new TurnEvent.DefenseHeld(1, 5, "Sirius", 4, 3));
    }

    @Test
    void battleEventsRoundTripTheirStrengths() {
        List<TurnEvent> events = List.of(
                new TurnEvent.SystemLost(1, 2, 5, "Sirius", 9, 6, 3),
                new TurnEvent.DefenseHeld(1, 5, "Sirius", 4, 8, 5));

        for (TurnEvent original : events) {
            assertThat(mapper.readValue(mapper.writeValueAsString(original), TurnEvent.class)).isEqualTo(original);
        }
    }

    @Test
    void legacyBattleEventUsesOriginalForMissingRolledStrengths() {
        String legacyJson = """
                {"type":"battleWon","attackerId":1,"systemId":5,"systemName":"Sirius",
                 "attacking":9,"defending":6,"remaining":3,"wasNeutral":false}
                """;

        TurnEvent.BattleWon event = (TurnEvent.BattleWon) mapper.readValue(legacyJson, TurnEvent.class);

        assertThat(event.attackerStrength()).isEqualTo(9);
        assertThat(event.defenderStrength()).isEqualTo(6);
    }

    @Test
    void everyEventTypeReportsItsSystems() {
        List<TurnEvent> battles = List.of(
                new TurnEvent.BattleWon(1, 8, "Vega", 12, 5, 7, false),
                new TurnEvent.BattleLost(1, 8, "Vega", 12, 15, 4),
                new TurnEvent.SystemLost(1, 2, 8, "Vega", 9, 6, 3),
                new TurnEvent.DefenseHeld(1, 8, "Vega", 4, 8, 5));
        List<TurnEvent> systemOnly = List.of(
                new TurnEvent.Production(1, 8, "Vega", 3),
                new TurnEvent.Reinforcement(1, 8, "Vega", 7, 12, "F1"));
        List<TurnEvent> gameEnd = List.of(new TurnEvent.Victory(1), new TurnEvent.Defeat(1, "Hans"));

        assertThat(battles).isNotEmpty().allSatisfy(event -> {
            assertThat(event.affectedSystemId()).hasValue(8);
            assertThat(event.battleSystemId()).hasValue(8);
        });
        assertThat(systemOnly).isNotEmpty().allSatisfy(event -> {
            assertThat(event.affectedSystemId()).hasValue(8);
            assertThat(event.battleSystemId()).isEmpty();
        });
        assertThat(gameEnd).isNotEmpty().allSatisfy(event -> {
            assertThat(event.affectedSystemId()).isEmpty();
            assertThat(event.battleSystemId()).isEmpty();
        });
    }
}
