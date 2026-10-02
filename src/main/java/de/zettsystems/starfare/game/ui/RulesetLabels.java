package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.i18n.I18n;

/** Shared display formatting; persisted identities never contain localized labels. */
public final class RulesetLabels {
    private RulesetLabels() { }

    public static String label(RulesetCatalog catalog, RulesetRef ref) {
        String name = catalog.find(ref.variant()).map(entry -> I18n.t(entry.nameKey())).orElse(ref.variant());
        return I18n.t(UiTexts.RULESET_LABEL, name, ref.version());
    }
}
