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

    private static final String PLAY_JS = """
            const root = this;
            const attacking = $0, defending = $1, attackingRemaining = $2, defendingRemaining = $3;
            const sound = $4;
            const attackNumber = root.querySelector('[data-battle-attacking]');
            const defenseNumber = root.querySelector('[data-battle-defending]');
            const result = root.querySelector('[data-battle-result]');
            const attackShips = Array.from(root.querySelectorAll('[data-battle-attacker-ship]'));
            const defenseShips = Array.from(root.querySelectorAll('[data-battle-defender-ship]'));
            const duration = Math.min(6200, Math.max(2200, Math.max(attacking, defending) * 24));
            const hideShips = (ships, current, initial) => ships.forEach((ship, index) => {
                ship.classList.toggle('battle-ship-lost', index >= Math.ceil(ships.length * current / initial));
            });
            const playImpact = () => {
                if (!sound || !window.AudioContext && !window.webkitAudioContext) return;
                const C = window.AudioContext || window.webkitAudioContext;
                window.__starfareBattleAudio = window.__starfareBattleAudio || new C();
                const context = window.__starfareBattleAudio;
                if (context.state === 'suspended') context.resume();
                const oscillator = context.createOscillator(), gain = context.createGain();
                oscillator.type = 'triangle';
                oscillator.frequency.setValueAtTime(180, context.currentTime);
                oscillator.frequency.exponentialRampToValueAtTime(70, context.currentTime + .13);
                gain.gain.setValueAtTime(.035, context.currentTime);
                gain.gain.exponentialRampToValueAtTime(.001, context.currentTime + .15);
                oscillator.connect(gain).connect(context.destination);
                oscillator.start(); oscillator.stop(context.currentTime + .16);
            };
            const started = performance.now(); let lastImpact = started;
            const animate = now => {
                const progress = Math.min(1, (now - started) / duration);
                const currentAttack = Math.round(attacking + (attackingRemaining - attacking) * progress);
                const currentDefense = Math.round(defending + (defendingRemaining - defending) * progress);
                attackNumber.textContent = String(currentAttack);
                defenseNumber.textContent = String(currentDefense);
                hideShips(attackShips, currentAttack, attacking);
                hideShips(defenseShips, currentDefense, defending);
                if (progress < 1) {
                    if (now - lastImpact > 420) { playImpact(); lastImpact = now; }
                    requestAnimationFrame(animate);
                } else {
                    result.classList.add('battle-replay-result-visible');
                }
            };
            if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                attackNumber.textContent = String(attackingRemaining);
                defenseNumber.textContent = String(defendingRemaining);
                hideShips(attackShips, attackingRemaining, attacking);
                hideShips(defenseShips, defendingRemaining, defending);
                result.classList.add('battle-replay-result-visible');
            } else {
                requestAnimationFrame(animate);
            }
            """;

    private BattleReplayDialog() {
    }

    static void open(BattleReplay replay) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.BATTLE_REPLAY_TITLE, replay.systemName()));
        dialog.addClassName("battle-replay-dialog");
        dialog.setWidth("min(900px, 94vw)");

        Div visual = new Div();
        visual.addClassName("battle-replay");
        visual.add(side("battle-attacker", UiTexts.BATTLE_REPLAY_ATTACKERS, replay.attacking(), replay.defending()),
                new Span("✦"),
                side("battle-defender", UiTexts.BATTLE_REPLAY_DEFENDERS, replay.defending(), replay.attacking()));
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
            sound.getElement().executeJs("localStorage.setItem('starfare.battleSound', $0)", event.getValue());
        });
        Button close = new Button(I18n.t(UiTexts.BATTLE_REPLAY_CLOSE), _ -> dialog.close());
        dialog.getFooter().add(sound, close);
        dialog.add(visual);
        dialog.open();
        visual.getElement().executeJs(PLAY_JS, replay.attacking(), replay.defending(), replay.attackingRemaining(),
                replay.defendingRemaining(), soundEnabled);
    }

    /** Prepares audio in the browser's actual click handler, before the Vaadin round-trip. */
    static void primeAudio(Div trigger) {
        trigger.getElement().executeJs("""
                this.addEventListener('click', () => {
                    if (localStorage.getItem('starfare.battleSound') === 'false') return;
                    const C = window.AudioContext || window.webkitAudioContext;
                    if (!C) return;
                    window.__starfareBattleAudio = window.__starfareBattleAudio || new C();
                    const context = window.__starfareBattleAudio;
                    if (context.state === 'suspended') context.resume();
                });
                """);
    }

    private static Div side(String sideClass, String labelKey, int ships, int opponentShips) {
        Div side = new Div();
        side.addClassNames("battle-side", sideClass);
        side.add(new Span(I18n.t(labelKey)));
        Span number = new Span(String.valueOf(ships));
        number.addClassName("battle-number");
        number.getElement().setAttribute(sideClass.endsWith("attacker") ? "data-battle-attacking" : "data-battle-defending", "");
        side.add(number);
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
}
