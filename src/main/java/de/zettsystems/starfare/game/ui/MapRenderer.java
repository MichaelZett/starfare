package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Renders the map content (system dots, fleet lanes, badges, right-click hit areas)
 * into a target Div. All interaction goes through the provided callbacks so the view
 * stays owner of selection state and refresh flow.
 */
final class MapRenderer {

    private MapRenderer() {
    }

    record Inputs(GameService game, GameId gameId,
                  PlayerViewState view, @Nullable GameState gs,
                  int playerId, boolean observer,
                  @Nullable VisibleSystem selectedFrom,
                  @Nullable Integer highlightedFleetId,
                  Set<Integer> badgesShowingFleetNo,
                  List<VisibleSystem> systems,
                  Consumer<VisibleSystem> onToggleSelect,
                  BiConsumer<VisibleSystem, VisibleSystem> onOpenSend,
                  IntConsumer onBadgeToggleLabel,
                  IntConsumer onFleetHighlight,
                  Runnable onRefresh) {
    }

    static void render(Div map, Inputs in) {
        map.removeAll();
        for (VisibleSystem sys : in.systems()) {
            map.add(buildSystemDot(sys, in));
        }
        for (Fleet f : in.view().ownFleets()) {
            renderFleet(map, f, in);
        }
    }

    private static Div buildSystemDot(VisibleSystem sys, Inputs in) {
        Div dot = new Div();
        dot.addClassName(sys.fullyVisible() ? "sys-own" : "sys-fog");
        if (sys.ownerId() == null) {
            dot.addClassName("sys-neutral");
        }

        String hex = sys.colorHex();
        if (hex != null) {
            applyColoredDotStyle(dot, sys, hex);
        } else {
            applyFogDotStyle(dot, sys);
        }

        dot.setText(systemLabel(sys));
        dot.getElement().setProperty(HtmlAttributes.TITLE, UiMapper.systemTooltip(sys, in.view().turn()));
        dot.getStyle().set(CssProperties.LEFT, sys.x() + "px");
        dot.getStyle().set(CssProperties.TOP, sys.y() + "px");

        if (!in.observer()) {
            attachSystemInteraction(dot, sys, in);
        }
        return dot;
    }

    private static void applyColoredDotStyle(Div dot, VisibleSystem sys, String hex) {
        dot.getStyle().set(CssProperties.BORDER_COLOR, hex);
        dot.getStyle().set(CssProperties.BACKGROUND, sys.fullyVisible() ? hex : hex + "33");
        boolean light = isLightHex(hex);
        dot.getStyle().set(CssProperties.COLOR, light ? "#0b1020" : "#e8eefc");
        dot.getStyle().set(CssProperties.TEXT_SHADOW,
                light ? "0 1px 0 rgba(255,255,255,0.45)" : "0 1px 2px rgba(0,0,0,0.5)");
    }

    private static void applyFogDotStyle(Div dot, VisibleSystem sys) {
        boolean visible = sys.fullyVisible();
        dot.getStyle().set(CssProperties.BORDER_COLOR, visible ? "#f3f6ff" : "rgba(243,246,255,0.7)");
        dot.getStyle().set(CssProperties.BACKGROUND, visible ? "rgba(243,246,255,0.35)" : "rgba(243,246,255,0.22)");
        boolean neutralFog = !visible && sys.ownerId() == null;
        dot.getStyle().set(CssProperties.COLOR, neutralFog ? "#f7fbff" : "#0b1020");
        dot.getStyle().set(CssProperties.TEXT_SHADOW,
                neutralFog ? "0 2px 3px rgba(0,0,0,0.75)" : "0 1px 0 rgba(255,255,255,0.6)");
    }

    private static String systemLabel(VisibleSystem sys) {
        if (!sys.fullyVisible()) {
            return sys.name();
        }
        return String.join("\n", sys.name(), "G:" + sys.garrison(), "P:" + sys.productionPerTurn());
    }

