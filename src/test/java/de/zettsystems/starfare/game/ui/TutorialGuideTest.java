package de.zettsystems.starfare.game.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Which step of a round the tutorial names, and when it keeps quiet. */
class TutorialGuideTest {

    @Test
    void theStepFollowsWhatThePlayerHasDone() {
        assertThat(TutorialGuide.step(situation(false, 0), false)).isEqualTo(TutorialGuide.Step.CHOOSE_SOURCE);
        assertThat(TutorialGuide.step(situation(true, 0), false)).isEqualTo(TutorialGuide.Step.CHOOSE_TARGET);
        assertThat(TutorialGuide.step(situation(true, 2), false)).as("a new fleet is being planned")
                .isEqualTo(TutorialGuide.Step.CHOOSE_TARGET);
        assertThat(TutorialGuide.step(situation(false, 1), false)).isEqualTo(TutorialGuide.Step.SUBMIT);
    }

    @Test
    void noStepOutsideATutorialWhileLockedAfterSubmittingOrOnceHidden() {
        assertThat(TutorialGuide.step(new TutorialGuide.Situation(false, false, false, false, 0), false)).isNull();
        assertThat(TutorialGuide.step(new TutorialGuide.Situation(true, true, false, false, 0), false)).isNull();
        assertThat(TutorialGuide.step(new TutorialGuide.Situation(true, false, true, false, 1), false)).isNull();
        assertThat(TutorialGuide.step(situation(false, 0), true)).isNull();
    }

    private static TutorialGuide.Situation situation(boolean sourceChosen, int plannedOrders) {
        return new TutorialGuide.Situation(true, false, false, sourceChosen, plannedOrders);
    }
}
