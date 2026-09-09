package de.zettsystems.starfare.game.application;
import de.zettsystems.starfare.game.domain.GameState;
import org.jspecify.annotations.Nullable;
public interface GameAccessPolicy {
    boolean visible(GameState state, @Nullable String host, String account);
    boolean related(GameState state, @Nullable String host, String account);
    boolean canObserve(GameState state, @Nullable String host, String account);
    boolean canReview(GameState state, @Nullable String host, String account);
}
