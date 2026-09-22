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