    private static void attachSystemInteraction(Div dot, VisibleSystem sys, Inputs in) {
        VisibleSystem selected = in.selectedFrom();
        boolean isSelected = selected != null && selected.id() == sys.id();
        if (isSelected) {
            dot.addClassName("sys-selected");
        }

        if (sys.fullyVisible()) {
            dot.addClassName("sys-clickable");
            dot.addClickListener(_ -> {
                VisibleSystem from = in.selectedFrom();
                if (from == null) {
                    in.onToggleSelect().accept(sys);
                } else if (from.id() == sys.id()) {
                    in.onToggleSelect().accept(null);
                } else {
                    in.onOpenSend().accept(from, sys);
                }
            });
        } else if (selected != null) {
            dot.addClassName("sys-target");
            final VisibleSystem from = selected;
            dot.addClickListener(_ -> in.onOpenSend().accept(from, sys));
        }
    }

    private static void renderFleet(Div map, Fleet f, Inputs in) {
        VisibleSystem a = findSystem(in, f.fromSystemId()).orElse(null);
        VisibleSystem b = findSystem(in, f.toSystemId()).orElse(null);
        if (a == null || b == null) {
            return;
        }

        double len = Math.hypot(b.x() - a.x(), b.y() - a.y());
        if (len < 1) {
            return;
        }

        LaneGeometry geom = computeLaneGeometry(a, b, len);
        String color = resolveFleetColor(in, f.ownerId());
        String tooltip = fleetTooltip(f, in.view().turn());
        boolean highlighted = in.highlightedFleetId() != null && in.highlightedFleetId() == f.globalId();

        map.add(buildLaneDiv(geom, color, tooltip, highlighted));
        map.add(buildFleetBadge(f, a, b, color, tooltip, in, highlighted));

        if (!in.observer() && in.gs() != null && in.playerId() >= 0) {
            map.add(buildFleetHitArea(f, a.x(), a.y(), len, geom.angle, in));
        }
    }

    /**
     * Linear ETA-progress along the launch→arrival span, clamped to [0, 1].
     * Fresh fleets sit at the source; arriving fleets sit at the target.
     */
    private static double etaProgress(Fleet f, int currentTurn) {
        int total = f.arrivalTurn() - f.launchTurn();
        if (total <= 0) {
            return 1.0;
        }
        int elapsed = currentTurn - f.launchTurn();
        return Math.clamp((double) elapsed / total, 0.0, 1.0);
    }

    private static Optional<VisibleSystem> findSystem(Inputs in, int systemId) {
        return in.systems().stream().filter(ss -> ss.id() == systemId).findFirst();
    }

    private static String resolveFleetColor(Inputs in, int ownerId) {
        return in.view().players().stream()
                .filter(p -> p.id() == ownerId)
                .map(Player::colorHex)
                .findFirst().orElse("#58a6ff");
    }

    private record LaneGeometry(double x1, double y1, double laneLen, double angle) {
    }

