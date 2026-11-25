package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import de.zettsystems.gamesplatformapi.LifecycleStage;
import de.zettsystems.starfare.game.values.GalaxyExtent;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GalaxySize;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.i18n.I18n;

import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;

/** The picture-card choices of the new-game form: rules, map form, galaxy size and randomness. */
final class SetupCards {
    private SetupCards() {
    }

    /** Variants the account may use; planned ones show but cannot be chosen, alpha and beta carry a badge. */
    static RadioButtonGroup<RulesetDefinition> rules(RulesetCatalog catalog, RulesetOffer offer) {
        List<RulesetDefinition> offered = catalog.definitions().stream()
                .filter(entry -> offer.usable().test(entry.defaultRef())).toList();
        RadioButtonGroup<RulesetDefinition> group = SetupLayout.cards("ruleset-selection", I18n.t(UiTexts.RULESET_CHOOSE),
                offered, offered.stream().filter(RulesetDefinition::creationEnabled).findFirst().orElseThrow(),
                new SetupLayout.Card<>(entry -> I18n.t(entry.nameKey()), entry -> I18n.t(entry.descriptionKey()),
                        entry -> "variant-" + entry.defaultRef().variant(),
                        entry -> stage(entry, offer)));
        group.setItemEnabledProvider(RulesetDefinition::creationEnabled);
        return group;
    }

    static RadioButtonGroup<GalaxyLayout> layout() {
        return SetupLayout.cards("galaxy-layout", I18n.t(UiTexts.LOBBY_FIELD_GALAXY_LAYOUT),
                List.of(GalaxyLayout.values()), GameConfig.DEFAULT_GALAXY_LAYOUT,
                new SetupLayout.Card<>(layout -> I18n.t(layout == GalaxyLayout.EVEN
                        ? UiTexts.LOBBY_GALAXY_LAYOUT_EVEN : UiTexts.LOBBY_GALAXY_LAYOUT_RANDOM),
                        layout -> I18n.t(layout == GalaxyLayout.EVEN
                                ? UiTexts.SETUP_LAYOUT_EVEN_TEXT : UiTexts.SETUP_LAYOUT_RANDOM_TEXT),
                        layout -> "map-" + layout.name().toLowerCase(Locale.ROOT)));
    }

    /** Sizes with fewer than two systems per empire stay visible but cannot be chosen. */
    static RadioButtonGroup<GalaxySize> size(IntSupplier players) {
        RadioButtonGroup<GalaxySize> group = SetupLayout.cards("galaxy-size", I18n.t(UiTexts.SETUP_FIELD_SIZE),
                List.of(GalaxySize.values()), GalaxySize.DEFAULT,
                new SetupLayout.Card<>(SetupCards::sizeName,
                        size -> I18n.t(UiTexts.SETUP_SIZE_TEXT, size.systems(), crossingRounds(size)),
                        size -> "size-" + size.name().toLowerCase(Locale.ROOT),
                        size -> size.fits(players.getAsInt()) ? ""
                                : I18n.t(UiTexts.SETUP_SIZE_TOO_SMALL, players.getAsInt())));
        group.setItemEnabledProvider(size -> size.fits(players.getAsInt()));
        return group;
    }

    static RadioButtonGroup<RandomnessLevel> randomness() {
        return SetupLayout.cards("randomness", I18n.t(UiTexts.SETUP_SECTION_CHANCE),
                List.of(RandomnessLevel.values()), RandomnessLevel.DEFAULT,
                new SetupLayout.Card<>(SetupCards::randomnessName, SetupCards::randomnessText,
                        level -> "chance-" + level.name().toLowerCase(Locale.ROOT)));
    }

    /** Rounds a fleet needs from one corner of the inhabited map to the opposite one. */
    static int crossingRounds(GalaxySize size) {
        GalaxyExtent extent = GalaxyExtent.forSystems(size.systems());
        double diagonal = Math.hypot(extent.width() - 2 * extent.marginX(), extent.height() - 2 * extent.marginY());
        return (int) Math.ceil(diagonal / GameConfig.TRAVEL_SPEED);
    }

    static String sizeName(GalaxySize size) {
        return I18n.t(switch (size) {
            case SMALL -> UiTexts.SETUP_SIZE_SMALL;
            case MEDIUM -> UiTexts.SETUP_SIZE_MEDIUM;
            case LARGE -> UiTexts.SETUP_SIZE_LARGE;
            case HUGE -> UiTexts.SETUP_SIZE_HUGE;
        });
    }

    static String randomnessName(RandomnessLevel level) {
        return I18n.t(switch (level) {
            case NONE -> UiTexts.SETUP_CHANCE_NONE;
            case LOW -> UiTexts.SETUP_CHANCE_LOW;
            case MEDIUM -> UiTexts.SETUP_CHANCE_MEDIUM;
            case HIGH -> UiTexts.SETUP_CHANCE_HIGH;
        });
    }

    private static String randomnessText(RandomnessLevel level) {
        return switch (level) {
            case NONE -> I18n.t(UiTexts.SETUP_CHANCE_NONE_TEXT);
            case LOW -> I18n.t(UiTexts.SETUP_CHANCE_LOW_TEXT, level.combatRandomnessPercent());
            case MEDIUM -> I18n.t(UiTexts.SETUP_CHANCE_MEDIUM_TEXT, level.combatRandomnessPercent());
            case HIGH -> I18n.t(UiTexts.SETUP_CHANCE_HIGH_TEXT, level.combatRandomnessPercent());
        };
    }

    private static String stage(RulesetDefinition entry, RulesetOffer offer) {
        if (!entry.creationEnabled()) {
            return I18n.t(UiTexts.RULESET_PLANNED);
        }
        return offer.marking().apply(entry.defaultRef())
                .filter(stage -> stage == LifecycleStage.ALPHA || stage == LifecycleStage.BETA)
                .map(stage -> stage == LifecycleStage.ALPHA ? "Alpha" : "Beta").orElse("");
    }
}
