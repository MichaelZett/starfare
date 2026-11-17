package de.zettsystems.starfare.standalone.account;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.AccountRemoved;
import de.zettsystems.identity.values.AccountDeleted;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Announces a deleted zs-identity account to the game as games-platform-api {@link AccountRemoved},
 * as a platform host does: synchronously inside identity's deleting transaction, so that a failure
 * in the game rolls the deletion back.
 */
@Component
class IdentityAccountRemovals {
    private final ApplicationEventPublisher events;

    IdentityAccountRemovals(ApplicationEventPublisher events) {
        this.events = events;
    }

    @EventListener
    @Order(0)
    @Transactional(propagation = Propagation.MANDATORY)
    public void accountDeleted(AccountDeleted event) {
        events.publishEvent(new AccountRemoved(AccountId.of(event.userId())));
    }
}
