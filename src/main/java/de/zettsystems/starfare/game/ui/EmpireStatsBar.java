package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.values.EmpireStats;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;

/**
 * Header-bar widget showing the current player's system count, production and ships.
 * Call {@link #update(PlayerViewState)} on each refresh; the bar
 * hides itself when the view is observing-only.
 */
final class EmpireStatsBar extends HorizontalLayout {

    private final Span systemsValue = new Span("0");
    private final Span productionValue = new Span("0");
    private final Span shipsValue = new Span("0");

    EmpireStatsBar() {
        addClassName("app-header-stats");
        setSpacing(false);
        setPadding(false);
        add(
                buildStat(VaadinIcon.GLOBE, systemsValue, I18n.t(UiTexts.MAP_STATS_SYSTEMS_TOOLTIP)),
                buildStat(VaadinIcon.COGS, productionValue, I18n.t(UiTexts.MAP_STATS_PRODUCTION_TOOLTIP)),
                buildStat(VaadinIcon.ROCKET, shipsValue, I18n.t(UiTexts.MAP_STATS_SHIPS_TOOLTIP))
        );
        setVisible(false);
    }

    void update(PlayerViewState view) {
        EmpireStats empire = view.empire();
        systemsValue.setText(String.valueOf(empire.systems()));
        productionValue.setText(String.valueOf(empire.production()));
        shipsValue.setText(String.valueOf(empire.ships()));
    }

    private static Span buildStat(VaadinIcon icon, Span valueSpan, String tooltip) {
        Span wrapper = new Span();
        wrapper.addClassName("app-header-stat");
        wrapper.getElement().setProperty(HtmlAttributes.TITLE, tooltip);
        var iconEl = icon.create();
        iconEl.addClassName("app-header-stat-icon");
        valueSpan.addClassName("app-header-stat-value");
        wrapper.add(iconEl, valueSpan);
        return wrapper;
    }
}
