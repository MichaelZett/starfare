# System illustrations

The right-hand system inspector adds a schematic view of stars, planets, moons
and debris. It changes no Classic rules. Shipyards illustrate existing ship
production; they are not buildings that can be constructed. Industry remains
outside this implementation.

## Scientific basis

The illustration uses main-sequence M, K, G, F and A stars. Their conventional
colours and approximate surface-temperature ranges follow the
[OpenStax spectral classification table](https://openstax.org/books/astronomy-2e/pages/17-3-the-spectra-of-stars-and-brown-dwarfs).
An A-type white star is distinct from a white dwarf. Small red dwarfs are cool,
faint and long-lived; the colours are illustrative, not measured photographs.
See [NASA's star types](https://science.nasa.gov/universe/stars/types/).

Companions are common, but there is no universal binary fraction. The review
[Stellar Multiplicity, Duchêne and Kraus (2013)](https://arxiv.org/html/1303.3028)
reports about 26% multiple systems for low-mass main-sequence stars and about
44% for solar-type stars, with a higher fraction at higher masses. The generator
uses 26%, 44% and 50% as approximate companion chances by primary type, simplifies
all multiples to two stars, and depicts a close pair with shared outer orbits.
It does not model the observed distribution of binary separations or stability.

The primary-star mix is rounded to 73% M, 13% K, 6% G, 5% F and 3% A.
The first three proportions are inspired by
[NASA's stellar-population overview](https://science.nasa.gov/exoplanets/stars/);
the remaining split is an artistic choice. This is not an unbiased astronomical
catalogue: hotter stars, stellar remnants and higher-order multiples are omitted.

Rocky planets, gas giants, Neptune-like ice giants and super-Earths follow
[NASA's planet-type overview](https://science.nasa.gov/exoplanets/planet-types/).
A super-Earth is a size/mass category, not a promise of an Earth-like surface.
Ice giants are not frozen rocky balls. Inner planets are usually rocky, with
occasional close-in gas giants; orbit order is not a universal Solar-System copy.
Moons are solid companions of planets or dwarf planets; see
[NASA's moon overview](https://science.nasa.gov/solar-system/moons/).

## Deliberate simplifications

Each illustration has two to five planets, at most one asteroid belt and one
dwarf planet. At most three major moons per body are drawn; this is a readable
selection, not a count of all satellites. Orbit spacing, body sizes, colours
and orbital phase are schematic. Asteroid belts are mostly empty space in
reality; dots are enlarged for visibility. No habitability, orbital periods,
distances, resources or economic capacity are inferred from the decoration.

## Stability and visibility

`SystemComposition` generates the same illustration from game ID, system ID
and a frozen presentation version. It reads neither production nor garrison,
and changes no game state or snapshot. Existing archives need no migration.
Incomplete fog-of-war views receive no composition. Pending battle details
remain masked before the illustration is attached. The cached panel preserves
body selection and the expanded catalogue across normal refreshes.

Future V2 systems can replace decorative descriptions with persisted physical
data and add industry graphics; that requires a separate rules implementation.


## Fictional individual radius model (v1)

Individual radii now come from a separate frozen hash of game, system and body
identities. It does not consume the illustration generator's random sequence,
so existing types, rings, orbit order and moon counts remain unchanged. Star,
planet and moon identities are distinct. This is an artistic model, not a
measurement or an astrophysical inference from a colony's suitability.

| Category | Radius range in km |
| --- | --- |
| Red / orange / yellow stars | 100,000–400,000 / 400,000–600,000 / 600,000–850,000 |
| Yellow-white / white stars | 850,000–1,200,000 / 1,200,000–1,800,000 |
| Rocky / super-Earth | 2,500–8,000 / 8,000–15,000 |
| Ice giant / gas giant | 18,000–35,000 / 35,000–80,000 |
| Dwarf planet / major moon | 200–2,000 / 300–3,500 |

Values are rounded to 100 km and explicitly labelled fictional model values.
Asteroid belts have no single-body radius. Planets and moons use the same fixed
logarithmic compression across all systems; stars have their own separate scale.
Pixel rounding can make nearby radii appear equal. No per-system normalisation
is used. Colony moons retain their own size and environment rather than borrowing
those of the parent gas giant. No measured radii, mass, orbital physics or new
economic effects are implied. The version-1 mapping is frozen for old games.
