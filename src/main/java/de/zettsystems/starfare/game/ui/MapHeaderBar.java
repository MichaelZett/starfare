package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.RoundStatus;
import de.zettsystems.starfare.i18n.I18n;
import org.jspecify.annotations.Nullable;

/**
 * Top header bar: game title/round info, empire stats, next-round button and menu.
 * Wiring is fully callback-driven so MainView stays in charge of navigation. The compact variant
 * for phones drops the title and language switcher and moves logistics and legend into the menu.
 */
final class MapHeaderBar extends HorizontalLayout {

    private final Span gameNameLabel = new Span();
    private final Span empireNameLabel = new Span();
    private final Span roundLabel = new Span();
    private final EmpireStatsBar empireStats = new EmpireStatsBar();
    private final de.zettsystems.starfare.economy.ui.EconomyNoticeBar economyNotices = new de.zettsystems.starfare.economy.ui.EconomyNoticeBar();
    de.zettsystems.starfare.economy.ui.EconomyNoticeBar economyNotices() { return economyNotices; }
    private final Button next;
    private final Button chat;
    private final @Nullable Button logistics;
    private final @Nullable MenuItem logisticsItem;
    private final MenuItem researchItem;
    private final MenuItem leaveItem;
    private final MenuItem roundRulesItem;

    record Actions(Runnable onNext, Runnable onToggleLogistics, Runnable onLeave, Runnable onLobby,
                   Runnable onChat, Runnable onEditRoundRules, Runnable onResearch) { }

    MapHeaderBar(Actions actions, boolean compact) {
        gameNameLabel.addClassName("app-header-game");
        empireNameLabel.addClassName("app-header-empire");
        empireNameLabel.setVisible(false);
        roundLabel.addClassName("app-header-round");
        roundLabel.setText(I18n.t(UiTexts.MAP_ROUND_LABEL, 1));

        next = new Button(I18n.t(UiTexts.MAP_SUBMIT_TURN), _ -> actions.onNext().run());
        next.addClassName("round-submit");
        next.setDisableOnClick(true);
        next.addThemeVariants(ButtonVariant.PRIMARY);

        MenuBar headerMenu = new MenuBar();
        headerMenu.addClassName("app-header-menu");
        MenuItem menuRoot = headerMenu.addItem(VaadinIcon.MENU.create());
        menuRoot.getElement().setAttribute("aria-label", "Menü");
        if (compact) {
            logistics = null;
            logisticsItem = menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_LOGISTICS_MODE), _ -> actions.onToggleLogistics().run());
            logisticsItem.setCheckable(true);
            menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_LEGEND), _ -> MapLegend.open());
        } else {
            logisticsItem = null;
            logistics = new Button(I18n.t(UiTexts.MAP_LOGISTICS_MODE), _ -> actions.onToggleLogistics().run());
            logistics.addThemeVariants(ButtonVariant.TERTIARY);
        }
        chat = new Button(I18n.t(UiTexts.GAME_CHAT_TITLE), VaadinIcon.CHAT.create(), _ -> actions.onChat().run());
        chat.addThemeVariants(ButtonVariant.TERTIARY);
        chat.addClassName("map-chat-toggle");
        chat.setAriaLabel(I18n.t(UiTexts.GAME_CHAT_TITLE));
        researchItem = menuRoot.getSubMenu().addItem(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_TITLE), _ -> actions.onResearch().run());
        researchItem.setVisible(false);
        roundRulesItem = menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_ROUND_RULES),
                _ -> actions.onEditRoundRules().run());
        leaveItem = menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_LEAVE), _ -> actions.onLeave().run());
        menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_LOBBY), _ -> actions.onLobby().run());

        var headerLeft = compact
                ? new HorizontalLayout(gameNameLabel, roundLabel, empireStats, economyNotices)
                : new HorizontalLayout(title(), gameNameLabel, empireNameLabel, roundLabel, empireStats, economyNotices);
        headerLeft.setAlignItems(Alignment.BASELINE);
        headerLeft.setSpacing(true);
        headerLeft.addClassName("app-header-left");

        var headerRight = logistics == null
                ? new HorizontalLayout(chat, next, headerMenu)
                : new HorizontalLayout(new LanguageSwitcher(), logistics, chat, next, headerMenu);
        headerRight.setAlignItems(Alignment.CENTER);
        headerRight.setSpacing(true);
        headerRight.addClassName("app-header-right");

        setWidthFull();
        setAlignItems(Alignment.CENTER);
        addClassName("app-header");
        if (compact) { addClassName("app-header-compact"); }
        add(headerLeft, headerRight);
        setFlexGrow(1, headerLeft);
    }

    private static H1 title() {
        H1 title = new H1(I18n.t(UiTexts.MAP_HEADER_TITLE));
        title.addClassName("app-header-title");
        return title;
    }

    void setGameName(String name) {
        gameNameLabel.setText(name);
    }

    void setChatAvailable(boolean permitted, boolean targetSelection) {
        chat.setVisible(permitted);
        chat.setEnabled(permitted && !targetSelection);
        chat.setTooltipText(I18n.t(targetSelection ? UiTexts.CHAT_FINISH_TARGET : UiTexts.GAME_CHAT_TITLE));
    }

    void setEmpireName(String name) {
        empireNameLabel.setText(name);
        empireNameLabel.setVisible(!name.isBlank());
    }

    void setRound(int turn) {
        roundLabel.setText(I18n.t(UiTexts.MAP_ROUND_LABEL, turn));
    }

    void setLeaveVisible(boolean visible) { leaveItem.setVisible(visible); }

    void setResearchVisible(boolean visible) { researchItem.setVisible(visible); }

    void setRoundRulesVisible(boolean visible) {
        roundRulesItem.setVisible(visible);
    }

    void setLeaveText(String text) {
        leaveItem.setText(text);
    }

    void updateTurnAction(RoundStatus status, int playerId, boolean observer, boolean gameOver) {
        boolean submitted = status.hasSubmitted(playerId);
        String key = UiTexts.MAP_SUBMIT_TURN;
        if (observer) {
            key = UiTexts.MAP_NEXT_ROUND;
        } else if (submitted) {
            key = UiTexts.MAP_TURN_SUBMITTED;
        }
        next.setText(I18n.t(key));
        next.setEnabled(!gameOver && !submitted);
    }

    void setNextVisible(boolean visible) {
        next.setVisible(visible);
    }

    void setLogisticsActive(boolean active) {
        if (logisticsItem != null) { logisticsItem.setChecked(active); }
        if (logistics == null) { return; }
        if (active) {
            logistics.addThemeVariants(ButtonVariant.PRIMARY);
        } else {
            logistics.removeThemeVariants(ButtonVariant.PRIMARY);
        }
    }

    void setEmpireStatsVisible(boolean visible) {
        empireStats.setVisible(visible);
    }

    void updateEmpireStats(PlayerViewState view) {
        empireStats.update(view);
    }
}
