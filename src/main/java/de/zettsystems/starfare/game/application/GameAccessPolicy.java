package de.zettsystems.starfare.game.application;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
public interface GameAccessPolicy {
    boolean visible(GameState state, @Nullable String host, String account);
    /** Same rule for the summary of an active game whose state is not loaded (an unloaded game). */
    boolean visible(GameSummary summary, String account);
    /**
     * The same rule with the variants the account may use already asked ({@link RulesetAccess#usableBy}), so a list
     * of many games asks the platform once per variant, not once per game.
     */
    boolean visible(GameSummary summary, String account, Predicate<RulesetRef> usableVariants);
    boolean related(GameState state, @Nullable String host, String account);
    boolean canObserve(GameState state, @Nullable String host, String account);
    boolean canReview(GameState state, @Nullable String host, String account);
    /** Everybody seated reads the chat, also after leaving, resigning or losing the seat; observers do not. */
    boolean mayReadChat(GameState state, String account);
    /** Only who still plays writes, from the waiting room until the game ends. */
    boolean mayWriteChat(GameState state, String account);
    /** Orders come only from a seat that still plays a started game that is not over. */
    boolean mayCommand(GameState state, String account);
}
