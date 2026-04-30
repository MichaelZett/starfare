package de.zettsystems.starfare.report.values;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TurnEventJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

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
}
