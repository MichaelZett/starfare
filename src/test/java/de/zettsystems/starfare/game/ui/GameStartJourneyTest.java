package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** The start of a game in front of the board and the guided first round of a tutorial. */
class GameStartJourneyTest extends AbstractMapJourneyTest {

    @Autowired
    private BattleAcknowledgementService acknowledgements;

    @Test
    void aNewGameBeginsWithWhereTheEmpireStartsAndAgainstWhom() {
        createSharedGame();
        showStartAgain();

        assertThat(text("start-card")).contains("Die Partie beginnt", "Host Empire beginnt in Alpha",
                "1 Gegner, alle von Menschen gespielt").doesNotContain("Eine Runde:");
        assertThat(find(RoundCurtain.class).single().isVisible()).isTrue();
        assertThat(button("Runde abgeben").isEnabled()).as("nothing to plan before the game began").isFalse();
        assertThat(guideShown()).isFalse();

        click("Los geht’s");

        assertThat(find(Div.class).withClassName("start-card").all()).isEmpty();
        assertThat(button("Runde abgeben").isEnabled()).isTrue();
        reloadMap();
        assertThat(find(Div.class).withClassName("start-card").all()).as("seen once").isEmpty();
    }

    @Test
    void theStartOfATutorialSaysWhatARoundIs() {
        createReviewGame(true);
        showStartAgain();

        assertThat(text("start-card")).contains("Eine Runde:", "„Runde abgeben“");
    }

    @Test
    void theTutorialSaysTheNextStepOfTheFirstRound() {
        createReviewGame(true);
        assertThat(guideShown()).isTrue();
        assertThat(step()).isEqualTo("choose_source");

        select("Alpha");
        click("Flotte senden");
        assertThat(step()).isEqualTo("choose_target");

        select("Beta");
        dialogClick("Flotte senden");
        assertThat(step()).isEqualTo("submit");
        assertThat(text("tutorial-guide")).contains("„Runde abgeben“");

        test(find(Button.class).withClassName("tutorial-guide-hide").single()).click();
        assertThat(guideShown()).isFalse();
        reloadMap();
        assertThat(guideShown()).as("hidden for good").isFalse();
    }

    @Test
    void onlyATutorialGuidesTheRound() {
        createReviewGame(false);

        assertThat(guideShown()).isFalse();
    }

    private void showStartAgain() {
        acknowledgements.forgetGame(id);
        reloadMap();
    }

    private boolean guideShown() {
        return find(TutorialGuide.class).exists();
    }

    private String step() {
        return find(TutorialGuide.class).single().getElement().getAttribute("data-step");
    }
}
