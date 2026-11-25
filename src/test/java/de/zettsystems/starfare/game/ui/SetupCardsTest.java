package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.zettsystems.gamesplatformapi.LifecycleStage;
import de.zettsystems.starfare.game.values.GalaxySize;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** The picture cards of the new-game form. */
class SetupCardsTest {

    @Test
    void rulesOfferOnlyUsableVariantsAndPreselectTheFirstPlayableOne() {
        var catalog = RulesetCatalog.builtIn();
        RadioButtonGroup<RulesetDefinition> all = SetupCards.rules(catalog, RulesetOffer.everything());
        assertThat(all.getValue().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(all.getListDataView().getItems().map(RulesetDefinition::defaultRef))
                .containsExactly(RulesetRef.SECTOR_FORCES, RulesetRef.EXTENDED);

        RadioButtonGroup<RulesetDefinition> classicOnly = SetupCards.rules(catalog,
                new RulesetOffer(ruleset -> !ruleset.isSpaceward(), _ -> Optional.empty()));
        assertThat(classicOnly.getListDataView().getItems().map(RulesetDefinition::defaultRef))
                .containsExactly(RulesetRef.SECTOR_FORCES);
    }

    @Test
    void rulesShowOnlyTheirNameAndMarkAlphaOrBetaWithoutAVersion() {
        var catalog = RulesetCatalog.builtIn();
        RadioButtonGroup<RulesetDefinition> rules = SetupCards.rules(catalog, new RulesetOffer(_ -> true,
                ruleset -> ruleset.isSpaceward() ? Optional.of(LifecycleStage.ALPHA) : Optional.empty()));

        Div extended = card(rules, catalog.find("spaceward").orElseThrow());
        Div classic = card(rules, catalog.find("classic").orElseThrow());

        assertThat(text(extended, "setup-card-title")).isEqualTo("Erweitert");
        assertThat(text(extended, "setup-card-status")).isEqualTo("Alpha");
        assertThat(text(classic, "setup-card-title")).isEqualTo("Klassisch");
        assertThat(classic.getChildren().filter(child -> child.hasClassName("setup-card-status"))).isEmpty();
        assertThat(rules.getItemLabelGenerator().apply(catalog.find("classic").orElseThrow())).isEqualTo("Klassisch");
    }

    @Test
    void galaxySizesTooSmallForThePlayersAreMarkedAndCannotBeChosen() {
        AtomicInteger players = new AtomicInteger(3);
        RadioButtonGroup<GalaxySize> sizes = SetupCards.size(players::get);
        assertThat(sizes.getValue()).isEqualTo(GalaxySize.MEDIUM);
        assertThat(sizes.getItemEnabledProvider().test(GalaxySize.SMALL)).isTrue();

        players.set(10);
        assertThat(sizes.getItemEnabledProvider().test(GalaxySize.SMALL)).isFalse();
        assertThat(text(card(sizes, GalaxySize.SMALL), "setup-card-status")).isEqualTo("Zu klein für 10 Spieler");
        assertThat(text(card(sizes, GalaxySize.HUGE), "setup-card-text")).isEqualTo("40 Systeme, quer über die Karte etwa 29 Runden.");
    }

    @Test
    void crossingTheMapTakesLongerOnLargerGalaxies() {
        assertThat(GalaxySize.values()).extracting(SetupCards::crossingRounds).containsExactly(19, 23, 25, 29);
    }

    @SuppressWarnings("unchecked")
    private static <T> Div card(RadioButtonGroup<T> group, T value) {
        return (Div) ((ComponentRenderer<?, T>) group.getItemRenderer()).createComponent(value);
    }

    private static String text(Div card, String className) {
        return card.getChildren().filter(child -> child.hasClassName(className)).map(Span.class::cast)
                .findFirst().orElseThrow().getText();
    }
}
