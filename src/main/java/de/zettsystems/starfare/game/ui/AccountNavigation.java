package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.menubar.MenuBar;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.legal.ui.AccountDeletionView;

/** Shared navigation to the personal areas. */
public final class AccountNavigation extends MenuBar {
    public AccountNavigation() {
        var menu = addItem(I18n.t(UiTexts.ACCOUNT_NAVIGATION)).getSubMenu();
        menu.addItem(I18n.t(UiTexts.MAP_ACTION_LOBBY), _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)));
        menu.addItem(I18n.t(UiTexts.STATISTICS_TITLE), _ -> getUI().ifPresent(ui -> ui.navigate(StatisticsView.class)));
        menu.addItem(I18n.t(UiTexts.ARCHIVE_TITLE), _ -> getUI().ifPresent(ui -> ui.navigate(ArchiveView.class)));
        menu.addItem(I18n.t(UiTexts.LEGAL_DELETE_ACCOUNT), _ -> getUI().ifPresent(ui -> ui.navigate(AccountDeletionView.class)));
    }
}
