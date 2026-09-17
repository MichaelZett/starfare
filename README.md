# Starfare

A turn-based 4X browser game: conquer star systems, dispatch fleets, win
by dominating the galaxy (in memory of a game I believe called "Sector Forces" on the C-64)

Built as a Spring Boot / Vaadin web application. Multiple players —
humans and AI — share a game; the turn is only resolved once every
human has submitted.

This repository is a personal showcase of a modern Java backend stack
(Java 25, Spring Boot 4, Vaadin 25, Spring Modulith, Flyway,
Testcontainers) wrapped around a small but complete game.

## Lobby and archive

New games are private. Use **Manage** to invite players or publish the game
before it starts. Public games can be found by other signed-in players; private
games are visible to the host, seat holders and invitees. Visibility is fixed
once the game starts.

Finished games move from the lobby to **Archive**. Search by game or player name,
filter your games and open a completed map without issuing further orders.
When the final round contains a battle, the normal round report remains open first.
Play every battle from that report; only then does the victory or defeat dialog appear,
with personal totals for built, destroyed and lost ships.
In the completed map, select any participant (including AI) and enable **Fog of war**
to see their final perspective. Disable fog to reveal all systems and fleets.
For newly completed games, the timeline can rewind to every resolved round;
**Final state** returns to the perspective and fog controls.
Private archives remain restricted to the host and seat holders. For public
archives, outsiders can open the map only when spectators are allowed. Games
are copied to an independent archive. Optional cleanup of the operative session
is disabled by default and retains archive replay and statistics.

## Map sidebar

The right-hand sidebar keeps the map visible and switches between Contacts,
Details, Fleets, Orders, Relocations and Report. Contacts retain the last
known hostile systems with an estimate or a counted garrison. Select a system or fleet on
the map to inspect it in Details: systems show their known ownership history,
fleets their owner, launch turn, travel time and ETA. The report entry contains
the complete filterable round timeline, including victory and defeat reports.
Submitting a new command opens Orders; resolving a new round opens Report.
Systems mentioned in a report carry a gold marker on the map. Select a report
entry to centre the map on it, or select the marker to return to its report
entry.
Production relocations appear as purple dashed connections with their amount.
Drag one of your systems onto another system to create or replace a relocation;
the dialog limits the amount to the available routing capacity.
All game commands remain available only in a running game.

## Tech stack

- **Java 25** with virtual threads.
- **Spring Boot 4** — web, validation, security, data-jpa, flyway,
  actuator.
- **Vaadin 25 (Flow)** — server-driven UI, language switcher, fog of
  war on an SVG map.
- **Spring Modulith** — module boundaries verified on every test run.
- **PostgreSQL 17** via Flyway migrations and Testcontainers in tests.
- **Gradle (Groovy DSL)** with quality gates: ErrorProne + NullAway,
  SpotBugs, JaCoCo, SonarQube, OpenRewrite.

Architecture details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Screenshots

![Galaxy map with a travelling fleet](docs/screenshots/Karte.png)

*Galaxy map: own systems in blue, neutrals grey, a fleet in transit with
an ETA badge mid-route; systems within sensor range show a rough garrison
estimate, everything beyond stays frozen at its last known state.*

![Lobby view](docs/screenshots/Lobby.png)

*Lobby: running games with host actions, search and status filter,
presence panel, chat, language switcher and a per-user visibility menu.*

![Send-fleet dialog](docs/screenshots/Flotte.png)

*Send-fleet dialog: ship slider with quick buttons (half / double / all /
all except production), travel-time preview, optional "as production transfer"
mode. A configurable garrison reserve stays at the source system.*

Completed games are retained as compact results for the personal statistics
page (`/statistics`), including wins, losses and the record against each opponent.
For newly completed games, the archive map also provides a timeline for replaying
each resolved round.

Direct-message conversations track read state and can be archived independently
by either participant. Messages older than one year can be removed through the
message service without affecting invitations or game archives.

![Round report](docs/screenshots/Runde.png)

*Round report: a filterable timeline of production, reinforcements,
victories, defeats, and systems lost or defended.*

## Run it

Prerequisites: Java 25, Docker Desktop (for PostgreSQL).

```bash
./gradlew bootRun
```

