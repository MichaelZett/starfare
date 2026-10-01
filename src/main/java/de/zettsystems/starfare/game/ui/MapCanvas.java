package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.style.CssProperties;

import static de.zettsystems.starfare.game.values.GameConfig.MAX_X;
import static de.zettsystems.starfare.game.values.GameConfig.MAX_Y;

/**
 * Scrollable container for the game map. Wraps the scroll Div + inner map Div,
 * installs drag-to-pan on attach, and delegates content rendering to {@link MapRenderer}.
 */
final class MapCanvas extends Div {

    private final Div map = new Div();
    private String viewportKey = "";
    private java.util.List<de.zettsystems.starfare.game.values.VisibleSystem> ownSystems = java.util.List.of();

    MapCanvas(Runnable onBackgroundClick) {
        setId("scroll");
        setWidthFull();
        setHeight("min(80vh, 1100px)");
        getStyle().set(CssProperties.OVERFLOW, "auto");

        map.setId("map");
        map.getStyle().set(CssProperties.POSITION, "relative");
        map.setWidth(MAX_X, Unit.PIXELS);
        map.setHeight(MAX_Y, Unit.PIXELS);
        map.getElement().addEventListener("click", _ -> onBackgroundClick.run())
                .setFilter("event.target === event.currentTarget");
        add(map);
    }

    void useGame(GameId gameId) {
        viewportKey = "starfare.viewport." + gameId.value();
        getElement().executeJs("this.__starfareViewportKey = $0;", viewportKey);
    }

    void render(MapRenderer.Inputs inputs) {
        ownSystems = inputs.systems().stream()
                .filter(system -> java.util.Objects.equals(system.ownerId(), inputs.playerId())).toList();
        MapRenderer.render(map, inputs);
    }

    void zoom(double factor) {
        getElement().executeJs("""
                const map = this.querySelector('#map');
                const old = Number(map.style.zoom || 1);
                const zoom = Math.max(0.1, Math.min(2.5, old * $0));
                const x = (this.scrollLeft + this.clientWidth / 2) / old;
                const y = (this.scrollTop + this.clientHeight / 2) / old;
                map.style.zoom = zoom;
                this.scrollLeft = x * zoom - this.clientWidth / 2;
                this.scrollTop = y * zoom - this.clientHeight / 2;
                this.dispatchEvent(new Event('scroll'));
                """, factor);
    }

    void resetViewport() {
        getElement().executeJs("this.querySelector('#map').style.zoom = '1';");
        if (ownSystems.isEmpty()) {
            centerOn(MAX_X / 2.0, MAX_Y / 2.0);
        } else {
            centerOn(ownSystems.getFirst().x(), ownSystems.getFirst().y());
        }
    }

    void fit(boolean ownOnly) {
        var systems = ownOnly ? ownSystems : java.util.List.<de.zettsystems.starfare.game.values.VisibleSystem>of();
        double minX = systems.stream().mapToDouble(de.zettsystems.starfare.game.values.VisibleSystem::x).min().orElse(0);
        double minY = systems.stream().mapToDouble(de.zettsystems.starfare.game.values.VisibleSystem::y).min().orElse(0);
        double maxX = systems.stream().mapToDouble(de.zettsystems.starfare.game.values.VisibleSystem::x).max().orElse(MAX_X);
        double maxY = systems.stream().mapToDouble(de.zettsystems.starfare.game.values.VisibleSystem::y).max().orElse(MAX_Y);
        getElement().executeJs("""
                const zoom = Math.max(0.1, Math.min(2.5,
                    Math.min(this.clientWidth / ($2 - $0 + 180), this.clientHeight / ($3 - $1 + 180))));
                this.querySelector('#map').style.zoom = zoom;
                this.scrollLeft = ($0 + $2) / 2 * zoom - this.clientWidth / 2;
                this.scrollTop = ($1 + $3) / 2 * zoom - this.clientHeight / 2;
                this.dispatchEvent(new Event('scroll'));
                """, minX, minY, maxX, maxY);
    }

    /**
     * Stellt Zoom und Bildausschnitt aus der vorigen Ansicht dieser Partie wieder her.
     * Fehlt ein gespeicherter Stand — erster Aufruf, neuer Tab —, wird stattdessen auf
     * das übergebene Heimatsystem zentriert. Die Entscheidung fällt im Browser, weil
     * nur dort bekannt ist, ob ein Stand existiert.
     *
     * <p>Der Zustand liegt im {@code sessionStorage}: Nach jedem Zugwechsel baut Vaadin
     * die Kartenansicht neu auf, serverseitiger Zustand ginge dabei ebenso verloren.
     */
    void restoreViewportOrCenterOn(double homeX, double homeY) {
        getElement().executeJs(
                "const el = this;"
                        + "const map = el.querySelector('#map');"
                        + "let saved = null;"
                        + "try { saved = JSON.parse(sessionStorage.getItem($0) || 'null'); } catch (e) { saved = null; }"
                        + "if (saved && map) {"
                        + "  map.style.zoom = saved.zoom;"
                        + "  requestAnimationFrame(() => { el.scrollLeft = saved.left; el.scrollTop = saved.top; });"
                        + "  return;"
                        + "}"
                        + "if (map) map.style.zoom = '1';"
                        + "el.scrollTo({left: $1 - el.clientWidth / 2, top: $2 - el.clientHeight / 2});",
                viewportKey, homeX, homeY);
    }

