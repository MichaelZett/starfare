package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.LifecycleStage;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import de.zettsystems.starfare.game.config.StandaloneStart;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Im Plattform-Host entscheidet die Plattform je Spielvariante ({@link PlatformAccounts#mayUseVariant}:
 * Stufe der Variante, etwa Spaceward in Alpha, und Rolle des Kontos); Starfare meldet die Varianten mit
 * ihrer Startstufe in {@code games/starfare/game.properties}. Ohne Plattformkonto ist nichts frei. Im
 * eigenen Start ist alles frei. Die Plattform liest Stufen und Rollen bei jeder Frage frisch, damit eine
 * Änderung auch für laufende Sitzungen gilt.
 */
@Service
public class DefaultRulesetAccess implements RulesetAccess {
    static final String GAME_KEY = "starfare";

    private final PlatformAccounts accounts;
    private final boolean standalone;

    @Autowired
    public DefaultRulesetAccess(PlatformAccounts accounts) {
        this(accounts, StandaloneStart.active());
    }

    DefaultRulesetAccess(PlatformAccounts accounts, boolean standalone) {
        this.accounts = accounts;
        this.standalone = standalone;
    }

    @Override
    public boolean mayUse(RulesetRef ruleset, @Nullable String account) {
        return standalone || platformAccount(account).map(id -> mayUseVariant(id, ruleset)).orElse(false);
    }

    @Override
    public Predicate<RulesetRef> usableBy(@Nullable String account) {
        if (standalone) {
            return _ -> true;
        }
        Optional<AccountId> id = platformAccount(account);
        if (id.isEmpty()) {
            return _ -> false;
        }
        Map<String, Boolean> byVariant = new HashMap<>();
        return ruleset -> byVariant.computeIfAbsent(ruleset.variant(), _ -> mayUseVariant(id.orElseThrow(), ruleset));
    }

    @Override
    public Optional<LifecycleStage> marking(RulesetRef ruleset) {
        if (standalone) {
            return Optional.empty();
        }
        LifecycleStage stage = accounts.variantStage(GAME_KEY, ruleset.variant());
        return switch (stage) {
            case ALPHA, BETA -> Optional.of(stage);
            default -> Optional.empty();
        };
    }

    private boolean mayUseVariant(AccountId account, RulesetRef ruleset) {
        return accounts.mayUseVariant(account, GAME_KEY, ruleset.variant());
    }

    private static Optional<AccountId> platformAccount(@Nullable String account) {
        if (account == null || account.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(AccountId.of(Long.parseLong(account)));
        } catch (IllegalArgumentException _) {
            // Not a number or not positive: no platform account.
            return Optional.empty();
        }
    }
}
