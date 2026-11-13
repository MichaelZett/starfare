package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.menubar.MenuBar;
import de.zettsystems.starfare.i18n.I18n;

/** Navigation to the personal areas in Starfare's own start; a platform host shows its frame instead. */
public final class AccountNavigation extends MenuBar {
    /** The starting application owns this page; Starfare's own start serves it from legal/ui. */
    static final String ACCOUNT_DELETION = "account/delete";

    public AccountNavigation() {
        var menu = addItem(I18n.t(UiTexts.ACCOUNT_NAVIGATION)).getSubMenu();
        menu.addItem(I18n.t(UiTexts.MAP_ACTION_LOBBY), _ -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)));
        menu.addItem(I18n.t(UiTexts.STATISTICS_TITLE), _ -> getUI().ifPresent(ui -> ui.navigate(StatisticsView.class)));
        menu.addItem(I18n.t(UiTexts.ARCHIVE_TITLE), _ -> getUI().ifPresent(ui -> ui.navigate(ArchiveView.class)));
        menu.addItem(I18n.t(UiTexts.LEGAL_DELETE_ACCOUNT), _ -> getUI().ifPresent(ui -> ui.navigate(ACCOUNT_DELETION)));
    }
}
