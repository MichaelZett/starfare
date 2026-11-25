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
 * for phones drops the title and language switcher.
 */
final class MapHeaderBar extends HorizontalLayout {

    private final Span gameNameLabel = new Span();
    private final Span tutorialMark = TutorialMark.create();
    private final Span empireNameLabel = new Span();
    private final Span roundLabel = new Span();
    private final EmpireStatsBar empireStats;
    private final Button next;
    private final MenuItem leaveItem;
    private final MenuItem resignItem;
    private final MenuItem roundRulesItem;

    record Actions(Runnable onNext, Runnable onLeave, Runnable onLobby,
                   Runnable onEditRoundRules, Runnable onResign) { }

    MapHeaderBar(Actions actions, boolean compact, @Nullable OwnFrameParts frame) {
        empireStats = new EmpireStatsBar();
        gameNameLabel.addClassName("app-header-game");
        tutorialMark.setVisible(false);
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
        roundRulesItem = menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_ROUND_RULES),
                _ -> actions.onEditRoundRules().run());
        leaveItem = menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_LEAVE), _ -> actions.onLeave().run());
        resignItem = menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_RESIGN), _ -> actions.onResign().run());
        resignItem.setVisible(false);
        menuRoot.getSubMenu().addItem(I18n.t(UiTexts.MAP_ACTION_LOBBY), _ -> actions.onLobby().run());

        var headerLeft = compact
                ? new HorizontalLayout(gameNameLabel, tutorialMark, roundLabel, empireStats)
                : new HorizontalLayout(title(), gameNameLabel, tutorialMark, empireNameLabel, roundLabel, empireStats);
        headerLeft.setAlignItems(Alignment.BASELINE);
        headerLeft.setSpacing(true);
        headerLeft.addClassName("app-header-left");

        HorizontalLayout headerRight;
        if (!compact && frame != null) {
            headerRight = new HorizontalLayout(frame.languageSwitcher(), next, headerMenu);
        } else {
            headerRight = new HorizontalLayout(next, headerMenu);
        }
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

    /** Every seat and every observer sees that the game is a tutorial. */
    void setTutorial(boolean tutorial) {
        tutorialMark.setVisible(tutorial);
    }

    void setEmpireName(String name) {
        empireNameLabel.setText(name);
        empireNameLabel.setVisible(!name.isBlank());
    }

    void setRound(int turn) {
        roundLabel.setText(I18n.t(UiTexts.MAP_ROUND_LABEL, turn));
    }

    void setLeaveVisible(boolean visible) { leaveItem.setVisible(visible); }

    /** Only a seated player of a running game can resign. */
    void setResignVisible(boolean visible) { resignItem.setVisible(visible); }

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

    void setEmpireStatsVisible(boolean visible) {
        empireStats.setVisible(visible);
    }

    void updateEmpireStats(PlayerViewState view) {
        empireStats.update(view);
    }
}
