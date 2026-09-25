// Audio is unlocked by a browser gesture, before the server opens the dialog.
const audio = {
    enabled: true,
    context: null,
    async unlock() {
        const Audio = window.AudioContext || window.webkitAudioContext;
        if (!Audio) return;
        this.context ||= new Audio();
        if (this.context.state !== 'running') await this.context.resume();
    },
    impact() {
        if (!this.enabled || this.context?.state !== 'running') return;
        const context = this.context;
        const time = context.currentTime;
        const oscillator = context.createOscillator();
        const gain = context.createGain();
        oscillator.type = 'sawtooth';
        oscillator.frequency.setValueAtTime(180 + Math.random() * 220, time);
        oscillator.frequency.exponentialRampToValueAtTime(45, time + 0.22);
        gain.gain.setValueAtTime(0, time);
        gain.gain.linearRampToValueAtTime(0.16, time + 0.01);
        gain.gain.exponentialRampToValueAtTime(0.001, time + 0.24);
        oscillator.connect(gain).connect(context.destination);
        oscillator.onended = () => { oscillator.disconnect(); gain.disconnect(); };
        oscillator.start(time);
        oscillator.stop(time + 0.25);
    },
    async test() {
        await this.unlock();
        const enabled = this.enabled;
        this.enabled = true;
        this.impact();
        this.enabled = enabled;
    }
};
window.starfareBattleAudio = audio;
document.addEventListener('click', event => {
    if (event.composedPath().some(node => node.classList?.contains('event-battle-interactive'))) {
        audio.unlock().catch(() => { /* The sound toggle allows another user-initiated attempt. */ });
    }
}, true);

// Independent loss bursts, with a final beat that preserves the actual combat result.
export function lossSequence(initial, remaining, beats, random = Math.random) {
    const weights = Array.from({length: beats}, () => 0.1 + random() ** 2 * 3);
    const total = weights.reduce((sum, weight) => sum + weight, 0);
    let consumed = 0;
    return weights.map((weight, index) => {
        consumed += weight;
        if (index === beats - 1) return remaining;
        return Math.max(remaining === 0 && initial > 0 ? 1 : remaining,
            initial - Math.floor((initial - remaining) * consumed / total));
    });
}

window.starfarePlayBattle = (root, attacking, defending, attackingRemaining, defendingRemaining, sound) => {
    audio.enabled = sound;
    const attackNumber = root.querySelector('[data-battle-attacking]');
    const defenseNumber = root.querySelector('[data-battle-defending]');
    const result = root.querySelector('[data-battle-result]');
    const attackShips = [...root.querySelectorAll('[data-battle-attacker-ship]')];
    const defenseShips = [...root.querySelectorAll('[data-battle-defender-ship]')];
    const duration = Math.min(6200, Math.max(2200, Math.max(attacking, defending) * 24));
    const beats = Math.max(6, Math.round(duration / 360));
    const attacks = lossSequence(attacking, attackingRemaining, beats);
    const defenses = lossSequence(defending, defendingRemaining, beats);
    const hideShips = (ships, current, initial) => ships.forEach((ship, index) => {
        const visible = initial > 0 ? Math.ceil(ships.length * current / initial) : 0;
        ship.classList.toggle('battle-ship-lost', index >= visible);
    });
    const show = (attack, defense) => {
        attackNumber.textContent = String(attack);
        defenseNumber.textContent = String(defense);
        hideShips(attackShips, attack, attacking);
        hideShips(defenseShips, defense, defending);
    };
    const finish = () => {
        show(attackingRemaining, defendingRemaining);
        result.classList.add('battle-replay-result-visible');
        root.classList.add('battle-replay-result-known');
        root.classList.toggle('battle-replay-attacker-won', attackingRemaining > defendingRemaining);
    };
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        finish();
        audio.impact();
        return;
    }
    // A normalized random timeline gives pauses and bursts without changing the total duration.
    const intervals = Array.from({length: beats}, () => 0.4 + Math.random());
    const total = intervals.reduce((sum, interval) => sum + interval, 0);
    let elapsed = 0;
    const moments = intervals.map(interval => {
        elapsed += interval;
        return elapsed / total * duration;
    });
    const started = performance.now();
    let beat = 0;
    const animate = now => {
        if (!root.isConnected || !root.closest('vaadin-dialog')?.opened) return;
        if (now - started >= moments[beat]) {
            show(attacks[beat], defenses[beat]);
            audio.impact();
            beat++;
        }
        if (beat < beats) requestAnimationFrame(animate);
        else finish();
    };
    requestAnimationFrame(animate);
};
