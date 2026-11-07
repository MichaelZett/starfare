package de.zettsystems.starfare.game.application;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultGameAccessPolicy implements GameAccessPolicy {
    @Override
    public boolean related(GameState state, @Nullable String host, String account) {
        return !account.isBlank() && (account.equals(host) || state.seatByUser().containsKey(account));
    }
    @Override
    public boolean visible(GameState state, @Nullable String host, String account) {
        return state.active() && visible(state.visibility(), state.gameOver(), host, state.seatByUser(),
                state.invitedSeats(), account);
    }
    @Override
    public boolean visible(GameSummary summary, String account) {
        return visible(summary.visibility(), summary.gameOver(), summary.hostPlayerId(), summary.seatByPlayer(),
                summary.invitedSeats(), account);
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
    public boolean canReview(GameState state, @Nullable String host, String account) {
        return state.gameOver() && visible(state, host, account)
                && (related(state, host, account) || state.observersAllowed());
    }
}
