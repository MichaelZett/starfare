/** Free lane intervals in map coordinates, with circular system protection. */
export function freeSegments(length, obstacles) {
    let segments = [[0, length]];
    for (const {along, across, radius} of obstacles) {
        if (Math.abs(across) >= radius) continue;
        const reach = Math.sqrt(radius * radius - across * across);
        const low = along - reach, high = along + reach;
        segments = segments.flatMap(([start, end]) => {
            if (high <= start || low >= end) return [[start, end]];
            const parts = [];
            if (low > start) parts.push([start, Math.min(low, end)]);
            if (high < end) parts.push([Math.max(high, start), end]);
            return parts;
        });
    }
    return segments.filter(([start, end]) => end - start > 1);
}

export function badgePosition(segments, desired, halfWidth) {
    const choices = segments.filter(([start, end]) => end - start >= halfWidth * 2)
        .map(([start, end]) => Math.max(start + halfWidth, Math.min(end - halfWidth, desired)));
    return choices.sort((a, b) => Math.abs(a - desired) - Math.abs(b - desired))[0];
}

function clippedPath(segments, height) {
    return `path('${segments.map(([a, b]) => `M ${a} 0 H ${b} V ${height} H ${a} Z`).join(' ')}')`;
}

function groupBy(nodes, key) {
    const groups = new Map();
    for (const node of nodes) {
        const value = node.dataset[key];
        if (!groups.has(value)) groups.set(value, []);
        groups.get(value).push(node);
    }
    return groups;
}

function projectObstacles(items, geometry, extra) {
    const {ax, ay, ux, uy} = geometry;
    return items.map(item => ({
        along: (item.x - ax) * ux + (item.y - ay) * uy,
        across: -(item.x - ax) * uy + (item.y - ay) * ux,
        radius: item.radius + extra
    }));
}

function laneGeometry(lane, siblings, zoom) {
    const x = Number(lane.dataset.mapX), y = Number(lane.dataset.mapY);
    const length = Number(lane.dataset.mapLength), angle = Number(lane.dataset.mapAngle);
    const ux = Math.cos(angle), uy = Math.sin(angle);
    const slot = siblings.indexOf(lane) - (siblings.length - 1) / 2;
    // Reverse directions use the same physical perpendicular.
    const direction = lane.dataset.mapReverse === 'true' ? -1 : 1;
    const spacing = Math.min(14, 60 / Math.max(1, siblings.length - 1));
    const offset = slot * spacing / zoom * direction;
    return {ax:x - uy * offset, ay:y + ux * offset, ux, uy, length, angle, spacing};
}

function placeBadge(node, lane, siblings, geometry, context) {
    const {zoom, badgeObstacles, systems} = context;
    const {ax, ay, ux, uy, length} = geometry;
    const half = Math.hypot(node.offsetWidth / 2, node.offsetHeight / 2) + 4 / zoom;
    const preferred = siblings.length > 1 ? (siblings.indexOf(lane) + 1) / (siblings.length + 1)
        : Number(node.dataset.mapProgress || 0.5);
    const segments = freeSegments(length, [
        ...projectObstacles(systems, geometry, 6 / zoom),
        ...projectObstacles(badgeObstacles, geometry, half)
    ]);
    const position = badgePosition(segments, preferred * length, half);
    node.style.visibility = position === undefined ? 'hidden' : 'visible';
    if (position === undefined) return;
    node.style.left = `${ax + ux * position}px`;
    node.style.top = `${ay + uy * position}px`;
    badgeObstacles.push({x:ax + ux * position, y:ay + uy * position, radius:half});
}

function placeLane(node, geometry, segments, zoom) {
    const {ax, ay, length, angle, spacing} = geometry;
    const hit = node.classList.contains('fleet-lane-hit');
    const height = (hit ? Math.min(12, spacing) : Math.min(3, spacing * 0.7)) / zoom;
    node.style.left = `${ax}px`;
    node.style.top = `${ay - height / 2}px`;
    node.style.width = `${length}px`;
    node.style.height = `${height}px`;
    node.style.transform = `rotate(${angle}rad)`;
    node.style.transformOrigin = '0 50%';
    node.style.clipPath = segments.length ? clippedPath(segments, height) : 'inset(100%)';
    node.style.visibility = segments.length ? 'visible' : 'hidden';
}

