package de.zettsystems.starfare.navigation.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.navigation.values.RoutePreview;
import de.zettsystems.starfare.navigation.values.RouteState;
import de.zettsystems.starfare.style.CssProperties;
import java.util.List;
import java.util.Objects;

/** One keyboard-accessible, wrapping itinerary. Selection only focuses the map. */
public final class RouteStrip extends Div {
    private final List<Integer> systems;
    private final List<String> names;
    private final PlayerViewState view;
    private final int owner;
    private final RouteState state;
    private final boolean preview;
    private final Div stops = new Div();
    private boolean expanded;
    private java.util.Set<Integer> alliedOwners = java.util.Set.of();
    public void alliedOwners(java.util.Set<Integer> owners) { alliedOwners = java.util.Set.copyOf(owners); render(); }
    private java.util.function.Consumer<Boolean> onExpansion = _ -> { };

    public void rememberExpansion(boolean value, java.util.function.Consumer<Boolean> listener) {
        expanded = value; onExpansion = listener; render();
    }
    private void expand(boolean value) { expanded = value; onExpansion.accept(value); render(); }

    public RouteStrip(RoutePreview preview, PlayerViewState view, int owner, boolean reachable) {
        this(preview.systems(), preview.names(), view, owner,
                new RouteState(preview.systems(), 0, true, !reachable, null, null), true);
    }
    public RouteStrip(RouteState state, PlayerViewState view, int owner) {
        this(state.systems(), state.systems().stream().map(id -> view.systems().stream()
                .filter(system -> system.id() == id).map(VisibleSystem::name).findFirst().orElse("?")).toList(), view, owner, state, false);
    }
    private RouteStrip(List<Integer> systems, List<String> names, PlayerViewState view, int owner, RouteState state, boolean preview) {
        this.systems = systems; this.names = names; this.view = view; this.owner = owner; this.state = state; this.preview = preview;
        addClassName("route-strip"); stops.addClassName("route-stops"); add(stops); render();
    }
    private void render() {
        stops.removeAll();
        var indices = java.util.stream.IntStream.range(0, systems.size()).boxed().toList();
        if (!expanded && systems.size() > 4) { indices = state.compactIndices(); }
        int previous = -1;
        for (int index : indices) {
            if (index > previous + 1) {
                Button more = new Button(I18n.t(RouteKeys.MORE, index - previous - 1), _ -> expand(true));
                more.addClassName("route-more"); stops.add(more);
            }
            if (index > 0) {
                Span link = new Span(state.blocked() && index == state.activeIndex() + 1 ? "×" : "→");
                link.addClassName("route-link"); stops.add(link);
            }
            addStop(index);
            previous = index;
        }
        if (expanded && systems.size() > 4) {
            stops.add(new Button(I18n.t(RouteKeys.LESS), _ -> expand(false)));
        }
    }
    private void addStop(int index) {
        var system = view.systems().stream().filter(candidate -> candidate.id() == systems.get(index)).findFirst();
        Button point = new Button(names.get(index));
        point.addClassName("route-stop");
        point.getElement().setAttribute(de.zettsystems.starfare.style.HtmlAttributes.ROUTE_SYSTEM, String.valueOf(systems.get(index)));
        if (index < state.activeIndex()) { point.addClassName("route-completed"); }
        if (index == state.activeIndex()) { point.addClassName(state.atStation() ? "route-current" : "route-next"); }
        if (state.atStation() && index == state.activeIndex() + 1) { point.addClassName("route-next"); }
        if (state.blocked() && index == state.activeIndex()) { point.addClassName("route-blocked"); }
        String role = RouteKeys.STATION;
        if (index == 0) { role = RouteKeys.SOURCE; }
        if (index < state.activeIndex() && !preview) { role = RouteKeys.PASSED; }
        if (index == state.activeIndex() && !state.atStation()) { role = RouteKeys.NEXT; }
        if (index == systems.size() - 1) { role = RouteKeys.DESTINATION; }
        if (index == state.activeIndex() && state.atStation()) { role = RouteKeys.CURRENT; }
        String location = I18n.t(role, names.get(index));
        point.setAriaLabel(location);
        system.ifPresent(known -> {
            String color = known.colorHex();
            if (color != null) { point.getStyle().set(CssProperties.BORDER_COLOR, color); }
            Integer stationOwner = known.ownerId();
            boolean allied = stationOwner != null && !Objects.equals(stationOwner, owner)
                    && (alliedOwners.contains(stationOwner) || (preview && !state.blocked() && index > 0 && index < systems.size() - 1));
            if (allied) {
                point.setText("◇ " + names.get(index)); point.setAriaLabel(location + " · " + I18n.t(RouteKeys.ALLIED, names.get(index)));
            }
            point.addClickListener(_ -> focus(point, known));
        });
        point.setEnabled(system.isPresent()); stops.add(point);
    }
    private void focus(Button point, VisibleSystem system) {
        stops.getChildren().forEach(component -> component.removeClassName("route-selected"));
        point.addClassName("route-selected");
        getElement().executeJs("""
                const map = document.querySelector('#map');
                const pane = map?.parentElement;
                if (pane) {
                    const zoom = parseFloat(map.style.zoom || '1');
                    pane.scrollTo({left:$0 * zoom - pane.clientWidth/2, top:$1 * zoom - pane.clientHeight/2, behavior:'smooth'});
                }
                """, system.x(), system.y());
    }
}
