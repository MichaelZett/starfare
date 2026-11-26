package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The report of a round change as a curtain in front of the whole board (K8a): it darkens map, sidebar and header,
 * leaves only the site of the open event bright and holds the event card. Nothing behind it takes a click or the
 * keyboard focus ({@code inert}) until the last event is confirmed; the services refuse orders meanwhile anyway.
 */
final class RoundCurtain extends Div {

    static final String CLASS_NAME = "round-curtain";

    /**
     * Follows the event's system on the map, even while the map scrolls there or redraws, and cuts the bright spot
     * into the curtain at its place; outside the visible map there is no spot. Runs until the curtain is hidden.
     */
    private static final String FOLLOW_SITE = """
            const curtain = this;
            if (curtain.__starfareSpot) return;
            curtain.__starfareSpot = true;
            const follow = () => {
              if (!curtain.isConnected || curtain.hidden) { curtain.__starfareSpot = false; return; }
              const root = curtain.parentElement;
              const site = root && root.querySelector('#map .sys-report-event-active');
              const scroll = root && root.querySelector('#scroll');
              const box = curtain.getBoundingClientRect();
              let radius = 0;
              if (site && scroll) {
                const r = site.getBoundingClientRect(), view = scroll.getBoundingClientRect();
                const x = r.left + r.width / 2, y = r.top + r.height / 2;
                if (x >= view.left && x <= view.right && y >= view.top && y <= view.bottom) {
                  curtain.style.setProperty('--spot-x', (x - box.left) + 'px');
                  curtain.style.setProperty('--spot-y', (y - box.top) + 'px');
                  radius = Math.max(56, r.width * 0.75 + 24);
                }
              }
              curtain.style.setProperty('--spot-r', radius + 'px');
              requestAnimationFrame(follow);
            };
            requestAnimationFrame(follow);
            """;

    private final List<Component> board;

    /** {@code board}: everything the curtain lies in front of, made inert while it is shown. */
    RoundCurtain(List<Component> board) {
        this.board = List.copyOf(board);
        addClassName(CLASS_NAME);
        getElement().setAttribute("role", "dialog");
        getElement().setAttribute("aria-modal", "true");
        setVisible(false);
    }

    /** Shows the card of the open event in front of the board, or lifts the curtain with {@code null}. */
    void show(@Nullable Component card) {
        removeAll();
        boolean open = card != null;
        if (open) { add(card); }
        if (open != isVisible()) {
            setVisible(open);
            board.forEach(part -> part.getElement().setAttribute("inert", open));
        }
        if (open) { getElement().executeJs(FOLLOW_SITE); }
    }
}
