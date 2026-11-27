package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.i18n.I18n;

/**
 * The start of a game as the first card of the curtain in front of the board: where the player's empire begins and
 * against how many. A tutorial adds what a first round consists of. The rounds run at the same time, so there is no
 * order of play to show.
 */
final class StartCard {
    private StartCard() {
    }

    /** What the card names; {@code computers} counts the opponents the computer plays. */
    record Start(String empire, String home, int opponents, int computers) {
    }

    private static String opponentsKey(Start start) {
        if (start.computers() == 0) {
            return UiTexts.START_CARD_OPPONENTS_HUMAN;
        }
        return start.computers() == start.opponents() ? UiTexts.START_CARD_OPPONENTS_COMPUTER
                : UiTexts.START_CARD_OPPONENTS;
    }

    static Div card(Start start, boolean tutorial, Runnable begin) {
        Span title = new Span(I18n.t(UiTexts.START_CARD_TITLE));
        title.addClassName("round-walkthrough-title");
        Span home = new Span(I18n.t(UiTexts.START_CARD_HOME, start.empire(), start.home()));
        home.addClassName("round-walkthrough-text");
        Span opponents = new Span(I18n.t(opponentsKey(start), start.opponents(), start.computers()));
        opponents.addClassName("start-card-opponents");
        Div card = new Div(title, home, opponents);
        if (tutorial) {
            Span firstRound = new Span(I18n.t(UiTexts.START_CARD_TUTORIAL));
            firstRound.addClassName("round-walkthrough-hint");
            card.add(firstRound);
        }
        Button go = new Button(I18n.t(UiTexts.START_CARD_BEGIN), _ -> begin.run());
        go.addThemeVariants(ButtonVariant.PRIMARY);
        go.addClassName("round-walkthrough-action");
        go.setDisableOnClick(true);
        go.focus();
        card.add(go);
        card.addClassNames("round-walkthrough", "start-card");
        return card;
    }
}
