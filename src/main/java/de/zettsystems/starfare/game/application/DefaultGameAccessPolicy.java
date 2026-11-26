package de.zettsystems.starfare.game.application;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Who sees and plays a game. On top of the game's own rules, an account that may no longer use the
 * variant of the rules (the platform downgraded it, or took the account's role away) reaches none of its
 * games any more: no map, no review, no chat, no orders. That is the platform's rule for a whole game,
 * here for a variant ({@link RulesetAccess}).
 */
@Component
public class DefaultGameAccessPolicy implements GameAccessPolicy {
    private final RulesetAccess variants;

    /** Every variant open: for tests and callers without a platform. */
    public DefaultGameAccessPolicy() {
        this(RulesetAccess.unrestricted());
    }

    @Autowired
    public DefaultGameAccessPolicy(RulesetAccess variants) {
        this.variants = variants;
    }

    @Override
    public boolean related(GameState state, @Nullable String host, String account) {
        return !account.isBlank() && (account.equals(host) || state.seatByUser().containsKey(account));
    }
    @Override
    public boolean visible(GameState state, @Nullable String host, String account) {
        return visible(state.visibility(), state.gameOver(), host, state.seatByUser(),
                state.invitedSeats(), account) && variants.mayUse(state.ruleset(), account);
    }
    @Override
    public boolean visible(GameSummary summary, String account) {
        return visible(summary, account, ruleset -> variants.mayUse(ruleset, account));
    }
    @Override
    public boolean visible(GameSummary summary, String account, Predicate<RulesetRef> usableVariants) {
        return visible(summary.visibility(), summary.gameOver(), summary.hostPlayerId(), summary.seatByPlayer(),
                summary.invitedSeats(), account) && usableVariants.test(summary.ruleset());
    }
    private static boolean visible(GameVisibility visibility, boolean gameOver, @Nullable String host,
                                   Map<String, Integer> seats, Map<String, Integer> invited, String account) {
        return !account.isBlank() && (visibility == GameVisibility.PUBLIC
                || account.equals(host) || seats.containsKey(account)
                || (!gameOver && invited.containsKey(account)));
    }
    @Override
    public boolean canObserve(GameState state, @Nullable String host, String account) {
        return visible(state, host, account) && !state.gameOver() && state.observersAllowed()
                && (state.visibility() == GameVisibility.PUBLIC || related(state, host, account));
    }
    @Override
    public boolean mayReadChat(GameState state, String account) {
        return !account.isBlank() && state.seatByUser().containsKey(account) && variants.mayUse(state.ruleset(), account);
    }
    @Override
    public boolean mayWriteChat(GameState state, String account) {
        return !account.isBlank() && !state.gameOver() && state.activeSeat(account) != null
                && variants.mayUse(state.ruleset(), account);
    }
    @Override
    public boolean mayCommand(GameState state, String account) {
        return !account.isBlank() && state.started() && !state.gameOver() && state.activeSeat(account) != null
                && variants.mayUse(state.ruleset(), account);
    }
    @Override
    public boolean canReview(GameState state, @Nullable String host, String account) {
        return state.gameOver() && visible(state, host, account)
                && (related(state, host, account) || state.observersAllowed());
    }
}
