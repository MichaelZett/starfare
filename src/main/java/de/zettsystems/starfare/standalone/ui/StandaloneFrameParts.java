package de.zettsystems.starfare.standalone.ui;

import com.vaadin.flow.component.Component;
import de.zettsystems.starfare.game.ui.OwnFrameParts;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Provides the lobby and the frame parts of Starfare's own start to the game's views. */
@Service
class StandaloneFrameParts implements OwnFrameParts {
    private final ObjectProvider<StandaloneLobby> lobbies;

    StandaloneFrameParts(ObjectProvider<StandaloneLobby> lobbies) {
        this.lobbies = lobbies;
    }

    @Override
    public Component lobby() {
        return lobbies.getObject();
    }

    @Override
    public Component languageSwitcher() {
        return new LanguageSwitcher();
    }

    @Override
    public Component accountNavigation() {
        return new AccountNavigation();
    }

    @Override
    public Component legalFooter() {
        return new LegalFooter();
    }
}