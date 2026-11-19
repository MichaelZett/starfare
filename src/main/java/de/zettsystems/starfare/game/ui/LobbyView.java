package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;

/**
 * Starfare's entry route. A platform host forwards it to its own game overview (game.properties,
 * {@code platformLobby}), so the game artifact carries no lobby of its own; Starfare's own start
 * fills the route with its lobby ({@link OwnFrameParts#lobby()}).
 */
@Route(StarfareRoutes.LOBBY)
@CssImport("./styles/starfare.css")
@PermitAll
public class LobbyView extends VerticalLayout {

    /** Spring passes {@code null} when no bean provides the parts, as inside a platform host. */
    public LobbyView(@Nullable OwnFrameParts frame) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        if (frame != null) {
            add(frame.lobby());
        }
    }
}