package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;

/**
 * What Starfare's own start adds to its lobby (plan M13): friends, invitations, who is online, the
 * chat and the choice whom one shows oneself to. A host keeps these on its own pages and provides
 * none, so the lobby shows only the matches.
 */
public interface LobbyExtras {

    /** New components for one lobby page. */
    Parts create();

    /**
     * @param toolbar a control for the lobby's toolbar
     * @param sidebar the panels beside the match list
     */
    record Parts(Component toolbar, Component sidebar) {
    }
}
