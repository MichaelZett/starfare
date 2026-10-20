# Ship research experiments

The R1 candidate (`spaceward/1.3.0-r1`) supports one shared ship technology level
per empire. This study compares research budgets while keeping military choices,
production, metal conservation and combat in the actual game implementation.
It is an initial calibration, not a claim of optimal human play.

## Method

The discovery family uses map seeds 42 and 137 with a 150-round limit. The
independent control family uses 19001 and 29003 with a 300-round limit. Both
families cover EVEN/RANDOM layouts, 16/32 systems, combat seeds 314159/271828,
and both starting seats. The research seat always uses CONCENTRATION; opponents
use CONCENTRATION or RUSH with 0% research. Each family therefore has 64 matched
cases per budget, or 384 matches across 0/5/10/15/25/40% research.

Resources use NATURAL environments, combat randomness is 10%, and the individual
victory threshold is 70% of systems. Budget changes occur before the ordinary AI
plans mining, expansion, recycling and fleet orders. No extra knowledge of enemy
resources is supplied. The same per-round combat and resolution-order seeds are
used for every budget on the same scenario. These are paired stochastic scenarios;
paths can diverge after changed orders and combats.

The harness checks legal allocations and recycling reservations and conserves the
original metal ledger after every round. Snapshots are restored every 25 rounds.
Twelve cases per family are replayed completely and compared, including research
and ship generation trajectories. Those 24 repeats are additional to the study
matches. The hash is a within-process repeatability check, not a cross-runtime
save-file checksum. Both full families were subsequently rerun with strict
rejection-checking economy and fleet services: an invalid AI command aborts the
study. All 768 outcomes and every recorded numeric match metric agree with the
initial runs, with no rejected commands or metal ledger failures.

Run either family explicitly; neither is part of normal `test` or `build`:

```sh
./gradlew researchBenchmark -PresearchFamily=discovery
./gradlew researchBenchmark -PresearchFamily=control
```

Raw match records and invocation metadata are written below `build/research/`.
Each CSV records scenario, seat, budget, winner, resolved rounds, research spent,
first technology completion, final technology, hull count, generation-weighted
strength, owned systems and trajectory hash. Generated output is not committed.

## Outcomes

Each cell is wins / losses / still open at the round limit, from the research
seat's perspective. An open game is not a draw or a projected victory.

| Research budget | Discovery, up to 150 rounds | Control, up to 300 rounds |
| --- | --- | --- |
| 0% | 32 / 19 / 13 | 37 / 26 / 1 |
| 5% | 29 / 18 / 17 | 40 / 23 / 1 |
| 10% | 38 / 18 / 8 | 40 / 23 / 1 |
| 15% | 31 / 19 / 14 | 38 / 26 / 0 |
| 25% | 26 / 24 / 14 | 31 / 32 / 1 |
| 40% | 20 / 31 / 13 | 21 / 42 / 1 |

At 10%, nine discovery cases improve relative to their own 0% reference, two get
worse and 53 retain the same result category. In control, six improve, three get
worse and 55 remain unchanged. The ordering for this comparison is win > open >
loss; open results remain censored and should not be read as half a victory.

Against CONCENTRATION specifically, the 0% reference has 11/11/10 discovery
results and 16/16/0 control results. The 10% research seat has 14/11/7 and 17/14/1.
Against RUSH, the reference has 21/8/3 and 21/10/1; 10% produces 24/7/1 and 23/9/0.
The aggregate advantage is not solely caused by the easier opposing profile.

First technology completion is slow at very small budgets. Among matches that
complete at least one level, the median first completion rounds are:

| Budget | Discovery | Control |
| --- | --- | --- |
| 5% | 68 (33/64 complete) | 55 (40/64 complete) |
| 10% | 37 (51/64 complete) | 34 (55/64 complete) |
| 15% | 30.5 (56/64 complete) | 29 (61/64 complete) |
| 25% | 23 (58/64 complete) | 22 (63/64 complete) |
| 40% | 17 (59/64 complete) | 17 (64/64 complete) |

Whole-empire integer rounding also matters: a 5% budget reserves no points below
20 installed industry, and 10% reserves none below 10. A selected percentage does
not guarantee immediate progress in a small opening empire.

These conditional medians omit games ending or reaching the limit before a
completion. They are not an unconditional research completion forecast. The
768 study matches resolve 68,062 rounds; 84 remain open, and 86 control matches
continue beyond round 150. The highest observed technology is level 6.

## Calibration and limits

Small research budgets can be viable against a fleet-focused opponent. Heavy
continuous investment is costly: reaching technology earlier does not recover
the lost early hulls and territory in most tested cases. Existing ships retain
their generation, so the gain also depends on subsequent construction. A 10%
bonus to new hulls cannot be treated as a 10% bonus to the entire existing fleet.

The regular AI budget is reduced from 15% to 10%; RUSH retains its conservative
5% policy. Human empires still start at 0% and choose their own budget. The cost
curve, 10% strength increase per level, build timing, level ceiling and W6/W7
rules are unchanged. The data support this modest baseline, not a uniquely best
budget: 5% ties 10% in the control family and individual paired cases reverse.

Research is an alternative to producing more physical hulls, including when
metal constrains further construction. This study does not establish an optimal
late-game switch or an adaptive policy. It tests fixed budgets against 0% research,
not a matrix of research-aware opponents. Military AI still makes its existing
hull-count decisions; it is not a technology-aware tactical optimizer. Other
resource environments, maps larger than 32 systems, multiplayer diplomacy,
quality-dependent research, more opening policies and higher technology levels
need separate experiments. Human gameplay acceptance remains pending.

## Browser acceptance and fixes

Four real-browser journeys passed: research, the Spaceward creation wizard,
existing phone navigation and direct production planning. Research acceptance
uses a persisted two-human-seat fixture near level completion and runs with
both the ordinary W6/W7 catalog and the opt-in research catalog. It checks desktop
and touch-emulated phone editing, reload persistence across both sessions,
visible and reachable phone controls without horizontal page overflow,
read-only budget after submission, the completion report and mixed old/new hulls
in the following production round. The separate wizard journey creates R1 through
the opt-in catalog and exercises the ordinary economy. Phone emulation uses
Chromium at 390×844 CSS pixels, not a physical iPhone or Safari.

Two defects were corrected during acceptance. A conquest could shift research
rounding in both empires while only one empire's plans had been clamped, producing
negative shipbuilding during refresh. All affected plans are now clamped before
outputs refresh; the failing seeded game is a regression test. The economy notice
bar also mislabeled research reservations as missing workers; its labor warning
now requires an actual labor limit.

The final Java suite has 939 tests, with no failures or skips. The full build,
Error Prone/NullAway, SpotBugs (zero findings), coverage gates and both frontend
check files pass.

Screenshots are generated at `build/ui-acceptance/research-desktop.png`,
`build/ui-acceptance/research-phone.png` and
`build/ui-acceptance/research-mixed-desktop.png`. Run the focused journeys with:

```sh
./gradlew e2eTest -PresearchPreview=true \
  '-Dcucumber.filter.tags=@research or @economy-launch or @mobile or @production-planning'
```
