package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Multi-section dialog for creating a new game: system/player counts, neutral-system
 * production bounds, per-player starting production. Calls {@code GameService.newGame}
 * on confirm and runs {@code onCreated} afterwards.
 */
final class CreateGameWizardDialog {

    private CreateGameWizardDialog() {
    }

    static void open(GameService game, PlayerDirectory players, Runnable onCreated) {
        Dialog dialog = new Dialog();
        dialog.addClassName("create-game-dialog");
        dialog.setHeaderTitle(I18n.t(UiTexts.LOBBY_WIZARD_TITLE));
        dialog.setWidth("min(1380px, 96vw)");

        IntegerField systems = intField(I18n.t(UiTexts.LOBBY_FIELD_SYSTEMS),
                GameConfig.MIN_SYSTEM_COUNT, GameConfig.MAX_SYSTEM_COUNT, GameConfig.DEFAULT_SYSTEM_COUNT);
        IntegerField humans = intField(I18n.t(UiTexts.LOBBY_FIELD_HUMANS),
                GameConfig.MIN_HUMAN_PLAYERS, GameConfig.MAX_HUMAN_PLAYERS, GameConfig.DEFAULT_HUMAN_PLAYERS);
        IntegerField ai = intField(I18n.t(UiTexts.LOBBY_FIELD_AI),
                GameConfig.MIN_AI_PLAYERS, GameConfig.MAX_AI_PLAYERS, GameConfig.DEFAULT_AI_PLAYERS);
        TextField gameName = new TextField(I18n.t(UiTexts.LOBBY_FIELD_GAME_NAME));
        gameName.setValue(GameNameGenerator.random());
        gameName.setMaxLength(80);

        IntegerField neutralMinProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_NEUTRAL_MIN_PRODUCTION));
        neutralMinProduction.setMin(GameConfig.MIN_PRODUCTION);
        neutralMinProduction.setMax(GameConfig.MAX_PRODUCTION);
        neutralMinProduction.setValue(GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION);

        IntegerField neutralMaxProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_NEUTRAL_MAX_PRODUCTION));
        neutralMaxProduction.setMin(GameConfig.MIN_PRODUCTION);
        neutralMaxProduction.setMax(GameConfig.MAX_PRODUCTION);
        neutralMaxProduction.setValue(GameConfig.DEFAULT_NEUTRAL_MAX_PRODUCTION);

        IntegerField startGarrison = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_START_GARRISON));
        startGarrison.setMin(GameConfig.MIN_START_GARRISON);
        startGarrison.setMax(GameConfig.MAX_START_GARRISON);
        startGarrison.setValue(GameConfig.DEFAULT_START_GARRISON);

        ComboBox<ProductionDistribution> productionDistribution =
                new ComboBox<>(I18n.t(UiTexts.LOBBY_FIELD_PRODUCTION_DISTRIBUTION));
        productionDistribution.setItems(ProductionDistribution.values());
        productionDistribution.setItemLabelGenerator(CreateGameWizardDialog::labelFor);
        productionDistribution.setValue(GameConfig.DEFAULT_PRODUCTION_DISTRIBUTION);

        ComboBox<GalaxyLayout> galaxyLayout = new ComboBox<>(I18n.t(UiTexts.LOBBY_FIELD_GALAXY_LAYOUT));
        galaxyLayout.setItems(GalaxyLayout.values());
        galaxyLayout.setItemLabelGenerator(CreateGameWizardDialog::labelFor);
        galaxyLayout.setValue(GameConfig.DEFAULT_GALAXY_LAYOUT);

        Checkbox observersAllowed = new Checkbox(I18n.t(UiTexts.LOBBY_FIELD_OBSERVERS_ALLOWED));
        observersAllowed.setValue(GameConfig.DEFAULT_OBSERVERS_ALLOWED);
        Checkbox reentryAllowed = new Checkbox(I18n.t(UiTexts.LOBBY_FIELD_REENTRY_ALLOWED));
        reentryAllowed.setValue(GameConfig.DEFAULT_REENTRY_ALLOWED);
        Checkbox battlePresentation = new Checkbox(I18n.t(UiTexts.LOBBY_FIELD_BATTLE_PRESENTATION));
        battlePresentation.setValue(GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED);
        Input combatRandomness = combatRandomnessSlider();
        Span combatRandomnessValue = new Span();
        combatRandomnessValue.addClassName("combat-randomness-value");
        Runnable updateCombatRandomnessValue = () -> combatRandomnessValue.setText(I18n.t(
                UiTexts.LOBBY_FIELD_COMBAT_RANDOMNESS_VALUE,
                sliderValue(combatRandomness, GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT)));
        combatRandomness.addValueChangeListener(_ -> updateCombatRandomnessValue.run());
        updateCombatRandomnessValue.run();
        Span combatRandomnessHint = new Span(I18n.t(UiTexts.LOBBY_FIELD_COMBAT_RANDOMNESS_HINT));
        combatRandomnessHint.addClassName("combat-randomness-hint");
        Div combatRandomnessField = new Div(new Span(I18n.t(UiTexts.LOBBY_FIELD_COMBAT_RANDOMNESS)),
                combatRandomness, combatRandomnessValue, combatRandomnessHint);
        combatRandomnessField.addClassName("combat-randomness-field");
        Checkbox joinAfterCreate = new Checkbox(I18n.t(UiTexts.LOBBY_WIZARD_JOIN_AFTER_CREATE));
        joinAfterCreate.setId("join-after-create");
        joinAfterCreate.setValue(true);
        String hostPlayerId = UserContext.currentPlayerId().orElse("");
        String hostName = hostPlayerId.isBlank() ? "" : players.displayName(hostPlayerId);
        TextField empireName = new TextField(I18n.t(UiTexts.LOBBY_FIELD_EMPIRE_NAME));
        empireName.setValue(EmpireNameGenerator.forHuman(hostName));
        empireName.setRequiredIndicatorVisible(true);

        FormLayout setupGrid = grid(3, "13em", gameName, systems, humans, ai, galaxyLayout, empireName,
                joinAfterCreate);
        FormLayout settingsGrid = grid(3, "13em", startGarrison, combatRandomnessField,
                observersAllowed, reentryAllowed, battlePresentation);
        FormLayout neutralGrid = grid(3, "16em", neutralMinProduction, neutralMaxProduction, productionDistribution);

        ComboBox<Duration> roundLimit = durationCombo(I18n.t(UiTexts.LOBBY_FIELD_ROUND_LIMIT),
                RoundRules.ROUND_LIMIT_CHOICES, RoundRules.DEFAULT_ROUND_LIMIT);
        ComboBox<Duration> stragglerLimit = durationCombo(I18n.t(UiTexts.LOBBY_FIELD_STRAGGLER_LIMIT),
                RoundRules.STRAGGLER_LIMIT_CHOICES, RoundRules.DEFAULT_STRAGGLER_LIMIT);
        ComboBox<AttackOrder> attackOrder = new ComboBox<>(I18n.t(UiTexts.LOBBY_FIELD_ATTACK_ORDER));
        attackOrder.setItems(AttackOrder.values());
        attackOrder.setItemLabelGenerator(CreateGameWizardDialog::labelFor);
        attackOrder.setValue(RoundRules.DEFAULT_ATTACK_ORDER);
        FormLayout timerGrid = grid(2, "16em", roundLimit, stragglerLimit);
        Div timerSection = wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_ROUNDS),
                I18n.t(UiTexts.LOBBY_WIZARD_SECTION_ROUNDS_HINT, GameConfig.MAX_MISSED_ROUNDS), timerGrid);
        FormLayout combatGrid = grid(1, "16em", attackOrder);
        Div combatSection = wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_COMBAT),
                I18n.t(UiTexts.LOBBY_WIZARD_SECTION_COMBAT_HINT), combatGrid);
        Runnable updateRoundTimerVisibility = () -> timerSection.setVisible(
                valueOrDefault(humans.getValue(), GameConfig.DEFAULT_HUMAN_PLAYERS) > 1);
        humans.addValueChangeListener(_ -> updateRoundTimerVisibility.run());
        updateRoundTimerVisibility.run();

        FormLayout startProductionFields = new FormLayout();
        configureColumns(startProductionFields, 4, "13em");
        startProductionFields.setWidthFull();
        startProductionFields.addClassName("wizard-start-production");

        List<IntegerField> startProductionInputs = new ArrayList<>();
        List<ComboBox<ColorOption>> seatColorInputs = new ArrayList<>();
        Runnable rebuildProductionInputs = () -> rebuildStartProduction(
                startProductionFields, startProductionInputs, seatColorInputs, humans, ai);
        humans.addValueChangeListener(_ -> rebuildProductionInputs.run());
        ai.addValueChangeListener(_ -> rebuildProductionInputs.run());
        rebuildProductionInputs.run();

        Div intro = new Div();
        intro.addClassName("wizard-intro");
        intro.setText(I18n.t(UiTexts.LOBBY_WIZARD_INTRO));

        Div privateHint = new Div();
        privateHint.addClassName("wizard-private-hint");
        privateHint.setText(I18n.t(UiTexts.GAME_PRIVATE_HINT));

        Div sideSections = new Div(
                wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_SETTINGS),
                        I18n.t(UiTexts.LOBBY_WIZARD_SECTION_SETTINGS_HINT), settingsGrid),
                wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_NEUTRAL),
                        I18n.t(UiTexts.LOBBY_WIZARD_SECTION_NEUTRAL_HINT), neutralGrid), timerSection, combatSection);
        sideSections.addClassName("wizard-overview-side");
        Div advancedContent = new Div(sideSections,
                wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_START),
                        I18n.t(UiTexts.LOBBY_WIZARD_SECTION_START_HINT), startProductionFields));
        advancedContent.addClassName("wizard-advanced-content");
        Details advanced = new Details(I18n.t(UiTexts.LOBBY_WIZARD_ADVANCED), advancedContent);
        advanced.addClassName("wizard-advanced");
        Div overview = wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_SETUP),
                I18n.t(UiTexts.LOBBY_WIZARD_SECTION_SETUP_HINT), setupGrid);
        VerticalLayout body = new VerticalLayout(
                privateHint,
                intro,
                overview,
                advanced);
        body.addClassName("wizard-body");
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();
        Div confirmation = new Div();
        confirmation.addClassName("wizard-confirmation");
        confirmation.setVisible(false);
        AtomicReference<GameSetup> confirmedSetup = new AtomicReference<>();
        AtomicReference<Button> createButton = new AtomicReference<>();
        AtomicReference<Button> backButton = new AtomicReference<>();
        Button back = new Button(I18n.t(UiTexts.LOBBY_WIZARD_BACK), _ -> {
            confirmation.removeAll();
            confirmation.setVisible(false);
            body.setVisible(true);
            Button backAction = backButton.get();
            if (backAction != null) {
                backAction.setVisible(false);
            }
            confirmedSetup.set(null);
            Button button = createButton.get();
            if (button != null) {
                button.setText(I18n.t(UiTexts.LOBBY_WIZARD_REVIEW));
                button.setEnabled(true);
            }
        });
        backButton.set(back);
        back.setVisible(false);
        dialog.getFooter().add(back);

        FormInputs formInputs = new FormInputs(systems, humans, ai, startProductionInputs, seatColorInputs,
                neutralMinProduction, neutralMaxProduction, startGarrison,
                observersAllowed, reentryAllowed, battlePresentation, productionDistribution, galaxyLayout,
                combatRandomness, roundLimit, stragglerLimit, attackOrder);
        Button create = new Button(I18n.t(UiTexts.LOBBY_WIZARD_REVIEW));
        createButton.set(create);
        WizardActionContext actionContext = new WizardActionContext(game, onCreated, hostName, gameName, empireName,
                joinAfterCreate, formInputs, seatColorInputs, confirmedSetup, confirmation, body, back, create, dialog);
        create.addClickListener(_ -> handleCreate(actionContext));
        create.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button(I18n.t(UiTexts.LOBBY_WIZARD_CANCEL), _ -> dialog.close());
        dialog.getFooter().add(cancel, create);
        dialog.add(body, confirmation);
        dialog.open();
    }

    private static void handleCreate(WizardActionContext context) {
        GameSetup setup = context.confirmedSetup().get();
        if (setup == null) {
            showConfirmation(context);
            return;
        }
        if (context.empireName().getValue().trim().isEmpty()) {
            context.empireName().setInvalid(true);
            context.empireName().setErrorMessage(I18n.t(UiTexts.LOBBY_EMPIRE_NAME_REQUIRED));
            return;
        }
        if (context.create().isEnabled()) {
            createGame(context, setup);
        }
    }

    private static void showConfirmation(WizardActionContext context) {
        if (context.gameName().getValue().trim().isEmpty()) {
            context.gameName().setInvalid(true);
            context.gameName().setErrorMessage(I18n.t(UiTexts.LOBBY_GAME_NAME_REQUIRED));
            return;
        }
        GameSetup setup = buildSetup(context.formInputs());
        applyNormalizedColors(context.seatColorInputs(), setup);
        context.confirmedSetup().set(setup);
        context.confirmation().removeAll();
        context.confirmation().add(buildConfirmation(setup, context.gameName().getValue().trim()));
        context.body().setVisible(false);
        context.confirmation().setVisible(true);
        context.back().setVisible(true);
        context.create().setText(I18n.t(UiTexts.LOBBY_WIZARD_CREATE));
    }

    private static void createGame(WizardActionContext context, GameSetup setup) {
        context.create().setEnabled(false);
        String accountId = UserContext.currentPlayerId().orElse(null);
        GameId gameId = context.game().newGame(setup, accountId, context.gameName().getValue().trim());
        boolean joined = accountId != null && joinNewGame(context.game(), gameId, accountId,
                context.hostName(), context.empireName().getValue(),
                Boolean.TRUE.equals(context.joinAfterCreate().getValue()));
        boolean started = joined && setup.humanPlayers() == 1 && setup.aiPlayers() > 0
                && context.game().startGame(gameId);
        context.onCreated().run();
        context.dialog().close();
        if (started) {
            context.dialog().getUI().ifPresent(ui -> ui.navigate(MainView.class,
                    new RouteParameters("gameId", gameId.value())));
        }
    }

    private record WizardActionContext(GameService game, Runnable onCreated, String hostName,
                                       TextField gameName, TextField empireName, Checkbox joinAfterCreate,
                                       FormInputs formInputs, List<ComboBox<ColorOption>> seatColorInputs,
                                       AtomicReference<GameSetup> confirmedSetup, Div confirmation,
                                       VerticalLayout body, Button back, Button create, Dialog dialog) {}

    private static void rebuildStartProduction(FormLayout fields, List<IntegerField> inputs, List<ComboBox<ColorOption>> colors,
                                               IntegerField humans, IntegerField ai) {
        fields.removeAll();
        inputs.clear();
        colors.clear();

        int humanCount = Math.max(1, valueOrDefault(humans.getValue(), GameConfig.DEFAULT_HUMAN_PLAYERS));
        for (int i = 1; i <= humanCount; i++) {
            IntegerField humanProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_START_PRODUCTION_HUMAN, i));
            humanProduction.setMin(GameConfig.MIN_PRODUCTION);
            humanProduction.setMax(GameConfig.MAX_PRODUCTION);
            humanProduction.setValue(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION);
            ComboBox<ColorOption> color = buildColorCombo();
            color.setLabel(I18n.t(UiTexts.LOBBY_FIELD_COLOR) + " " + i);
            color.setValue(colorOptions().get((i - 1) % GameConfig.PLAYER_PALETTE.size()));
            fields.add(humanProduction, color);
            inputs.add(humanProduction);
            colors.add(color);
        }

        int aiCount = Math.max(0, valueOrDefault(ai.getValue(), GameConfig.DEFAULT_AI_PLAYERS));
        for (int i = 1; i <= aiCount; i++) {
            IntegerField aiProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_START_PRODUCTION_AI, i));
            aiProduction.setMin(GameConfig.MIN_PRODUCTION);
            aiProduction.setMax(GameConfig.MAX_PRODUCTION);
            aiProduction.setValue(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION);
            ComboBox<ColorOption> color = buildColorCombo();
            color.setLabel(I18n.t(UiTexts.LOBBY_FIELD_COLOR) + " " + (humanCount + i));
            color.setValue(colorOptions().get((humanCount + i - 1) % GameConfig.PLAYER_PALETTE.size()));
            fields.add(aiProduction, color);
            inputs.add(aiProduction);
            colors.add(color);
        }
    }

    private record FormInputs(IntegerField systems, IntegerField humans, IntegerField ai,
                              List<IntegerField> startProductionInputs,
                              List<ComboBox<ColorOption>> seatColorInputs,
                              IntegerField neutralMinProduction, IntegerField neutralMaxProduction,
                              IntegerField startGarrison,
                              Checkbox observersAllowed, Checkbox reentryAllowed, Checkbox battlePresentation,
                              ComboBox<ProductionDistribution> productionDistribution,
                              ComboBox<GalaxyLayout> galaxyLayout,
                              Input combatRandomness,
                              ComboBox<Duration> roundLimit, ComboBox<Duration> stragglerLimit,
                              ComboBox<AttackOrder> attackOrder) {
    }

    private static GameSetup buildSetup(FormInputs in) {
        List<Integer> startProductionPerPlayer = in.startProductionInputs().stream()
                .map(field -> valueOrDefault(field.getValue(), GameConfig.DEFAULT_START_SYSTEM_PRODUCTION))
                .toList();
        List<String> seatColors = in.seatColorInputs().stream()
                .map(field -> field.getValue() == null ? GameConfig.PLAYER_PALETTE.getFirst() : field.getValue().hex())
                .toList();
        return new GameSetup(
                valueOrDefault(in.systems().getValue(), GameConfig.DEFAULT_SYSTEM_COUNT),
                valueOrDefault(in.humans().getValue(), GameConfig.DEFAULT_HUMAN_PLAYERS),
                valueOrDefault(in.ai().getValue(), GameConfig.DEFAULT_AI_PLAYERS),
                startProductionPerPlayer,
                valueOrDefault(in.neutralMinProduction().getValue(), GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION),
                valueOrDefault(in.neutralMaxProduction().getValue(), GameConfig.DEFAULT_NEUTRAL_MAX_PRODUCTION),
                valueOrDefault(in.startGarrison().getValue(), GameConfig.DEFAULT_START_GARRISON),
                in.observersAllowed().getValue(),
                in.reentryAllowed().getValue(),
                seatColors,
                in.productionDistribution().getValue(),
                in.galaxyLayout().getValue(),
                in.battlePresentation().getValue(),
                sliderValue(in.combatRandomness(), GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT),
                new RoundRules(in.roundLimit().getValue(), in.stragglerLimit().getValue(), in.attackOrder().getValue())
        ).normalized();
    }

    private static void applyNormalizedColors(List<ComboBox<ColorOption>> inputs, GameSetup setup) {
        for (int index = 0; index < inputs.size() && index < setup.seatColorHexes().size(); index++) {
            String normalizedHex = setup.seatColorHexes().get(index);
            inputs.get(index).setValue(colorOptions().stream()
                    .filter(option -> option.hex().equals(normalizedHex))
                    .findFirst().orElse(colorOptions().getFirst()));
        }
    }

    private static Div buildConfirmation(GameSetup setup, String gameName) {
        Div content = new Div();
        content.addClassName("wizard-confirmation-content");
        Div summary = new Div();
        summary.addClassName("wizard-summary-grid");
        summary.add(summaryLine(UiTexts.LOBBY_SUMMARY_NAME, gameName),
                summaryLine(UiTexts.LOBBY_SUMMARY_VISIBILITY, I18n.t(UiTexts.GAME_PRIVATE)),
                summaryLine(UiTexts.LOBBY_SUMMARY_SYSTEMS, setup.systemCount()),
                summaryLine(UiTexts.LOBBY_SUMMARY_PLAYERS, setup.humanPlayers(), setup.aiPlayers()),
                summaryLine(UiTexts.LOBBY_SUMMARY_GALAXY, I18n.t(setup.galaxyLayout() == GalaxyLayout.EVEN
                        ? UiTexts.LOBBY_GALAXY_LAYOUT_EVEN : UiTexts.LOBBY_GALAXY_LAYOUT_RANDOM)),
                summaryLine(UiTexts.LOBBY_SUMMARY_GARRISON, setup.startGarrison()),
                summaryLine(UiTexts.LOBBY_SUMMARY_NEUTRAL_PRODUCTION,
                        setup.neutralMinProduction(), setup.neutralMaxProduction()),
                summaryLine(UiTexts.LOBBY_SUMMARY_ROUNDS, DurationLabels.label(setup.roundRules().roundLimit()),
                        DurationLabels.label(setup.roundRules().stragglerLimit())),
                summaryLine(UiTexts.LOBBY_SUMMARY_COMBAT, setup.combatRandomnessPercent(),
                        I18n.t(setup.battlePresentationEnabled() ? UiTexts.LOBBY_SUMMARY_ENABLED
                                : UiTexts.LOBBY_SUMMARY_DISABLED)),
                summaryLine(UiTexts.LOBBY_SUMMARY_RULES, I18n.t(setup.roundRules().attackOrder() == AttackOrder.RANDOM
                        ? UiTexts.LOBBY_ATTACK_ORDER_RANDOM : UiTexts.LOBBY_ATTACK_ORDER_STRONGEST_FIRST)),
                summaryLine(UiTexts.LOBBY_SUMMARY_ACCESS,
                        I18n.t(setup.observersAllowed() ? UiTexts.LOBBY_SUMMARY_OBSERVERS_ALLOWED
                                : UiTexts.LOBBY_SUMMARY_OBSERVERS_BLOCKED),
                        I18n.t(setup.reentryAllowed() ? UiTexts.LOBBY_SUMMARY_REENTRY_ALLOWED
                                : UiTexts.LOBBY_SUMMARY_REENTRY_BLOCKED)));
        for (int index = 0; index < setup.totalPlayers(); index++) {
            summary.add(summaryLine(UiTexts.LOBBY_SUMMARY_START_PRODUCTION,
                    index < setup.humanPlayers() ? I18n.t(UiTexts.LOBBY_SUMMARY_HUMAN, index + 1)
                            : I18n.t(UiTexts.LOBBY_SUMMARY_AI, index - setup.humanPlayers() + 1),
                    setup.startProductionForSeat(index)));
        }
        Div colors = new Div();
        colors.addClassName("wizard-summary-colors");
        for (int index = 0; index < setup.totalPlayers(); index++) {
            Span seat = new Span(I18n.t(index < setup.humanPlayers()
                    ? UiTexts.LOBBY_FIELD_START_PRODUCTION_HUMAN : UiTexts.LOBBY_FIELD_START_PRODUCTION_AI,
                    index < setup.humanPlayers() ? index + 1 : index - setup.humanPlayers() + 1));
            seat.addClassName("wizard-summary-color");
            seat.getStyle().set("--seat-color", setup.colorForSeat(index));
            colors.add(seat);
        }
        content.add(summary, colors, galaxyPreview(setup.galaxyLayout()));
        return content;
    }

    private static Span summaryLine(String key, Object... values) {
        Span line = new Span(I18n.t(key, values));
        line.addClassName("wizard-summary-line");
        return line;
    }

    private static Div galaxyPreview(GalaxyLayout layout) {
        Div preview = new Div();
        preview.addClassName("wizard-galaxy-preview");
        preview.addClassName(layout == GalaxyLayout.EVEN ? "wizard-galaxy-even" : "wizard-galaxy-random");
        preview.add(new Span(I18n.t(UiTexts.LOBBY_SUMMARY_SCHEMATIC)));
        Div stars = new Div();
        stars.addClassName("wizard-galaxy-stars");
        double[][] evenPositions = {{12, 18}, {34, 16}, {57, 19}, {82, 14}, {22, 43}, {48, 42},
                {73, 44}, {12, 72}, {38, 68}, {62, 74}, {86, 69}, {50, 88}};
        double[][] randomPositions = {{8, 20}, {31, 12}, {67, 25}, {85, 11}, {17, 47}, {48, 36},
                {77, 53}, {29, 76}, {58, 66}, {91, 81}, {6, 88}, {48, 91}};
        double[][] positions = layout == GalaxyLayout.EVEN ? evenPositions : randomPositions;
        for (double[] position : positions) {
            Span star = new Span("✦");
            star.addClassName("wizard-galaxy-star");
            star.getStyle().set(CssProperties.LEFT, position[0] + "%");
            star.getStyle().set(CssProperties.TOP, position[1] + "%");
            stars.add(star);
        }
        preview.add(stars);
        return preview;
    }

    private static String labelFor(ProductionDistribution value) {
        return I18n.t(value == ProductionDistribution.GAUSSIAN
                ? UiTexts.LOBBY_PRODUCTION_DISTRIBUTION_GAUSSIAN
                : UiTexts.LOBBY_PRODUCTION_DISTRIBUTION_UNIFORM);
    }

    private static String labelFor(GalaxyLayout value) {
        return I18n.t(value == GalaxyLayout.EVEN
                ? UiTexts.LOBBY_GALAXY_LAYOUT_EVEN
                : UiTexts.LOBBY_GALAXY_LAYOUT_RANDOM);
    }

    private static String labelFor(AttackOrder value) {
        return I18n.t(value == AttackOrder.STRONGEST_FIRST
                ? UiTexts.LOBBY_ATTACK_ORDER_STRONGEST_FIRST
                : UiTexts.LOBBY_ATTACK_ORDER_RANDOM);
    }

    private static ComboBox<Duration> durationCombo(String label, List<Duration> choices, Duration value) {
        ComboBox<Duration> combo = new ComboBox<>(label);
        combo.setItems(choices);
        combo.setItemLabelGenerator(DurationLabels::label);
        combo.setValue(value);
        return combo;
    }

    private static ComboBox<ColorOption> buildColorCombo() {
        ComboBox<ColorOption> color = new ComboBox<>(I18n.t(UiTexts.LOBBY_FIELD_COLOR));
        List<ColorOption> options = colorOptions();
        color.setItems(options);
        color.setItemLabelGenerator(ColorOption::label);
        color.setRenderer(new ComponentRenderer<>(option -> {
            Div swatch = new Div();
            swatch.addClassName("color-swatch");
            swatch.getStyle().set(CssProperties.BACKGROUND, option.hex());
            swatch.getStyle().set(CssProperties.BORDER_COLOR, option.hex());

            Span name = new Span(option.label());
            name.addClassName("color-option-name");
            Span hex = new Span(option.hex());
            hex.addClassName("color-option-hex");

            HorizontalLayout row = new HorizontalLayout(swatch, name, hex);
            row.addClassName("color-option-row");
            row.setPadding(false);
            row.setSpacing(true);
            row.setAlignItems(Alignment.CENTER);
            return row;
        }));
        color.setValue(options.stream()
                .filter(option -> option.hex().equals(GameConfig.PLAYER_PALETTE.getFirst()))
                .findFirst()
                .orElse(options.getFirst()));
        return color;
    }

    private static IntegerField intField(String label, int min, int max, int value) {
        IntegerField field = new IntegerField(label);
        field.setMin(min);
        field.setMax(max);
        field.setValue(value);
        return field;
    }

    private static Input combatRandomnessSlider() {
        Input slider = new Input();
        slider.setType("range");
        slider.setValue(String.valueOf(GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT));
        slider.getElement().setAttribute("min", String.valueOf(GameConfig.MIN_COMBAT_RANDOMNESS_PERCENT));
        slider.getElement().setAttribute("max", String.valueOf(GameConfig.MAX_COMBAT_RANDOMNESS_PERCENT));
        slider.getElement().setAttribute("step", "1");
        slider.getElement().setAttribute("aria-label", I18n.t(UiTexts.LOBBY_FIELD_COMBAT_RANDOMNESS));
        slider.addClassName("combat-randomness-slider");
        return slider;
    }

    private static int sliderValue(Input slider, int fallback) {
        try {
            return Integer.parseInt(slider.getValue());
        } catch (NumberFormatException _) {
            return fallback;
        }
    }

    private static FormLayout grid(int maxColumns, String columnWidth, Component... fields) {
        FormLayout layout = new FormLayout(fields);
        layout.addClassName("wizard-grid");
        configureColumns(layout, maxColumns, columnWidth);
        return layout;
    }

    private static void configureColumns(FormLayout layout, int maxColumns, String columnWidth) {
        layout.setWidthFull();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("34em", Math.min(2, maxColumns)),
                new FormLayout.ResponsiveStep("56em", maxColumns));
        layout.setColumnWidth(columnWidth);
        layout.setMaxColumns(maxColumns);
    }

    private static Div wizardSection(String title, String hint, Component content) {
        Div section = new Div();
        section.addClassName("wizard-section");
        H3 heading = new H3(title);
        heading.addClassName("wizard-section-title");
        Span sub = new Span(hint);
        sub.addClassName("wizard-section-hint");
        section.add(heading, sub, content);
        return section;
    }

    private static int valueOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static boolean joinNewGame(GameService game, GameId gameId, String hostPlayerId, String hostName,
                                       String empireName, boolean joinAfterCreate) {
        if (!joinAfterCreate) {
            return false;
        }
        if (game.joinGame(gameId, hostPlayerId, hostName, empireName).isEmpty()) {
            Notification.show(I18n.t(UiTexts.LOBBY_JOIN_FAILED));
            return false;
        }
        return true;
    }

    private static List<ColorOption> colorOptions() {
        return List.of(
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_COBALT), "#0072B2"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_AMBER), "#E69F00"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_EMERALD), "#009E73"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_VERMILION), "#D55E00"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_MAGENTA), "#CC79A7"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_SKY), "#56B4E9"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_GOLD), "#F0E442"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_CRIMSON), "#C0392B"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_TURQUOISE), "#00A6A6"),
                new ColorOption(I18n.t(UiTexts.LOBBY_COLOR_VIOLET), "#7A4EAB")
        );
    }

    private record ColorOption(String label, String hex) {
    }
}