On first start, `spring-boot-docker-compose` brings the Postgres
instance up automatically and supplies its connection details. The local database
port is bound to loopback. Without Docker Compose, set `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` explicitly; the
application has no fallback credentials. Then open
[http://localhost:8080](http://localhost:8080).

For local development with Vaadin hot reload:

```bash
./gradlew bootRun -Pvaadin.productionMode=false
```

The Sonar analysis includes application sources, authored frontend files and
both test source sets. Generated frontend files are excluded. Run all checks
with `./gradlew build e2eTest sonar` against the configured SonarQube server.

## Sign in

On first visit: create an account with email address, public player name and
password, then confirm the email address. Subsequent sessions use the email
address and password. Starfare stores the internal account ID as the player
reference; the public player name is shown in games.

For real verification and password-reset mail, start with the `brevo` profile.
It reads the SMTP login, key and verified sender address from the external file
`~/.config/starfare/brevo.yaml` under `starfare.mail.username`,
`starfare.mail.password` and `starfare.mail.from-address`.

## Language

Top-right in the lobby and the map view there is a language switcher
(German / English). The choice is kept in the Vaadin session and
survives navigation and reload; the default is German.

## Lobby

The landing route `/` is the lobby. From there:

- **New game** — opens the wizard:
    - Galaxy size: 8–120 systems, never fewer than the participating players
    - Up to 8 human players and 7 AI players; at most 10 participants together
    - A colour per seat (duplicates fall back to a free palette entry)
    - Neutral and starting production: 1–20 per turn; home-system ships: 1–50
    - Allow observers? Allow rejoining?
    - Battle presentation on by default; it can be changed again for every round
- **Search / filter** — narrow the list by game or player name, and by
  status: all, mine, open, running or finished games.
- **Join** — take an open slot. One slot per game; first come, first
  served.
- **Observe** — read-only spectator mode (when allowed by the game).
- **Start** — available once all human seats are filled. The initiator
  starts the game.
- **Open map** — for running games you are playing or observing.
- **Abort** — close the game entirely (only meaningful while nobody is
  actively playing).

## Map view (`/map/:gameId`)

Top: **Starfare**, game name, round number and an empire summary with
icons for owned systems, total production per round and total ships
(garrisons plus those en route).

Centre: the galaxy map. Owned systems are in the player's colour,
enemies in the opponent's colour, neutrals grey. Visibility comes in
three levels:

- **Own systems** — exact garrison and production.
- **Within sensor range** — up to two travel rounds from one of your
  systems or a fleet's destination. Owner and colour are live, the
  garrison is only a rough estimate (shown as `~G:`).
- **Everything else** — whatever you last learned in combat there
  (`G:`, with the round it was seen in the tooltip), or nothing at all.

System diameters provide an extra map cue without leaking hidden information:
your systems follow their production, sensor estimates follow estimated
production, and systems without current intelligence all have the same size.

Travelling fleets are rendered as SVG lines with arrowheads; halfway
along sits a small pill with the ship count, whose tooltip shows the
arrival round and remaining turns.

- **Pan**: hold left mouse button and drag.
- **Select system**: click. First click as source, second click on a
  different system as target.
- The initial view centres on your home system.

### Send a fleet

1. Click your source system.
2. Click the target system.
3. Set the ship count via slider or input field. Quick buttons:
   **Half**, **Double**, **All** (max garrison).
4. **Send fleet**.

The order lands in **Planned orders** in the right-hand sidebar. Until the turn ends you
can take it back via **Cancel**.

Ticking **as production transfer** turns the order into a standing one: the
given number of ships is shipped every turn. A system may route at most its
own production plus whatever is routed into it, and several targets share that
budget — the map shows the still-free share in brackets after `P:`. Whatever
is not routed stays behind as garrison. The **Relocations** tab also offers
**Edit** and **Delete** on every route; editing opens the same capacity-aware
dialog.

### Own fleets (table)

Already in transit. Select a row to see its owner, launch round and elapsed
travel time. Columns: No, From, To, Ships, ETA.

- **Wait** — the fleet rests one round at its current position
  (ETA +1).
- **Disband** — the ships are abandoned. Only allowed in certain
  states.

### End the round

Bottom left:

- **Next round** — submits your turn. Once all humans have submitted,
  the turn pipeline runs (production → wait orders →
  arrivals/combat → AI turns → victory check → next round).
  A seat that does not submit within five minutes is handed to the AI
  permanently, so a single idle player cannot stall the game.
- **Leave game** — your seat becomes AI. If rejoining is allowed, you
  can come back later.
- **Back to lobby** — exit the map view; the game keeps running.

After every turn change the app shows a **round report** with the
events (production, combat, conquests). A battle card first only names its
location. Click it for a separate battle playback: both fleet strengths count
down over a sun-and-planet background, with scaled ship markers and optional
sound. The browser activates audio on the first click on a battle card. Use
**Battle presentation for this round** in the report to switch between this
playback and direct results; new rounds begin with the setting chosen in the
game wizard. The result appears only after consciously applying the playback;
until then the map shows a large battle marker and conceals the affected
system's owner and values. Continue via **Back to map**.
Both fleets begin in neutral yellow and turn green or red only with the final
result. A pending map marker keeps neutral systems grey and known player-system
colours faded, without revealing combat details.

## Victory condition

Whoever controls at least 70% of the star systems wins — neutral systems
count towards the total, so a galaxy that is still largely unclaimed
cannot be won. The game ends and a _"Game over. Winner: …"_ message is
shown. The threshold is `GameConfig.VICTORY_SYSTEM_PERCENT`.

## Observer mode

If "allow observers" was checked at game creation, non-participating
users can follow the map live. They select a participant perspective and can
switch the fog of war on or off; spectator views never permit commands. In
pure-AI games the round runs automatically (autoplay) until the last observer leaves or the game
ends.

## Release

`appVersion` in `gradle.properties` drives releases. While it ends in
`-SNAPSHOT`, pushes to `main` only build and test. To release:

1. Remove the `-SNAPSHOT` suffix.
2. Rename `## Unreleased` in `CHANGELOG.md` to `## <appVersion> - <date>`.
3. Commit and push to `main`. The workflow tags `v<appVersion>`, creates a
   GitHub Release with the changelog section and `starfare.jar`, then bumps
   `appVersion` to the next patch `-SNAPSHOT` (`[skip ci]`) — run `git pull`
   before continuing. There is no deployment step.

## Tests

`./gradlew test` runs the unit and integration tests (PostgreSQL via
Testcontainers). `./gradlew e2eTest` runs the Cucumber/Selenium smoke test
against the real application in headless Chrome — register, confirm the
e-mail, log in, create and start a game, play one round. It is not part of
`build`; CI runs it as a separate job (`[skip e2e]` in the commit message
skips it). Failures leave page text and a screenshot in `build/e2e-failures/`.

```bash
./gradlew test
```

Integration tests start a single PostgreSQL container via
Testcontainers; Docker must be running.

## License

[MIT](LICENSE).
