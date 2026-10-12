package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.ui.RouteStrip;
import de.zettsystems.starfare.navigation.values.RoutePreview;
import de.zettsystems.starfare.navigation.values.RouteState;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Set;

/** Boundary cases against the production component, only on the browser test classpath. */
@Route("acceptance/routes")
@PermitAll
public class RouteAcceptanceView extends Div {
    public RouteAcceptanceView() {
        setId("route-acceptance"); add(new LanguageSwitcher());
        var systems = java.util.stream.IntStream.rangeClosed(1, 7).mapToObj(id ->
                new VisibleSystem(id, "Station " + id + " mit langem Namen", id * 100, 100,
                        id == 2 ? 2 : 1, 1, 1, true, id == 2 ? "#d55e00" : "#56b4e9", null, false, null, List.of())).toList();
        var view = new PlayerViewState(8, List.of(), systems, List.of(), null, false, null,
                List.of(), List.of(), EmpireStats.NONE, Set.of());
        var ids = systems.stream().map(VisibleSystem::id).toList();
        card("preview", new RouteStrip(new RoutePreview(ids, systems.stream().map(VisibleSystem::name).toList(), 12, 450), view, 1, true));
        card("blocked", new RouteStrip(new RouteState(ids, 4, true, true, null, null), view, 1));
        card("direct", new RouteStrip(new RouteState(List.of(1, 7), 1, false, false, 10, 10), view, 1));
        card("unknown", new RouteStrip(new RouteState(List.of(1, 99), 1, false, false, null, null), view, 1));
    }
    private void card(String id, RouteStrip strip) {
        Div card = new Div(new H3(id), strip); card.setId("route-" + id); add(card);
    }
}
