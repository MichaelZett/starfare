package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.game.values.ComputerStrength;
import de.zettsystems.starfare.game.values.EmpireNameGenerator;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GalaxySize;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameNameGenerator;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameTempo;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.i18n.I18n;

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * New-game form after Frontier Lords: a suggested name and one section per decision, with picture cards for
 * rules, map and randomness. Everything else of a game is fixed.
 */
final class NewGamePanel extends Div {
    static final int DEFAULT_PLAYERS = 3;

    private final TextField name = new TextField(I18n.t(UiTexts.LOBBY_FIELD_GAME_NAME));
    private final TextField empire = new TextField(I18n.t(UiTexts.LOBBY_FIELD_EMPIRE_NAME));
    private final RadioButtonGroup<RulesetDefinition> rules;
    private final ComboBox<Integer> players = new ComboBox<>(I18n.t(UiTexts.SETUP_FIELD_PLAYERS));
    private final ComboBox<Integer> computers = new ComboBox<>(I18n.t(UiTexts.SETUP_FIELD_COMPUTERS));
    private final ComboBox<ComputerStrength> strength = new ComboBox<>(I18n.t(UiTexts.SETUP_FIELD_STRENGTH));
    private final ComboBox<GameTempo> tempo = new ComboBox<>(I18n.t(UiTexts.LOBBY_FIELD_TEMPO));
    private final Checkbox inviteOnly = new Checkbox(I18n.t(UiTexts.SETUP_FIELD_INVITE_ONLY));
    private final Checkbox tutorial = new Checkbox(I18n.t(UiTexts.LOBBY_FIELD_TUTORIAL));
    private final RadioButtonGroup<GalaxyLayout> layout = SetupCards.layout();
    private final RadioButtonGroup<GalaxySize> size = SetupCards.size(this::playerCount);
    private final ComboBox<Integer> maxProduction = new ComboBox<>(I18n.t(UiTexts.SETUP_FIELD_MAX_PRODUCTION));
    private final RadioButtonGroup<RandomnessLevel> randomness = SetupCards.randomness();
    private final SetupDetails details = new SetupDetails();

    NewGamePanel(RulesetCatalog catalog, RulesetOffer offer, String hostName) {
        addClassName("setup-form");
        rules = SetupCards.rules(catalog, offer);
        configureNames(hostName);
        configurePlayers();
        maxProduction.setId("max-production");
        maxProduction.setItems(GameConfig.MAX_PRODUCTION_CHOICES);
        maxProduction.setValue(GameConfig.DEFAULT_MAX_PRODUCTION);
        maxProduction.setHelperText(I18n.t(UiTexts.SETUP_FIELD_MAX_PRODUCTION_HINT));
        rules.addValueChangeListener(_ -> refreshDetails());
        randomness.addValueChangeListener(_ -> refreshDetails());
        refreshDetails();
        add(nameRow(),
                SetupLayout.section(SetupLayout.Emblem.GAME, I18n.t(UiTexts.SETUP_SECTION_GAME),
                        I18n.t(UiTexts.SETUP_SECTION_GAME_HINT), rules, SetupLayout.grid(empire)),
                SetupLayout.section(SetupLayout.Emblem.PLAYERS, I18n.t(UiTexts.SETUP_SECTION_PLAYERS),
                        I18n.t(UiTexts.SETUP_SECTION_PLAYERS_HINT),
                        SetupLayout.grid(players, computers, strength, tempo, inviteOnly, tutorial)),
                SetupLayout.section(SetupLayout.Emblem.MAP, I18n.t(UiTexts.SETUP_SECTION_MAP),
                        I18n.t(UiTexts.SETUP_SECTION_MAP_HINT), layout, size, SetupLayout.grid(maxProduction)),
                SetupLayout.section(SetupLayout.Emblem.CHANCE, I18n.t(UiTexts.SETUP_SECTION_CHANCE),
                        I18n.t(UiTexts.SETUP_SECTION_CHANCE_HINT), randomness),
                details);
    }

    private Div nameRow() {
        Button roll = new Button(VaadinIcon.REFRESH.create(), _ -> name.setValue(GameNameGenerator.random()));
        roll.addThemeVariants(ButtonVariant.TERTIARY);
        roll.setId("roll-game-name");
        roll.setTooltipText(I18n.t(UiTexts.SETUP_ROLL_NAME));
        roll.setAriaLabel(I18n.t(UiTexts.SETUP_ROLL_NAME));
        Div row = new Div(name, roll);
        row.addClassName("setup-name");
        return row;
    }

    private void configureNames(String hostName) {
        name.setId("wizard-game-name");
        name.setMaxLength(80);
        name.setValue(GameNameGenerator.random());
        empire.setId("wizard-empire-name");
        empire.setValue(EmpireNameGenerator.forHuman(hostName));
        empire.setRequiredIndicatorVisible(true);
    }

