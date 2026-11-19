package de.zettsystems.starfare.standalone.ui;

import com.vaadin.flow.component.menubar.MenuBar;
import de.zettsystems.starfare.game.ui.LobbyView;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;

/** Navigation to the personal areas in Starfare's own start; a platform host shows its frame instead. */
public final class AccountNavigation extends MenuBar {
    public AccountNavigation() {
        var menu = addItem(I18n.t(UiTexts.ACCOUNT_NAVIGATION)).getSubMenu();
        menu.addItem(I18n.t(UiTexts.MAP_ACTION_LOBBY), _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)));
        menu.addItem(I18n.t(UiTexts.ARCHIVE_TITLE), _ -> getUI().ifPresent(ui -> ui.navigate(ArchiveView.class)));
    }
}