function layoutRoutes(map, systems, zoom) {
    const groups = groupBy([...map.querySelectorAll('[data-map-route]')], 'mapRoute');
    const lanes = [...map.querySelectorAll('.fleet-lane')];
    const pairs = groupBy(lanes, 'mapPair');
    const context = {zoom, systems, badgeObstacles:[]};
    for (const lane of lanes) {
        const siblings = pairs.get(lane.dataset.mapPair);
        const geometry = laneGeometry(lane, siblings, zoom);
        const segments = freeSegments(geometry.length, projectObstacles(systems, geometry, 6 / zoom));
        for (const node of groups.get(lane.dataset.mapRoute)) {
            if (node.classList.contains('fleet-badge') || node.classList.contains('standing-order-badge')) {
                placeBadge(node, lane, siblings, geometry, context);
            } else {
                placeLane(node, geometry, segments, zoom);
            }
        }
    }
}

function markerPosition(system, systems, occupied, zoom) {
    const reach = system.radius + 32 / zoom;
    let point;
    for (let attempt = 0; attempt < 32; attempt++) {
        const angle = -Math.PI / 4 + (attempt % 8) * Math.PI / 4;
        const radius = reach + Math.floor(attempt / 8) * 48 / zoom;
        point = {x:system.x + Math.cos(angle) * radius, y:system.y + Math.sin(angle) * radius};
        if (systems.every(item => Math.hypot(item.x - point.x, item.y - point.y) > item.radius + 22 / zoom)
            && occupied.every(item => Math.hypot(item.x - point.x, item.y - point.y) > 46 / zoom)) break;
    }
    return point;
}

function layoutMarkers(map, systems, zoom) {
    const occupied = [];
    for (const marker of map.querySelectorAll('.sys-report-marker, .sys-economy-marker')) {
        const dot = marker.closest('.sys-own, .sys-fog');
        const x = dot ? parseFloat(dot.style.left) : Number(marker.dataset.mapX);
        const y = dot ? parseFloat(dot.style.top) : Number(marker.dataset.mapY);
        const system = systems.find(item => item.x === x && item.y === y);
        if (!system) continue;
        const point = markerPosition(system, systems, occupied, zoom);
        occupied.push(point);
        // Report markers are children of transformed system dots.
        marker.style.left = `${dot ? point.x - x + dot.offsetWidth / 2 : point.x}px`;
        marker.style.top = `${dot ? point.y - y + dot.offsetHeight / 2 : point.y}px`;
        marker.style.right = 'auto';
    }
}

function layout(map) {
    const zoom = Number(map.style.zoom || 1);
    const inverse = String(1 / zoom);
    if (map.style.getPropertyValue('--map-inverse-zoom') !== inverse) {
        map.style.setProperty('--map-inverse-zoom', inverse);
    }
    const systems = [...map.querySelectorAll('.sys-own, .sys-fog')].map(dot => {
        const radius = Math.max(dot.offsetWidth / 2 + 8 / zoom, 22 / zoom);
        dot.style.setProperty('--map-system-target', `${radius * 2}px`);
        return {x:parseFloat(dot.style.left), y:parseFloat(dot.style.top), radius};
    });
    layoutRoutes(map, systems, zoom);
    layoutMarkers(map, systems, zoom);
}

window.starfareMapGeometry = map => {
    if (map.__geometryObserver) { layout(map); return; }
    let queued = false;
    const schedule = () => {
        if (queued) return;
        queued = true;
        requestAnimationFrame(() => { queued = false; if (map.isConnected) layout(map); });
    };
    map.__geometryObserver = new MutationObserver(schedule);
    map.__geometryObserver.observe(map, {childList: true, attributes: true, attributeFilter: ['style']});
    schedule();
};