    private void configurePlayers() {
        players.setId("wizard-players");
        players.setItems(IntStream.rangeClosed(GameConfig.MIN_TOTAL_PLAYERS, GameConfig.MAX_TOTAL_PLAYERS).boxed().toList());
        players.setValue(DEFAULT_PLAYERS);
        computers.setId("wizard-ai");
        computers.setItems(IntStream.range(0, DEFAULT_PLAYERS).boxed().toList());
        computers.setValue(DEFAULT_PLAYERS - 1);
        strength.setId("wizard-strength");
        strength.setItems(ComputerStrength.values());
        strength.setItemLabelGenerator(NewGamePanel::strengthName);
        strength.setValue(ComputerStrength.DEFAULT);
        strength.setHelperText(I18n.t(UiTexts.SETUP_FIELD_STRENGTH_HINT));
        tempo.setId("wizard-tempo");
        tempo.setItems(GameTempo.values());
        tempo.setItemLabelGenerator(TempoLabels::label);
        tempo.setValue(GameTempo.DEFAULT);
        tempo.setHelperText(I18n.t(UiTexts.LOBBY_WIZARD_SECTION_ROUNDS_HINT, GameConfig.MAX_MISSED_ROUNDS));
        inviteOnly.setId("wizard-invite-only");
        inviteOnly.setHelperText(I18n.t(UiTexts.SETUP_FIELD_INVITE_ONLY_HINT));
        tutorial.setId("wizard-tutorial");
        tutorial.setHelperText(I18n.t(UiTexts.LOBBY_FIELD_TUTORIAL_HINT));
        players.addValueChangeListener(event -> {
            int count = playerCount();
            int previousCount = event.getOldValue() == null ? DEFAULT_PLAYERS : event.getOldValue();
            int previous = computerCount();
            computers.setItems(IntStream.range(0, count).boxed().toList());
            // Whoever played alone against the computer keeps doing so; a mixed table keeps its computers.
            computers.setValue(previous == previousCount - 1 ? count - 1 : Math.min(previous, count - 1));
            size.setItemEnabledProvider(choice -> choice.fits(count));
            size.setValue(GalaxySize.fitting(Objects.requireNonNullElse(size.getValue(), GalaxySize.DEFAULT), count));
        });
        computers.addValueChangeListener(_ -> refreshSeats());
        refreshSeats();
    }

    private void refreshSeats() {
        strength.setEnabled(computerCount() > 0);
        // Only games with several people run against deadlines and look for players, so only they get a tempo
        // and the choice to be found in the lobby; a game against the computer alone stays private.
        tempo.setVisible(humanCount() > 1);
        inviteOnly.setVisible(humanCount() > 1);
    }

    private void refreshDetails() {
        boolean classic = chosenRuleset().usesSectorOptions();
        details.setVisible(classic);
        details.showEvents(classic && chosenRandomness().hasEvents());
    }

    int playerCount() {
        return Objects.requireNonNullElse(players.getValue(), DEFAULT_PLAYERS);
    }

    private int computerCount() {
        return Math.clamp(Objects.requireNonNullElse(computers.getValue(), 0), 0, playerCount() - 1);
    }

    private int humanCount() {
        return playerCount() - computerCount();
    }

    private RandomnessLevel chosenRandomness() {
        return Objects.requireNonNullElse(randomness.getValue(), RandomnessLevel.DEFAULT);
    }

    private RulesetRef chosenRuleset() {
        RulesetDefinition entry = rules.getValue();
        if (entry == null) {
            throw new IllegalStateException("Select a released game variant");
        }
        return entry.defaultRef();
    }

    /** Every choice of the form as one setup; the person creating the game always takes the first seat. */
    GameSetup setup() {
        GameTempo chosenTempo = Objects.requireNonNullElse(tempo.getValue(), GameTempo.DEFAULT);
        RandomnessLevel level = chosenRandomness();
        RoundRules deadlines = humanCount() > 1 ? RoundRules.forTempo(chosenTempo, level.attackOrder())
                : new RoundRules(null, null, level.attackOrder());
        GalaxySize chosenSize = Objects.requireNonNullElse(size.getValue(), GalaxySize.DEFAULT);
        return GameSetup.defaults()
                .seatPlayers(humanCount(), computerCount())
                .chooseGalaxy(chosenSize.systems(), Objects.requireNonNullElse(layout.getValue(), GalaxyLayout.EVEN))
                .chooseMaxProduction(Objects.requireNonNullElse(maxProduction.getValue(), GameConfig.DEFAULT_MAX_PRODUCTION))
                .chooseRoundRules(deadlines)
                .selectRuleset(chosenRuleset())
                .chooseComputerStrength(Objects.requireNonNullElse(strength.getValue(), ComputerStrength.DEFAULT))
                .chooseRandomness(level)
                .chooseEvents(details.chosenEvents())
                .limitRounds(details.chosenRoundLimit())
                .chooseTutorial(tutorial.getValue())
                .normalized();
    }

    TextField gameName() {
        return name;
    }

    TextField empireName() {
        return empire;
    }

    /** Whether others find the game in the lobby: never for a game against the computer alone. */
    boolean openToAll() {
        return humanCount() > 1 && !inviteOnly.getValue();
    }

    static String strengthName(ComputerStrength value) {
        return I18n.t(switch (value) {
            case EASY -> UiTexts.SETUP_STRENGTH_EASY;
            case NORMAL -> UiTexts.SETUP_STRENGTH_NORMAL;
            case HARD -> UiTexts.SETUP_STRENGTH_HARD;
        });
    }
}
