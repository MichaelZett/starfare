package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.zettsystems.starfare.game.values.DisplaySetting;
import de.zettsystems.starfare.i18n.I18n;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The guided first moves of a tutorial, next to the button that ends the round: choose an own system, choose the
 * target, then plan more or submit. It follows what the player is doing and says only the next step. Hidden once
 * the player hides it; the choice follows the account ({@link DisplaySetting#TUTORIAL_GUIDE}). Only a tutorial shows
 * it: in a normal game the idea for a move has to come from the player.
 */
final class TutorialGuide extends Div {
    static final String HIDDEN = "hidden";

    /** The next step of a round. */
    enum Step {
        CHOOSE_SOURCE(UiTexts.TUTORIAL_GUIDE_SOURCE),
        CHOOSE_TARGET(UiTexts.TUTORIAL_GUIDE_TARGET),
        SUBMIT(UiTexts.TUTORIAL_GUIDE_SUBMIT);

        private final String textKey;

        Step(String textKey) {
            this.textKey = textKey;
        }
    }

    /** What the seated player of a tutorial can do next; nothing while the round is locked or submitted. */
    record Situation(boolean tutorial, boolean locked, boolean submitted, boolean sourceChosen, int plannedOrders) {
    }

    private final Span text = new Span();

    TutorialGuide() {
        addClassName("tutorial-guide");
        text.addClassName("tutorial-guide-text");
        Button hide = new Button(VaadinIcon.CLOSE_SMALL.create(), _ -> {
            DisplayPreferences.choose(DisplaySetting.TUTORIAL_GUIDE, HIDDEN);
            setVisible(false);
        });
        hide.addThemeVariants(ButtonVariant.TERTIARY);
        hide.addClassName("tutorial-guide-hide");
        hide.setTooltipText(I18n.t(UiTexts.TUTORIAL_GUIDE_HIDE));
        hide.setAriaLabel(I18n.t(UiTexts.TUTORIAL_GUIDE_HIDE));
        add(text, hide);
        setVisible(false);
    }

    static @Nullable Step step(Situation situation, boolean hidden) {
        if (!situation.tutorial() || hidden || situation.locked() || situation.submitted()) {
            return null;
        }
        if (situation.sourceChosen()) {
            return Step.CHOOSE_TARGET;
        }
        return situation.plannedOrders() > 0 ? Step.SUBMIT : Step.CHOOSE_SOURCE;
    }

    void show(Situation situation) {
        Step step = step(situation, HIDDEN.equals(DisplayPreferences.get(DisplaySetting.TUTORIAL_GUIDE, "")));
        setVisible(step != null);
        if (step != null) {
            text.setText(I18n.t(step.textKey));
            getElement().setAttribute("data-step", step.name().toLowerCase(Locale.ROOT));
        }
    }
}
