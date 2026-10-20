package de.zettsystems.starfare.research.ui;

import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.research.values.ShipCohorts;

public final class ResearchDisplay {
    private ResearchDisplay() { }
    public static Span ships(ShipCohorts composition) {
        String generations = composition.levels().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_GENERATION, entry.getKey(), entry.getValue()))
                .collect(java.util.stream.Collectors.joining(", "));
        return new Span(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_SHIPS, generations, String.format(java.util.Locale.ROOT, "%.1f", composition.strength())));
    }
}