    void centerOn(double x, double y) {
        getElement().executeJs(
                "const el = this; const map = el.querySelector('#map');"
                        + "const zoom = parseFloat((map && map.style.zoom) || '1');"
                        + "el.scrollTo({left: $0 * zoom - el.clientWidth / 2,"
                        + "top: $1 * zoom - el.clientHeight / 2, behavior: 'smooth'});",
                x, y);
    }

    void installDragToPan() {
        getElement().executeJs(
                "const el = this;"
                        + "el.__starfareViewportKey = $0;"
                        + "if (el.__starfarePan) return;"
                        + "el.__starfarePan = true;"
                        + "el.style.cursor = 'grab';"
                        + "el.style.userSelect = 'none';"
                        + "let pressed = false, dragged = false, sx = 0, sy = 0, lx = 0, ly = 0;"
                        + "const T = 4;"
                        + "el.addEventListener('mousedown', e => {"
                        + "  if (e.button !== 0) return;"
                        + "  pressed = true; dragged = false;"
                        + "  sx = e.pageX; sy = e.pageY; lx = el.scrollLeft; ly = el.scrollTop;"
                        + "});"
                        + "document.addEventListener('mousemove', e => {"
                        + "  if (!pressed) return;"
                        + "  const dx = e.pageX - sx, dy = e.pageY - sy;"
                        + "  if (!dragged && Math.hypot(dx, dy) > T) { dragged = true; el.style.cursor = 'grabbing'; }"
                        + "  if (dragged) { el.scrollLeft = lx - dx; el.scrollTop = ly - dy; }"
                        + "});"
                        + "document.addEventListener('mouseup', () => { pressed = false; el.style.cursor = 'grab'; });"
                        + "el.addEventListener('click', e => { if (dragged) { e.stopPropagation(); e.preventDefault(); dragged = false; } }, true);"
                        // Mouse-wheel zoom: scales the inner #map via the CSS `zoom`
                        // property and re-anchors scroll so the point under the cursor
                        // stays put. Range is clamped so the map cannot disappear or
                        // explode.
                        + // Mouse-wheel zoom: scales the inner #map via the CSS `zoom`
                        // property and re-anchors scroll so the point under the cursor
                        // stays put. Range is clamped so the map cannot disappear or
                        // explode.
                        "const map = el.querySelector('#map');"
                        + "if (map) {"
                        + "  el.addEventListener('wheel', e => {"
                        + "    e.preventDefault();"
                        + "    const oldZoom = parseFloat(map.style.zoom || '1');"
                        + "    const factor = e.deltaY < 0 ? 1.1 : 1 / 1.1;"
                        + "    const newZoom = Math.max(0.4, Math.min(2.5, oldZoom * factor));"
                        + "    if (newZoom === oldZoom) return;"
                        + "    const rect = el.getBoundingClientRect();"
                        + "    const px = e.clientX - rect.left;"
                        + "    const py = e.clientY - rect.top;"
                        + "    const cx = el.scrollLeft + px;"
                        + "    const cy = el.scrollTop + py;"
                        + "    map.style.zoom = newZoom;"
                        + "    const ratio = newZoom / oldZoom;"
                        + "    el.scrollLeft = cx * ratio - px;"
                        + "    el.scrollTop = cy * ratio - py;"
                        + "    store();"
                        + "  }, { passive: false });"
                        + "}"
                        // Zoom und Ausschnitt festhalten, damit der naechste Aufbau der
                        // Ansicht dort weitermacht, wo der Spieler aufgehoert hat.
                        + // Zoom und Ausschnitt festhalten, damit der naechste Aufbau der
                        // Ansicht dort weitermacht, wo der Spieler aufgehoert hat.
                        "let pending = 0;"
                        + "function store() {"
                        + "  clearTimeout(pending);"
                        + "  const key = el.__starfareViewportKey;"
                        + "  pending = setTimeout(() => {"
                        + "    if (key !== el.__starfareViewportKey) return;"
                        + "    try {"
                        + "      sessionStorage.setItem(key, JSON.stringify({"
                        + "        zoom: parseFloat((map && map.style.zoom) || '1'),"
                        + "        left: el.scrollLeft, top: el.scrollTop}));"
                        + "    } catch (e) { /* privater Modus o. ae. — dann eben ohne */ }"
                        + "  }, 150);"
                        + "}"
                        + "el.addEventListener('scroll', store, { passive: true });",
                viewportKey
        );
    }

}
