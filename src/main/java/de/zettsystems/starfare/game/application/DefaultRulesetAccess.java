package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import de.zettsystems.starfare.game.config.StandaloneStart;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Testvarianten (im Katalog als {@code testing} markiert) stehen im Plattform-Host nur Testern
 * offen; wer Tester ist, beantwortet die Plattform ({@link PlatformAccounts#isTester}: aktive
 * Systemadministratoren oder Tester genau dieses Spiels). Im eigenen Start ist alles frei. Die
 * Plattform liest Rollen bei jeder Frage frisch, damit eine Änderung auch für laufende Sitzungen gilt.
 */
@Service
public class DefaultRulesetAccess implements RulesetAccess {
    static final String GAME_KEY = "starfare";

    private final RulesetCatalog catalog;
    private final PlatformAccounts accounts;
    private final boolean standalone;

    @Autowired
    public DefaultRulesetAccess(RulesetCatalog catalog, PlatformAccounts accounts) {
        this(catalog, accounts, StandaloneStart.active());
    }

    DefaultRulesetAccess(RulesetCatalog catalog, PlatformAccounts accounts, boolean standalone) {
        this.catalog = catalog;
        this.accounts = accounts;
        this.standalone = standalone;
    }

    @Override
    public boolean mayUse(RulesetRef ruleset, @Nullable String account) {
        return !restricted(ruleset) || tester(account);
    }

    @Override
    public Predicate<RulesetRef> usableBy(@Nullable String account) {
        var tester = new Object() {
            private @Nullable Boolean known;

            boolean get() {
                Boolean value = known;
                if (value == null) {
                    value = tester(account);
                    known = value;
                }
                return value;
            }
        };
        return ruleset -> !restricted(ruleset) || tester.get();
    }

    private boolean restricted(RulesetRef ruleset) {
        return !standalone && catalog.testing(ruleset);
    }

    private boolean tester(@Nullable String account) {
        if (account == null || account.isBlank()) {
            return false;
        }
        AccountId id;
        try {
            id = AccountId.of(Long.parseLong(account));
        } catch (IllegalArgumentException _) {
            // Not a number or not positive: no platform account.
            return false;
        }
        return accounts.isTester(id, GAME_KEY);
    }
}
