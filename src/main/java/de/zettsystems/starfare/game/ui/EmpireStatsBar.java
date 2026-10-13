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
    private final Span metalValue = new Span();
    private final com.vaadin.flow.component.details.Details metalDetails = new com.vaadin.flow.component.details.Details();
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
        metalDetails.addClassName("empire-metal");
        add(metalDetails);
        setVisible(false);
    }

    void update(PlayerViewState view) {
        EmpireStats empire = view.empire();
        systemsValue.setText(String.valueOf(empire.systems()));
        productionValue.setText(String.valueOf(empire.production()));
        shipsValue.setText(String.valueOf(empire.ships()));
        var metal = empire.metal();
        metalDetails.setVisible(metal != null);
        if (metal != null) {
            String breakdown = I18n.t(UiTexts.UX_METAL_BALANCE, metal.extraction(), metal.recycling(), metal.consumption(), metal.next());
            Span amount = new Span(metal.current() + " " + metal.formattedChange());
            amount.addClassName("app-header-stat-value");
            Span summary = new Span(ingot(), amount);
            summary.addClassName("empire-metal-summary");
            summary.getStyle().set("display", "inline-flex").set("align-items", "center").set("gap", ".35rem");
            summary.getElement().setAttribute(HtmlAttributes.ARIA_LABEL,
                    I18n.t(UiTexts.UX_METAL_STOCK, metal.current(), metal.formattedChange()));
            metalDetails.setSummary(summary);
            metalDetails.getElement().setProperty(HtmlAttributes.TITLE, breakdown);
            metalValue.setText(breakdown);
            metalDetails.removeAll();
            metalDetails.add(metalValue);
        }
    }

    /** Raw-material symbol: a metal bar, deliberately unlike the mine silhouette of the mining function. */
    private static Span ingot() {
        Span ingot = new Span();
        ingot.addClassName("metal-ingot-icon");
        ingot.getElement().setAttribute("aria-hidden", "true");
        ingot.getStyle().set("display", "inline-block").set("width", "1.05rem").set("height", ".6rem")
                .set("background", "linear-gradient(180deg, #e3e8ee 0%, #9aa7b5 55%, #6d7987 100%)")
                .set("clip-path", "polygon(18% 0, 82% 0, 100% 100%, 0 100%)");
        return ingot;
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
