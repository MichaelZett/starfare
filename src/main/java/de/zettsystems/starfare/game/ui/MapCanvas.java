package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.style.CssProperties;

import static de.zettsystems.starfare.game.domain.GameState.MAX_X;
import static de.zettsystems.starfare.game.domain.GameState.MAX_Y;

/**
 * Scrollable container for the game map. Wraps the scroll Div + inner map Div,
 * installs drag-to-pan on attach, and delegates content rendering to {@link MapRenderer}.
 */
final class MapCanvas extends Div {

    private final Div map = new Div();

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

    void render(MapRenderer.Inputs inputs) {
        MapRenderer.render(map, inputs);
    }

    void installDragToPan() {
        getElement().executeJs(
                "const el = this;" +
                        "if (el.__starfarePan) return;" +
                        "el.__starfarePan = true;" +
                        "el.style.cursor = 'grab';" +
                        "el.style.userSelect = 'none';" +
                        "let pressed = false, dragged = false, sx = 0, sy = 0, lx = 0, ly = 0;" +
                        "const T = 4;" +
                        "el.addEventListener('mousedown', e => {" +
                        "  if (e.button !== 0) return;" +
                        "  pressed = true; dragged = false;" +
                        "  sx = e.pageX; sy = e.pageY; lx = el.scrollLeft; ly = el.scrollTop;" +
                        "});" +
                        "document.addEventListener('mousemove', e => {" +
                        "  if (!pressed) return;" +
                        "  const dx = e.pageX - sx, dy = e.pageY - sy;" +
                        "  if (!dragged && Math.hypot(dx, dy) > T) { dragged = true; el.style.cursor = 'grabbing'; }" +
                        "  if (dragged) { el.scrollLeft = lx - dx; el.scrollTop = ly - dy; }" +
                        "});" +
                        "document.addEventListener('mouseup', () => { pressed = false; el.style.cursor = 'grab'; });" +
                        "el.addEventListener('click', e => { if (dragged) { e.stopPropagation(); e.preventDefault(); dragged = false; } }, true);" +
                        // Mouse-wheel zoom: scales the inner #map via the CSS `zoom`
                        // property and re-anchors scroll so the point under the cursor
                        // stays put. Range is clamped so the map cannot disappear or
                        // explode.
                        "const map = el.querySelector('#map');" +
                        "if (map) {" +
                        "  el.addEventListener('wheel', e => {" +
                        "    e.preventDefault();" +
                        "    const oldZoom = parseFloat(map.style.zoom || '1');" +
                        "    const factor = e.deltaY < 0 ? 1.1 : 1 / 1.1;" +
                        "    const newZoom = Math.max(0.4, Math.min(2.5, oldZoom * factor));" +
                        "    if (newZoom === oldZoom) return;" +
                        "    const rect = el.getBoundingClientRect();" +
                        "    const px = e.clientX - rect.left;" +
                        "    const py = e.clientY - rect.top;" +
                        "    const cx = el.scrollLeft + px;" +
                        "    const cy = el.scrollTop + py;" +
                        "    map.style.zoom = newZoom;" +
                        "    const ratio = newZoom / oldZoom;" +
                        "    el.scrollLeft = cx * ratio - px;" +
                        "    el.scrollTop = cy * ratio - py;" +
                        "  }, { passive: false });" +
                        "}"
        );
    }

    void scrollTo(double x, double y) {
        getElement().executeJs(
                "this.scrollTo({left: $0 - this.clientWidth/2, top: $1 - this.clientHeight/2});",
                x, y
        );
    }
}