    private static LaneGeometry computeLaneGeometry(VisibleSystem a, VisibleSystem b, double len) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double ux = dx / len;
        double uy = dy / len;
        double offset = len > 100 ? 45 : 0;
        double laneLen = Math.max(1, len - 2 * offset);
        double x1 = a.x() + ux * offset;
        double y1 = a.y() + uy * offset;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        return new LaneGeometry(x1, y1, laneLen, angle);
    }

    private static Div buildLaneDiv(LaneGeometry geom, String color, String tooltip, boolean highlighted) {
        Div lane = new Div();
        lane.addClassName("fleet-lane");
        if (highlighted) {
            lane.addClassName("fleet-lane-highlighted");
        }
        lane.getElement().setProperty(HtmlAttributes.TITLE, tooltip);
        lane.getStyle()
                .set("position", "absolute")
                .set("left", geom.x1 + "px")
                .set("top", geom.y1 + "px")
                .set("width", geom.laneLen + "px")
                .set("background", color)
                .set("transform", "rotate(" + geom.angle + "deg)");
        return lane;
    }

    private static Div buildFleetBadge(Fleet f, VisibleSystem a, VisibleSystem b, String color, String tooltip,
                                       Inputs in, boolean highlighted) {
        double progress = etaProgress(f, in.view().turn());
        double mx = a.x() + (b.x() - a.x()) * progress;
        double my = a.y() + (b.y() - a.y()) * progress;
        boolean showFleetNo = in.badgesShowingFleetNo().contains(f.globalId());

        Div badge = new Div();
        badge.addClassName("fleet-badge");
        if (highlighted) {
            badge.addClassName("fleet-badge-highlighted");
        }
        if (showFleetNo) {
            badge.addClassName("fleet-badge-fleetno");
        }
        badge.setText(showFleetNo ? "#" + f.localNo() : String.valueOf(f.ships()));
        badge.getElement().setProperty(HtmlAttributes.TITLE, tooltip);
        badge.getStyle()
                .set("left", mx + "px")
                .set("top", my + "px")
                .set("borderColor", color)
                .set("color", color);

        final int fleetId = f.globalId();
        // Left-click highlights (mirrors the lane behaviour); right-click toggles the
        // ship-count vs fleet-number label and suppresses the browser context menu.
        badge.addClickListener(_ -> {
            in.onFleetHighlight().accept(fleetId);
            in.onRefresh().run();
        });
        badge.getElement().addEventListener("contextmenu", _ -> {
            in.onBadgeToggleLabel().accept(fleetId);
            in.onRefresh().run();
        }).addEventData("event.preventDefault()");
        return badge;
    }

    private static Div buildFleetHitArea(Fleet f, double ax, double ay, double len, double angle, Inputs in) {
        final int pid = in.playerId();
        final int fleetId = f.globalId();
        GameState gs = in.gs();
        if (gs == null) {
            throw new IllegalStateException("buildFleetHitArea requires gs (guarded by caller)");
        }
        boolean launchedThisTurn = gs.fleets().stream()
                .anyMatch(fl -> fl.globalId() == fleetId && fl.launchTurn() == gs.turn());
        boolean waitPending = gs.waitThisTurn().contains(fleetId)
                || gs.pendingOrders().getOrDefault(pid, List.of()).stream()
                .anyMatch(o -> o instanceof FleetOrder.Wait w && w.fleetId() == fleetId);

        Div hitArea = new Div();
        hitArea.addClassName("fleet-lane-hit");
        hitArea.getStyle()
                .set("position", "absolute")
                .set("left", ax + "px")
                .set("top", (ay - 6) + "px")
                .set("width", len + "px")
                .set("transform", "rotate(" + angle + "deg)");
        hitArea.addClickListener(_ -> {
            in.onFleetHighlight().accept(fleetId);
            in.onRefresh().run();
        });

        ContextMenu cm = new ContextMenu(hitArea);
        var waitItem = cm.addItem(I18n.t(waitPending ? UiTexts.MAP_ACTION_WAITING : UiTexts.MAP_ACTION_WAIT), _ -> {
            boolean ok = in.game().setFleetWait(in.gameId(), pid, fleetId);
            if (!ok) {
                Notification.show(I18n.t(UiTexts.MAP_WAIT_FAILED));
            }
            in.onRefresh().run();
        });
        waitItem.setEnabled(!waitPending);
        var disbandItem = cm.addItem(I18n.t(UiTexts.MAP_ACTION_DISBAND), _ -> {
            boolean ok = in.game().disbandFleet(in.gameId(), pid, fleetId);
            if (!ok) {
                Notification.show(I18n.t(UiTexts.MAP_DISBAND_FAILED));
            }
            in.onRefresh().run();
        });
        disbandItem.setEnabled(launchedThisTurn);
        return hitArea;
    }

    private static String fleetTooltip(Fleet f, int currentTurn) {
        int etaTurns = f.arrivalTurn() - currentTurn;
        String etaSuffix;
        if (etaTurns <= 0) {
            etaSuffix = I18n.t(UiTexts.MAP_FLEET_BADGE_ETA_THIS_TURN);
        } else if (etaTurns == 1) {
            etaSuffix = I18n.t(UiTexts.MAP_FLEET_BADGE_ETA_IN_SINGULAR, etaTurns);
        } else {
            etaSuffix = I18n.t(UiTexts.MAP_FLEET_BADGE_ETA_IN_PLURAL, etaTurns);
        }
        return I18n.t(UiTexts.MAP_FLEET_BADGE_BASE, f.ships(), f.arrivalTurn()) + etaSuffix;
    }

    private static boolean isLightHex(String hex) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') {
            return false;
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
            return luminance > 0.7;
        } catch (NumberFormatException _) {
            return false;
        }
    }
}
