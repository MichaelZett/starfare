package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Einzige Stelle für die Frage, ob ein Konto eine Spielvariante neu nutzen darf: Partien anlegen
 * (Wizard, Vorlage, Revanche), offene Partien sehen, beitreten, eingeladen werden oder neu
 * zuschauen. Bestehende Teilnahmen, Laden, Züge und Archiv prüft sie nicht.
 */
public interface RulesetAccess {

    boolean mayUse(RulesetRef ruleset, @Nullable String account);

    /** Dieselbe Regel für mehrere Varianten eines Kontos; das Konto wird höchstens einmal gelesen. */
    default Predicate<RulesetRef> usableBy(@Nullable String account) {
        return ruleset -> mayUse(ruleset, account);
    }

    /** Für Tests und Aufrufer ohne Kontenbezug: alle Varianten frei. */
    static RulesetAccess unrestricted() {
        return (_, _) -> true;
    }
}
