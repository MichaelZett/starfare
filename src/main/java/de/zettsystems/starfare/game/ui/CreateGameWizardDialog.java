package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameNameGenerator;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-section dialog for creating a new game: system/player counts, neutral-system
 * production bounds, per-player starting production. Calls {@code GameService.newGame}
 * on confirm and runs {@code onCreated} afterwards.
 */
final class CreateGameWizardDialog {

    private CreateGameWizardDialog() {
    }

    static void open(GameService game, Runnable onCreated) {
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

        IntegerField neutralMinProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_NEUTRAL_MIN_PRODUCTION));
        neutralMinProduction.setMin(1);
        neutralMinProduction.setValue(GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION);

        IntegerField neutralMaxProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_NEUTRAL_MAX_PRODUCTION));
        neutralMaxProduction.setMin(1);
        neutralMaxProduction.setValue(GameConfig.DEFAULT_NEUTRAL_MAX_PRODUCTION);

        IntegerField startGarrison = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_START_GARRISON));
        startGarrison.setMin(1);
        startGarrison.setValue(GameConfig.DEFAULT_START_GARRISON);

        ComboBox<ColorOption> color = buildColorCombo();

        Checkbox observersAllowed = new Checkbox(I18n.t(UiTexts.LOBBY_FIELD_OBSERVERS_ALLOWED));
        observersAllowed.setValue(GameConfig.DEFAULT_OBSERVERS_ALLOWED);
        Checkbox reentryAllowed = new Checkbox(I18n.t(UiTexts.LOBBY_FIELD_REENTRY_ALLOWED));
        reentryAllowed.setValue(GameConfig.DEFAULT_REENTRY_ALLOWED);

        FormLayout setupGrid = grid(3, "13em", systems, humans, ai, color, startGarrison, observersAllowed, reentryAllowed);
        FormLayout neutralGrid = grid(2, "16em", neutralMinProduction, neutralMaxProduction);

        FormLayout startProductionFields = new FormLayout();
        startProductionFields.setAutoResponsive(true);
        startProductionFields.setColumnWidth("13em");
        startProductionFields.setMaxColumns(3);
        startProductionFields.setWidthFull();
        startProductionFields.addClassName("wizard-start-production");

        List<IntegerField> startProductionInputs = new ArrayList<>();
        Runnable rebuildProductionInputs = () -> rebuildStartProduction(
                startProductionFields, startProductionInputs, humans, ai);
        humans.addValueChangeListener(_ -> rebuildProductionInputs.run());
        ai.addValueChangeListener(_ -> rebuildProductionInputs.run());
        rebuildProductionInputs.run();

        Div intro = new Div();
        intro.addClassName("wizard-intro");
        intro.setText(I18n.t(UiTexts.LOBBY_WIZARD_INTRO));

        VerticalLayout body = new VerticalLayout(
                intro,
                wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_SETUP), I18n.t(UiTexts.LOBBY_WIZARD_SECTION_SETUP_HINT), setupGrid),
                wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_NEUTRAL), I18n.t(UiTexts.LOBBY_WIZARD_SECTION_NEUTRAL_HINT), neutralGrid),
                wizardSection(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_START), I18n.t(UiTexts.LOBBY_WIZARD_SECTION_START_HINT), startProductionFields));
        body.addClassName("wizard-body");
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();
        dialog.add(body);

        FormInputs formInputs = new FormInputs(systems, humans, ai, startProductionInputs,
                neutralMinProduction, neutralMaxProduction, startGarrison,
                color, observersAllowed, reentryAllowed);
        Button create = new Button(I18n.t(UiTexts.LOBBY_WIZARD_CREATE), _ -> {
            GameSetup setup = buildSetup(formInputs);
            String hostUsername = UserContext.currentUsername().orElse(null);
            game.newGame(setup, hostUsername, GameNameGenerator.random());
            onCreated.run();
            dialog.close();
        });
        create.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button(I18n.t(UiTexts.LOBBY_WIZARD_CANCEL), _ -> dialog.close());
        dialog.getFooter().add(cancel, create);
        dialog.open();
    }

    private static void rebuildStartProduction(FormLayout fields, List<IntegerField> inputs,
                                               IntegerField humans, IntegerField ai) {
        fields.removeAll();
        inputs.clear();

        int humanCount = Math.max(1, valueOrDefault(humans.getValue(), GameConfig.DEFAULT_HUMAN_PLAYERS));
        for (int i = 1; i <= humanCount; i++) {
            IntegerField humanProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_START_PRODUCTION_HUMAN, i));
            humanProduction.setMin(1);
            humanProduction.setValue(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION);
            fields.add(humanProduction);
            inputs.add(humanProduction);
        }

        int aiCount = Math.max(0, valueOrDefault(ai.getValue(), GameConfig.DEFAULT_AI_PLAYERS));
        for (int i = 1; i <= aiCount; i++) {
            IntegerField aiProduction = new IntegerField(I18n.t(UiTexts.LOBBY_FIELD_START_PRODUCTION_AI, i));
            aiProduction.setMin(1);
            aiProduction.setValue(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION);
            fields.add(aiProduction);
            inputs.add(aiProduction);
        }
    }

    private record FormInputs(IntegerField systems, IntegerField humans, IntegerField ai,
                              List<IntegerField> startProductionInputs,
                              IntegerField neutralMinProduction, IntegerField neutralMaxProduction,
                              IntegerField startGarrison, ComboBox<ColorOption> color,
                              Checkbox observersAllowed, Checkbox reentryAllowed) {
    }

    private static GameSetup buildSetup(FormInputs in) {
        List<Integer> startProductionPerPlayer = in.startProductionInputs().stream()
                .map(field -> valueOrDefault(field.getValue(), GameConfig.DEFAULT_START_SYSTEM_PRODUCTION))
                .toList();
        return new GameSetup(
                valueOrDefault(in.systems().getValue(), GameConfig.DEFAULT_SYSTEM_COUNT),
                valueOrDefault(in.humans().getValue(), GameConfig.DEFAULT_HUMAN_PLAYERS),
                valueOrDefault(in.ai().getValue(), GameConfig.DEFAULT_AI_PLAYERS),
                startProductionPerPlayer,
                valueOrDefault(in.neutralMinProduction().getValue(), GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION),
                valueOrDefault(in.neutralMaxProduction().getValue(), GameConfig.DEFAULT_NEUTRAL_MAX_PRODUCTION),
                valueOrDefault(in.startGarrison().getValue(), GameConfig.DEFAULT_START_GARRISON),
                in.color().getValue() == null ? GameConfig.PLAYER_PALETTE.getFirst() : in.color().getValue().hex(),
                in.observersAllowed().getValue(),
                in.reentryAllowed().getValue()
        ).normalized();
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

    private static FormLayout grid(int maxColumns, String columnWidth, Component... fields) {
        FormLayout layout = new FormLayout(fields);
        layout.addClassName("wizard-grid");
        layout.setWidthFull();
        layout.setAutoResponsive(true);
        layout.setColumnWidth(columnWidth);
        layout.setMaxColumns(maxColumns);
        return layout;
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

    private static List<ColorOption> colorOptions() {
        return List.of(
                new ColorOption("Kobaltblau", "#0072B2"),
                new ColorOption("Bernstein", "#E69F00"),
                new ColorOption("Smaragd", "#009E73"),
                new ColorOption("Zinnober", "#D55E00"),
                new ColorOption("Magenta", "#CC79A7"),
                new ColorOption("Himmelblau", "#56B4E9"),
                new ColorOption("Gold", "#F0E442"),
                new ColorOption("Karminrot", "#C0392B"),
                new ColorOption("Türkis", "#00A6A6"),
                new ColorOption("Violett", "#7A4EAB")
        );
    }

    private record ColorOption(String label, String hex) {
    }
}
