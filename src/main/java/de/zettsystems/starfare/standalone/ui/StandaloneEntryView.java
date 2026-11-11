package de.zettsystems.starfare.standalone.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.game.ui.LobbyView;
import jakarta.annotation.security.PermitAll;

/**
 * Forwards {@code /} to the lobby in Starfare's own start. Not part of the game artifact: in a
 * host, {@code /} belongs to the host.
 */
@Route("")
@PermitAll
@Tag("div")
public class StandaloneEntryView extends Component implements BeforeEnterObserver {
    private static final long serialVersionUID = 1L;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(LobbyView.class);
    }
}
