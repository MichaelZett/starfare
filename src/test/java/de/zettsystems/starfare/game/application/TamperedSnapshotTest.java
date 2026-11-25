package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.testsupport.NavigationStates;
import de.zettsystems.starfare.testsupport.SpacewardStates;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A stored game whose parts contradict its rules or each other is refused on restore, not played on. */
class TamperedSnapshotTest extends AbstractIntegrationTest {
    @Autowired private ObjectMapper mapper;

    @Test
    void industryAndNavigationBelongOnlyToSpaceward() {
        assertRejected(SpacewardStates.running(), json -> json.set("ruleset", classic()),
                "Industry belongs only to Spaceward");
        assertRejected(SpacewardStates.running(), json -> {
            json.set("ruleset", classic());
            json.remove("industries");
        }, "Navigation state belongs only to Spaceward");
    }

    @Test
    void industryMustExplainTheShipProduction() {
        assertRejected(SpacewardStates.running(),
                json -> ((ObjectNode) json.path("systems").path(0)).put("productionPerTurn", 99),
                "System industry and ship production disagree");
    }

    @Test
    void itinerariesMayOnlyVisitKnownSystems() {
        GameState state = NavigationStates.running();
        new DefaultFleetService().sendFleet(state, 1, 1, 2, 10);

        assertRejected(state, json -> ((ArrayNode) json.path("fleets").path(0).path("journey").path("stations")).set(0, 999),
                "Fleet and itinerary disagree");
    }

    private ObjectNode tree(GameState state) {
        return (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
    }

    private ObjectNode classic() {
        return mapper.createObjectNode().put("variant", RulesetRef.SECTOR_FORCES.variant())
                .put("version", RulesetRef.SECTOR_FORCES.version());
    }

    private void assertRejected(GameState state, Consumer<ObjectNode> tamper, String message) {
        ObjectNode json = tree(state);
        tamper.accept(json);
        GameStateSnapshot snapshot = mapper.treeToValue(json, GameStateSnapshot.class);

        assertThatThrownBy(() -> GameState.fromSnapshot(snapshot))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
    }
}
