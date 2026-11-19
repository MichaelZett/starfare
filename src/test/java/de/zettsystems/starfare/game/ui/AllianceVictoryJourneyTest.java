package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.standalone.ui.ArchiveView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.VictoryRules;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Two allied people reach the shared victory share together; both are named winners everywhere. */
class AllianceVictoryJourneyTest extends AbstractMapJourneyTest {
    private static final String NAME = "Alliance victory";

    @Test
    void bothAlliesWinAndStayWinnersAfterReloadAndInTheArchive() {
        openGame(allianceAboutToWin(), NAME);
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        runPendingUiAccess();
        assertThat(games.viewFor(id, 1).outcome().winnerIds()).containsExactly(1, 2);

        assertThat(text("game-outcome-winner")).contains("Commander, Partner");
        assertThat(text("game-outcome-personal-result")).contains("Sieg");
        assertThat(text("game-outcome-economy")).contains("Industrie gebaut: 0 Kapazitätspunkte")
                .doesNotContain("Metall").doesNotContain("erfasst");

        reloadMap();
        assertThat(viewText()).contains("Spiel beendet. Sieger: Bündnis 1: Commander, Partner");
        assertThat(find(Component.class).withClassName("review-controls").all()).anyMatch(Component::isVisible);

        assertThat(archiveRows()).anyMatch(row -> row.contains(NAME) && row.contains("Commander, Partner"));
    }

    /** Eleven of sixteen systems belong to the first seat; with the partner this exceeds the 70 % alliance share. */
    private static GameState allianceAboutToWin() {
        GameState state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE)
                .chooseVictoryRules(new VictoryRules(70, true, true, 70)));
        state.players().addAll(List.of(new Player(1, "Commander", false, "#56b4e9"),
                new Player(2, "Partner", false, "#d55e00"), new Player(3, "Opponent", true, "#009e73")));
        for (int system = 1; system <= 16; system++) {
            state.systems().add(new StarSystem(system, "S" + system, system * 100, 100, system <= 11 ? 1 : 2, 5, 2, false));
        }
        state.joinedHumanPlayerIds().addAll(Set.of(1, 2));
        state.originalHumanPlayerIds().addAll(Set.of(1, 2));
        state.players().forEach(player -> state.intel().put(player.id(), new HashMap<>()));
        state.start();
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        return state;
    }

    @SuppressWarnings("unchecked") // rationale: archive cells are rendered components; their row type is immaterial here.
    private List<String> archiveRows() {
        navigate(ArchiveView.class);
        GridTester<Grid<Object>, Object> grid = test(find(Grid.class).single());
        return java.util.stream.IntStream.range(0, grid.size())
                .mapToObj(row -> elementText(grid.getCellComponent(row, 0).getElement())).toList();
    }
}
