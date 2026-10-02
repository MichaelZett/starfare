import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import test from 'node:test';

globalThis.window = {};
globalThis.document = {addEventListener() {}};
const source = await readFile(new URL('../../main/frontend/battle-replay.js', import.meta.url), 'utf8');
const {lossSequence} = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);

test('uneven losses preserve both endpoints and never reveal elimination early', () => {
    let index = 0;
    const values = [0, 0.9, 0.1, 0.6, 0.2, 1];
    const sequence = lossSequence(300, 0, 6, () => values[index++]);
    assert.equal(sequence.at(-1), 0);
    assert.ok(sequence.slice(0, -1).every(value => value > 0 && value <= 300));
    assert.ok(sequence.every((value, at) => at === 0 || value <= sequence[at - 1]));
    const losses = sequence.map((value, at) => (at === 0 ? 300 : sequence[at - 1]) - value);
    assert.ok(new Set(losses).size > 2);
});

test('survivors, no losses and empty armies retain the exact combat result', () => {
    for (const [initial, remaining] of [[90, 60], [1, 0], [12, 12], [0, 0]]) {
        const sequence = lossSequence(initial, remaining, 8, () => 0.5);
        assert.equal(sequence.at(-1), remaining);
        assert.ok(sequence.every(value => value >= remaining && value <= initial));
    }
});

for (const attackerWon of [true, false]) {
    test(`zero-survivor result uses the explicit winner flag (${attackerWon})`, () => {
        const classes = new Set();
        const classList = {
            add(value) { classes.add(value); },
            toggle(value, enabled) { if (enabled) classes.add(value); else classes.delete(value); }
        };
        const attackNumber = {textContent: ''};
        const defenseNumber = {textContent: ''};
        const root = {
            classList,
            getAttribute() { return String(attackerWon); },
            querySelector(selector) {
                if (selector === '[data-battle-attacking]') return attackNumber;
                if (selector === '[data-battle-defending]') return defenseNumber;
                return {classList};
            },
            querySelectorAll() { return []; }
        };
        window.matchMedia = () => ({matches: true});

        window.starfarePlayBattle(root, 1, 1, 0, 0, false);

        assert.equal(classes.has('battle-replay-attacker-won'), attackerWon);
        assert.ok(classes.has('battle-replay-result-known'));
        assert.equal(attackNumber.textContent, '0');
        assert.equal(defenseNumber.textContent, '0');
    });
}

test('coalition playback reveals the stored survivors and member losses together', () => {
    const sides = [
        {id: 1, members: [{playerId: 1, ships: 50, remaining: 20}, {playerId: 2, ships: 50, remaining: 20}]},
        {id: 3, members: [{playerId: 3, ships: 60, remaining: 0}]},
        {id: 0, members: [{playerId: 0, ships: 0, remaining: 0}]}
    ];
    const before = JSON.stringify(sides);
    const panels = sides.map(side => {
        const classes = new Set();
        const number = {textContent: ''};
        const members = side.members.map(member => ({textContent: 'initial', getAttribute: () => `${member.remaining} remaining`}));
        return {classes, number, members, classList: {add: value => classes.add(value)},
            querySelector: () => number, querySelectorAll: () => members};
    });
    const result = new Set();
    const root = {querySelector(selector) {
        if (selector === '[data-battle-result]') return {classList: {add: value => result.add(value)}};
        return panels[sides.findIndex(side => selector === `[data-coalition-side="${side.id}"]`)];
    }};
    window.matchMedia = () => ({matches: true});
    window.starfarePlayCoalition(root, sides, false);
    assert.deepEqual(panels.map(panel => panel.number.textContent), ['40', '0', '0']);
    assert.ok(panels[0].classes.has('coalition-survived'));
    assert.ok(panels[1].classes.has('coalition-defeated'));
    assert.equal(panels[0].members[0].textContent, '20 remaining');
    assert.ok(result.has('battle-replay-result-visible'));
    assert.equal(window.starfareBattleAudio.enabled, false);
    assert.equal(JSON.stringify(sides), before);
});
