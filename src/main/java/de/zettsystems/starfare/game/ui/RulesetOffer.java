package de.zettsystems.starfare.game.ui;

import de.zettsystems.gamesplatformapi.LifecycleStage;
import de.zettsystems.starfare.game.application.RulesetAccess;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The variants the wizard offers to an account ({@code usable}) and the stage each is marked with
 * where it is chosen ({@code marking}: alpha or beta, empty otherwise).
 */
public record RulesetOffer(Predicate<RulesetRef> usable, Function<RulesetRef, Optional<LifecycleStage>> marking) {
    public RulesetOffer {
        Objects.requireNonNull(usable, "usable");
        Objects.requireNonNull(marking, "marking");
    }

    /** Every variant, none marked. */
    public static RulesetOffer everything() {
        return new RulesetOffer(_ -> true, _ -> Optional.empty());
    }

    /** What {@code access} allows the account, marked by the platform's stage of each variant. */
    public static RulesetOffer of(RulesetAccess access, @Nullable String account) {
        return new RulesetOffer(access.usableBy(account), access::marking);
    }
}
