package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;

/**
 * The parts only Starfare's own start shows: its lobby, the language switch, the account menu and
 * the legal footer. The own start provides them as a bean (package {@code standalone}); the game
 * artifact carries none, so inside a platform host the host's frame and overview take their place.
 * Views take them as a {@code @Nullable} constructor parameter, which Spring leaves {@code null}
 * when no bean provides them, and then leave them out.
 */
public interface OwnFrameParts {

    /** The lobby with the list of games, shown by the entry route {@link LobbyView}. */
    Component lobby();

    Component languageSwitcher();

    Component accountNavigation();

    Component legalFooter();
}