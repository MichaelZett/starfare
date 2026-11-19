package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.values.EventFrequency;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A supernova reaches the report as ticker news and leaves a dark husk on the map. */
class CosmicNewsJourneyTest extends AbstractMapJourneyTest {

    @Test
    void supernovaShowsInTheReportAndOnTheMap() {
        email = loginAsNewPlayer("Ticker Reader");
        account = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        GameSetup setup = GameSetup.defaults().withSectorOptions(new SectorOptions(EventFrequency.NORMAL, null, false, 0));
        id = games.newGame(setup, account, "Supernova");
        player = games.joinGame(id, account, "Ticker Reader", "Ticker Empire").orElseThrow();
        assertThat(games.startGame(id)).isTrue();

        int seat = player;
        String[] names = new String[2];
        registry.writeState(id, state -> {
            List<StarSystem> neutral = state.systems().stream().filter(StarSystem::neutral).toList();
            StarSystem destroyed = neutral.get(0);
            StarSystem unstable = neutral.get(1);
            names[0] = destroyed.name();
            names[1] = unstable.name();
            state.updateCosmic(cosmic -> cosmic.explode(destroyed.id()).destabilize(unstable.id(), state.turn() + 1));
            state.updateSystem(destroyed.id(), StarSystem::explode);
            state.reports().put(seat, new TurnReport(state.turn(), List.of(), List.of(
                    new TurnEvent.Cosmic(CosmicNews.SUPERNOVA, destroyed.id(), destroyed.name(), 0, 0, 0, 0, ""),
                    new TurnEvent.Cosmic(CosmicNews.SUN_UNSTABLE, unstable.id(), unstable.name(), 0, 0, 0,
                            state.turn() + 1, ""))));
            state.nextTurn();
            return null;
        });
        openMap();
        selectTab("Bericht");

        assertThat(find(Div.class).withClassName("event-cosmic").all())
                .extracting(card -> card.getElement().getTextRecursively())
                .anySatisfy(text -> assertThat(text).contains(names[0], "Supernova"))
                .anySatisfy(text -> assertThat(text).contains(names[1], "instabil"));
        assertThat(find(Div.class).withClassName("sys-destroyed").all()).hasSize(1);
        assertThat(find(Div.class).withClassName("sys-unstable").all()).hasSize(1);
    }
}
