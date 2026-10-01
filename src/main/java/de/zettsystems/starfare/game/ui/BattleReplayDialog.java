package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.BattleReplay;
import de.zettsystems.starfare.style.HtmlAttributes;

/** Client-side combat playback with an optional synthesized sound effect. */
final class BattleReplayDialog {
    private static final int MAX_SHIP_MARKERS = 8;

    private BattleReplayDialog() {
    }

    static void open(BattleReplay replay) {
        open(replay, new BattleSides("", ""), () -> { });
    }

    static void open(BattleReplay replay, Runnable onAcknowledge) {
        open(replay, new BattleSides("", ""), onAcknowledge);
    }

    static void open(BattleReplay replay, BattleSides sides, Runnable onAcknowledge) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.BATTLE_REPLAY_TITLE, replay.systemName()));
        dialog.addClassName("battle-replay-dialog");
        dialog.setWidth("min(900px, 94vw)");

        Div visual = new Div();
        visual.addClassName("battle-replay");
        visual.getElement().setAttribute(HtmlAttributes.BATTLE_ATTACKER_WON, String.valueOf(replay.attackerWon()));
        visual.add(side("battle-attacker", UiTexts.BATTLE_REPLAY_ATTACKERS, replay.attacking(), replay.defending(),
                        replay.attackerStrength(), replay.defenderStrength(), sides.attacker()),
                new Span("✦"),
                side("battle-defender", UiTexts.BATTLE_REPLAY_DEFENDERS, replay.defending(), replay.attacking(),
                        replay.defenderStrength(), replay.attackerStrength(), sides.defender()));
        visual.getChildren().skip(1).findFirst().ifPresent(center -> center.addClassName("battle-replay-flash"));

        Div result = new Div();
        result.addClassName("battle-replay-result");
        result.getElement().setAttribute("data-battle-result", "");
        result.setText(I18n.t(UiTexts.BATTLE_REPLAY_RESULT, replay.attackingRemaining(), replay.defendingRemaining()));
        visual.add(result);

        boolean soundEnabled = soundEnabled();
        Checkbox sound = new Checkbox(I18n.t(UiTexts.BATTLE_REPLAY_SOUND), soundEnabled);
        sound.addValueChangeListener(event -> DisplayPreferences.choose(
                de.zettsystems.starfare.social.values.DisplaySetting.SOUND, String.valueOf(event.getValue())));
        sound.getElement().executeJs("""
                this.addEventListener('change', () => {
                    const audio = window.starfareBattleAudio;
                    audio.enabled = this.checked;
                    if (this.checked) audio.unlock().then(() => audio.impact()).catch(() => {});
                });
                """);
        Button soundTest = new Button(I18n.t(UiTexts.BATTLE_REPLAY_SOUND_TEST));
        soundTest.getElement().executeJs("""
                this.addEventListener('click', () => {
                    window.starfareBattleAudio?.test().catch(() => {});
                });
                """);
        Button acknowledge = new Button(I18n.t(UiTexts.BATTLE_REPLAY_ACKNOWLEDGE), _ -> {
            dialog.close();
            onAcknowledge.run();
        });
        ComboBox<Double> speed = new ComboBox<>(I18n.t(UiTexts.BATTLE_REPLAY_SPEED));
        speed.setItems(0.5, 1.0, 1.5, 2.0);
        speed.setItemLabelGenerator(value -> value + "×");
        speed.setValue(speed());
        speed.addValueChangeListener(event -> { if (event.getValue() != null) { setSpeed(event.getValue()); } });
        dialog.getFooter().add(sound, soundTest, speed, acknowledge);
        dialog.add(visual);
        dialog.open();
        visual.getElement().executeJs("window.starfarePlayBattle(this, $0, $1, $2, $3, $4, $5)",
                replay.attacking(), replay.defending(), replay.attackingRemaining(),
                replay.defendingRemaining(), soundEnabled, speed());
    }

    private static Div side(String sideClass, String labelKey, int ships, int opponentShips, double strength, double opposingStrength,
                            String identity) {
        Div side = new Div();
        side.addClassNames("battle-side", sideClass);
        side.add(new Span(I18n.t(labelKey)));
        if (!identity.isBlank()) {
            Span identityChip = new Span(identity);
            identityChip.addClassName("battle-identity");
            side.add(identityChip);
        }
        Span number = new Span(String.valueOf(ships));
        number.addClassName("battle-number");
        number.getElement().setAttribute(sideClass.endsWith("attacker") ? "data-battle-attacking" : "data-battle-defending", "");
        side.add(number);
        Span rolledStrength = new Span(I18n.t(UiTexts.BATTLE_REPLAY_STRENGTH,
                CombatStrengthFormat.format(strength, opposingStrength,
                        com.vaadin.flow.component.UI.getCurrentOrThrow().getLocale())));
        rolledStrength.addClassName("battle-strength");
        side.add(rolledStrength);
        Div markers = new Div();
        markers.addClassName("battle-ships");
        int scale = Math.max(1, (int) Math.ceil(Math.max(ships, opponentShips) / (double) MAX_SHIP_MARKERS));
        int markersCount = Math.max(1, (int) Math.ceil(ships / (double) scale));
        String markerAttribute = sideClass.endsWith("attacker") ? "data-battle-attacker-ship" : "data-battle-defender-ship";
        for (int i = 0; i < markersCount; i++) {
            Span marker = new Span("◆");
            marker.addClassName("battle-ship");
            marker.getElement().setAttribute(markerAttribute, "");
            markers.add(marker);
        }
        side.add(markers);
        return side;
    }

    private static boolean soundEnabled() {
        return Boolean.parseBoolean(DisplayPreferences.get(de.zettsystems.starfare.social.values.DisplaySetting.SOUND, "true"));
    }

    private static double speed() {
        double value = DisplayPreferences.number(de.zettsystems.starfare.social.values.DisplaySetting.SPEED, 1.0);
        return value > 0 ? value : 1.0;
    }

    static void setSpeed(double speed) {
        DisplayPreferences.choose(de.zettsystems.starfare.social.values.DisplaySetting.SPEED, String.valueOf(speed));
    }

    record BattleSides(String attacker, String defender) {
    }
}
