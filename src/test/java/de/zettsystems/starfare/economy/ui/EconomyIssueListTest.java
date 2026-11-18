package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import de.zettsystems.starfare.testsupport.ColonyStates;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyIssueListTest extends AbstractEconomyUiTest {
    @Test
    void aNoticeOpensTheFilteredListWhoseCardLeadsToTheColony() {
        openGame(ColonyStates.readyWithMoon(), "Economy issues");

        click("economy-notice-labor");

        assertThat(find(H2.class).all()).extracting(H2::getText).contains("Arbeitskräfte fehlen · 1 System");
        Component card = find(Component.class).withClassName("economy-issue-card").single();
        assertThat(elementText(card.getElement())).contains("Alpha", "Arbeitskräfte fehlen · 1 System", "Schiffe/Runde");
        Button colony = find(Button.class).withText("Kolonie ansehen").single();
        assertThat(find(Button.class).withText("Produktion ändern").all()).isEmpty();

        test(colony).click();
        runPendingUiAccess();

        assertThat(text("colony-details")).contains("Nutzbare Industrie: 6 von 10");
    }

    @Test
    void clearingTheFilterReturnsToTheLogisticsOverview() {
        openGame(ColonyStates.readyWithMoon(), "Economy issues cleared");
        click("economy-notice-labor");
        assertThat(find(Component.class).withClassName("economy-issue-card").all()).hasSize(1);

        test(find(Button.class).withText("Filter aufheben").single()).click();
        runPendingUiAccess();

        assertThat(find(Component.class).withClassName("economy-issue-card").all()).isEmpty();
        assertThat(find(H2.class).all()).extracting(H2::getText).doesNotContain("Arbeitskräfte fehlen · 1 System");
    }
}
