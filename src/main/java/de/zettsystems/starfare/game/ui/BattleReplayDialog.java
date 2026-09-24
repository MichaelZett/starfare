package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.BattleReplay;

/** Client-side combat playback with an optional synthesized sound effect. */
final class BattleReplayDialog {
    private static final String SOUND_SESSION_KEY = "starfare.battleSound";
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
        sound.addValueChangeListener(event -> {
            VaadinSession.getCurrent().setAttribute(SOUND_SESSION_KEY, event.getValue());
        });
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
        dialog.getFooter().add(sound, soundTest, acknowledge);
        dialog.add(visual);
        dialog.open();
        visual.getElement().executeJs("window.starfarePlayBattle(this, $0, $1, $2, $3, $4)",
                replay.attacking(), replay.defending(), replay.attackingRemaining(),
                replay.defendingRemaining(), soundEnabled);
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
        Object setting = VaadinSession.getCurrent().getAttribute(SOUND_SESSION_KEY);
        return !(setting instanceof Boolean enabled) || enabled;
    }

    record BattleSides(String attacker, String defender) {
    }
}
