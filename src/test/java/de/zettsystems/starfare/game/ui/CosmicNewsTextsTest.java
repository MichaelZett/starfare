package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CosmicNewsTextsTest {

    @Test
    void everyTickerItemReadsWithoutOpenPlaceholders() {
        for (CosmicNews kind : CosmicNews.values()) {
            String text = RoundEventTexts.text(new TurnEvent.Cosmic(kind, 7, "Deneb", 3, 12, 5, 9, "Wega"), false);

            assertThat(text).as(kind.name()).isNotBlank().doesNotContain("{").doesNotContain("sector.");
        }
    }

    @Test
    void refugeesNameFleetTargetAndArrival() {
        String text = RoundEventTexts.text(new TurnEvent.Cosmic(CosmicNews.REFUGEES, 7, "Deneb", 3, 12, 0, 9, "Wega"), false);

        assertThat(text).contains("Deneb", "12", "F3", "Wega", "9");
    }

    @Test
    void finalScoreListsEveryEmpireAndScoreVictoryHasItsOwnText() {
        String score = RoundEventTexts.text(new TurnEvent.FinalScore(40, List.of(
                new TurnEvent.ScoreLine(1, "Aldebaran", 5, 30, 2, 2790),
                new TurnEvent.ScoreLine(2, "Wega", 4, 10, 0, 1850))), false);
        String victory = RoundEventTexts.text(new TurnEvent.Victory(1, TurnEvent.Victory.BY_SCORE), false);

        assertThat(score.replaceAll("[.,  ]", "")).contains("40", "Aldebaran", "2790", "Wega", "1850");
        assertThat(victory).isNotBlank().doesNotContain("%");
    }
}
