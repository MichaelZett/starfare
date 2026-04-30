package de.zettsystems.starfare.social.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.social.application.PresenceTracker;
import org.springframework.stereotype.Component;

/**
 * Hooks every newly created Vaadin UI to the {@link PresenceTracker}. Each UI (typically one
 * browser tab) counts as one connection, so a user with multiple tabs stays online until the
 * last tab closes.
 */
@Component
public class PresenceVaadinBridge implements VaadinServiceInitListener {

    private final PresenceTracker presence;

    public PresenceVaadinBridge(PresenceTracker presence) {
        this.presence = presence;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            String username = UserContext.currentUsername().orElse(null);
            if (username == null) {
                return;
            }
            presence.attach(username);
            uiEvent.getUI().addDetachListener(detach -> presence.detach(username));
        });
    }
}
