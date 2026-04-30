package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.i18n.I18n;

/**
 * Top header bar: game title/round info, empire stats, next-round button and menu.
 * Wiring is fully callback-driven so MainView stays in charge of navigation.
 */
final class MapHeaderBar extends HorizontalLayout {

    private final Span gameNameLabel = new Span();
    private final Span roundLabel = new Span();
    private final EmpireStatsBar empireStats = new EmpireStatsBar();
    private final Button next;
    private final MenuItem leaveItem;

    MapHeaderBar(Runnable onNext, Runnable onLeave, Runnable onLobby) {
        H1 title = new H1(I18n.t(UiTexts.MAP_HEADER_TITLE));
        title.addClassName("app-header-title");
        gameNameLabel.addClassName("app-header-game");
        roundLabel.addClassName("app-header-round");
        roundLabel.setText(I18n.t(UiTexts.MAP_ROUND_LABEL, 1));

        next = new Button(I18n.t(UiTexts.MAP_NEXT_ROUND), _ -> onNext.run());
        next.addThemeVariants(ButtonVariant.PRIMARY);

        MenuBar headerMenu = new MenuBar();
        headerMenu.addClassName("app-header-menu");
        MenuItem menuRoot = headerMenu.addItem(VaadinIcon.MENU.create());
        menuRoot.getElement().setAttribute("aria-label", "Menü");
        leaveItem = menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_LEAVE), _ -> onLeave.run());
        menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_LOBBY), _ -> onLobby.run());

        var headerLeft = new HorizontalLayout(title, gameNameLabel, roundLabel, empireStats);
        headerLeft.setAlignItems(Alignment.BASELINE);
        headerLeft.setSpacing(true);
        headerLeft.addClassName("app-header-left");

        var headerRight = new HorizontalLayout(new LanguageSwitcher(), next, headerMenu);
        headerRight.setAlignItems(Alignment.CENTER);
        headerRight.setSpacing(true);
        headerRight.addClassName("app-header-right");

        setWidthFull();
        setAlignItems(Alignment.CENTER);
        addClassName("app-header");
        add(headerLeft, headerRight);
        setFlexGrow(1, headerLeft);
    }

    void setGameName(String name) {
        gameNameLabel.setText(name);
    }

    void setRound(int turn) {
        roundLabel.setText(I18n.t(UiTexts.MAP_ROUND_LABEL, turn));
    }

    void setLeaveText(String text) {
        leaveItem.setText(text);
    }

    void setNextEnabled(boolean enabled) {
        next.setEnabled(enabled);
    }

    void setNextVisible(boolean visible) {
        next.setVisible(visible);
    }

    void setEmpireStatsVisible(boolean visible) {
        empireStats.setVisible(visible);
    }

    void updateEmpireStats(PlayerViewState view, GameState gs, int playerId) {
        empireStats.update(view, gs, playerId);
    }
}
