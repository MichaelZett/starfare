import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import test from 'node:test';
globalThis.window = {};
const source = await readFile(new URL('../../main/frontend/map-geometry.js', import.meta.url), 'utf8');
const {freeSegments, badgePosition} = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);

test('source, target and a third crossing system are protected', () => {
    assert.deepEqual(freeSegments(400, [
        {along:0, across:0, radius:50}, {along:200, across:0, radius:60}, {along:400, across:0, radius:50}
    ]), [[50,140], [260,350]]);
});
test('zoom-dependent protection can hide short routes, and badges never occupy a protected gap', () => {
    assert.deepEqual(freeSegments(80, [{along:0, across:0, radius:220}, {along:80, across:0, radius:220}]), []);
    assert.equal(badgePosition([[50,140],[260,350]], 200, 20), 120);
    assert.equal(badgePosition([[10,20]], 0, 20), undefined);
});
test('off-axis circles, overlapping protection and route ends are clipped without negative intervals', () => {
    const obstacles = [{along:50,across:30,radius:50}, {along:60,across:0,radius:30}, {along:200,across:100,radius:50}];
    assert.deepEqual(freeSegments(120, obstacles), [[0,10],[90,120]]);
});
