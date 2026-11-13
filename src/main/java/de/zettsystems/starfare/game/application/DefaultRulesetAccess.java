package de.zettsystems.starfare.game.application;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.Scope;
import de.zettsystems.identity.values.UserAccountDto;
import de.zettsystems.starfare.game.config.StandaloneStart;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Testvarianten (im Katalog als {@code testing} markiert) stehen im Plattform-Host nur Testern
 * offen: aktive Konten mit globaler Rolle {@code SYSTEM_ADMIN} oder mit {@code GAME_TESTER} im
 * Bereich {@code game:starfare} – dieselbe Regel wie im Host. Ein globaler {@code GAME_TESTER}
 * oder der eines anderen Spiels reicht nicht. Im eigenen Start ist alles frei. Rollen werden bei
 * jeder Frage frisch gelesen, damit eine Änderung auch für laufende Sitzungen gilt.
 */
@Service
public class DefaultRulesetAccess implements RulesetAccess {
    static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    static final String GAME_TESTER = "GAME_TESTER";
    static final Scope TESTER_SCOPE = Scope.of("game", "starfare");

    private final RulesetCatalog catalog;
    private final UserAccountService accounts;
    private final boolean standalone;

    @Autowired
    public DefaultRulesetAccess(RulesetCatalog catalog, UserAccountService accounts) {
        this(catalog, accounts, StandaloneStart.active());
    }

    DefaultRulesetAccess(RulesetCatalog catalog, UserAccountService accounts, boolean standalone) {
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
        long id;
        try {
            id = Long.parseLong(account);
        } catch (NumberFormatException _) {
            return false;
        }
        return accounts.findById(id).filter(UserAccountDto::enabled).filter(DefaultRulesetAccess::isTester).isPresent();
    }

    private static boolean isTester(UserAccountDto account) {
        return account.hasRole(SYSTEM_ADMIN) || account.roleAssignments().stream()
                .anyMatch(role -> role.roleCode().equals(GAME_TESTER) && TESTER_SCOPE.equals(role.scope()));
    }
}
