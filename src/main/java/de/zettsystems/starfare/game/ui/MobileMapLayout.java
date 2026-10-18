package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.legal.ui.LegalFooter;
import java.util.List;

/**
 * Phone arrangement of the map screen: compact header on top, the map filling the screen with
 * floating zoom buttons, and the sidebar as a sheet over the map. A bottom bar switches between
 * map and sheet; while a target is picked the sheet closes and a banner offers to cancel.
 */
final class MobileMapLayout extends Div {

    private final Div sheet = new Div();
    private final Div targetBanner = new Div();
    private final Button mapTab;
    private final Button panelTab;

    MobileMapLayout(List<Component> top, Component gameOverBanner, MapCanvas canvas, Component actionBar,
                    FleetAndOrdersPanel panel, Runnable onCancelTarget) {
        addClassName("mobile-map");
        var head = new Div(top.toArray(Component[]::new));
        head.addClassName("mobile-map-top");

        canvas.setHeight("100%");
        var zoom = new Div(
                icon(VaadinIcon.PLUS, UiTexts.MAP_ZOOM_IN, () -> canvas.zoom(1.2)),
                icon(VaadinIcon.MINUS, UiTexts.MAP_ZOOM_OUT, () -> canvas.zoom(1 / 1.2)),
                icon(VaadinIcon.HOME, UiTexts.MAP_FIT_EMPIRE, () -> canvas.fit(true)),
                icon(VaadinIcon.EXPAND_SQUARE, UiTexts.MAP_FIT_GALAXY, () -> canvas.fit(false)));
        zoom.addClassName("mobile-map-zoom");

        var cancel = new Button(I18n.t(UiTexts.MAP_TARGET_SELECTION_CANCEL), _ -> onCancelTarget.run());
        cancel.addThemeVariants(ButtonVariant.TERTIARY);
        targetBanner.add(new Span(I18n.t(UiTexts.MAP_TARGET_SELECTION_HINT)), cancel);
        targetBanner.addClassName("mobile-map-target");
        targetBanner.setVisible(false);

        sheet.add(actionBar, panel, new LegalFooter());
        sheet.addClassName("mobile-map-sheet");

        gameOverBanner.getElement().getClassList().add("mobile-map-banner");
        var stage = new Div(gameOverBanner, canvas, zoom, targetBanner, sheet);
        stage.addClassName("mobile-map-stage");

        mapTab = tab(VaadinIcon.MAP_MARKER, UiTexts.MAP_MOBILE_MAP, this::showMap);
        panelTab = tab(VaadinIcon.LIST, UiTexts.MAP_MOBILE_PANEL, this::showPanel);
        var nav = new Div(mapTab, panelTab);
        nav.addClassName("mobile-map-nav");

        add(head, stage, nav);
        showMap();
    }

    void showMap() {
        sheet.setVisible(false);
        select(mapTab, panelTab);
    }

    void showPanel() {
        sheet.setVisible(true);
        select(panelTab, mapTab);
    }

    /** Picking a target happens on the map, so the sheet makes way and the banner explains what to do. */
    void targetSelection(boolean active) {
        targetBanner.setVisible(active);
        if (active) { showMap(); }
    }

    private static void select(Button selected, Button other) {
        selected.addThemeVariants(ButtonVariant.PRIMARY);
        other.removeThemeVariants(ButtonVariant.PRIMARY);
        selected.getElement().setAttribute("aria-pressed", "true");
        other.getElement().setAttribute("aria-pressed", "false");
    }

    private static Button tab(VaadinIcon icon, String key, Runnable action) {
        var button = new Button(I18n.t(key), icon.create(), _ -> action.run());
        button.addClassName("mobile-map-tab");
        return button;
    }

    private static Button icon(VaadinIcon icon, String key, Runnable action) {
        var button = new Button(icon.create(), _ -> action.run());
        button.setAriaLabel(I18n.t(key));
        button.setTooltipText(I18n.t(key));
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }
}
