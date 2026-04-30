package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;

/**
 * Header-bar widget showing the current player's system count, production and ships.
 * Call {@link #update(PlayerViewState, GameState, int)} on each refresh; the bar
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

    void update(PlayerViewState view, GameState gs, int playerId) {
        int ownSystems = 0;
        int ownProduction = 0;
        int ownGarrison = 0;
        for (var s : gs.systems()) {
            Integer ownerId = s.ownerId();
            if (ownerId != null && ownerId == playerId) {
                ownSystems++;
                ownProduction += s.productionPerTurn();
                ownGarrison += s.garrison();
            }
        }
        int inTransit = view.ownFleets().stream().mapToInt(Fleet::ships).sum();
        systemsValue.setText(String.valueOf(ownSystems));
        productionValue.setText(String.valueOf(ownProduction));
        shipsValue.setText(String.valueOf(ownGarrison + inTransit));
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
