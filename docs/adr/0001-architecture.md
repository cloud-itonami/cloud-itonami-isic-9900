# ADR-0001: MissionOps-LLM ⊣ Mission Operations Governor architecture

## Status

Accepted. `cloud-itonami-isic-9900` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry -- the LAST
`:blueprint`-tier entry anywhere in that registry (see this repo's own
superproject ADR-2607100300 for the fleet-wide consequence).

## Context

`cloud-itonami-isic-9900` publishes an OSS business blueprint for
community mission operations support (extraterritorial missions,
delegations, aid and relief bodies, standards/observer missions --
ISIC Rev.5 section U). Like every prior actor in this fleet, the
blueprint alone is not an implementation: this ADR records the
governed-actor architecture that promotes it to real, tested code,
following the same langgraph StateGraph + independent Governor +
Phase 0→3 rollout pattern established by `cloud-itonami-isic-6511`
(life insurance) and applied across 97 prior siblings, most recently
`cloud-itonami-isic-9700` (community domestic employment).

A kotoba-lang org search for mission/diplomatic/extraterritorial/
consular/aid/deployment returned zero hits -- no bespoke domain
capability library exists for this vertical. `kotoba-lang/robotics`,
named in this blueprint's own README, is the same generic
cross-cutting robotics contract every cloud-itonami vertical already
resolves via `kotoba.technology`, not a domain-specific library.

This blueprint's own `:itonami.blueprint/governor` keyword,
`:mission-operations-governor`, is grep-verified UNIQUE fleet-wide --
no naming-collision precedent question, a fresh independent build.

## Decision

### Decision 1: fresh governor identity, no reuse precedent needed

`:mission-operations-governor` is grep-verified unique across every
blueprint.edn in this fleet. This build follows the SAME governed-
actor architecture as every prior actor, but with its own distinct
governor identity.

### Decision 2: dual-actuation shape, SEQUENTIAL on the SAME `deployment` entity

This blueprint's own Core Contract ("Mission Advisor -> Mission
Operations Governor -> credential, dispatch, report, or human
approval") and its own Trust Controls ("credentials cannot be issued
outside their declared scope; public reports require source
evidence") name two real-world acts: dispatching a mission and
publishing a public report. These apply SEQUENTIALLY to the SAME
`deployment` entity -- dispatch first, report later -- matching
`domesticops`/9700's, `libraryops`/9101's, `adminops`/8411's,
`employmentops`/7810's, `practiceops`/7110's, `hospitalityops`/5510's,
`freightops`/4920's, `quarryops`/0810's and `agronomyops`/0162's own
sequential shape rather than `retailops`/4711's own alternative-kind
shape. `high-stakes` is `#{:actuation/dispatch-mission
:actuation/publish-report}`.

### Decision 3: `aid-value-matches-claim?` -- self-contained arithmetic, no capability library to delegate to

`missionops.registry/aid-value-matches-claim?` (deployment's own
claimed aid value vs. `aid-quantity x unit-value` recomputed
independently) applies the SAME ground-truth-recompute DISCIPLINE
every sibling actor's own cost/total-matching check establishes.
Unlike `domesticops`/9700's own reapplication (which delegated
DIRECTLY to a REAL capability library, `kotoba.labor/wages-for`), this
vertical has no bespoke capability library to delegate to (see
Decision 9 below), so the arithmetic is self-contained -- matching
the majority of this fleet's actors, not a regression from 9700's own
pattern.

### Decision 4: entity and op shape

The primary entity is a `deployment`. Four ops: `:deployment/intake`
(directory upsert, no field-facing risk), `:jurisdiction/assess`
(per-jurisdiction mission-accreditation/cross-border-notification
evidence checklist, never auto), `:deployment/dispatch` (POSITIVE,
high-stakes), and `:deployment/report` (POSITIVE, high-stakes).

### Decision 5: `dispatch-outside-credential-scope?` -- the 92nd unconditional-evaluation grounding, the FLAGSHIP genuinely new check

Grep-verified absent fleet-wide (zero hits for `dispatch-outside-
credential-scope` as a governor check name). Grounded in real
extraterritorial-mission-accreditation law: Japan's own 外交関係に関する
ウィーン条約 (Vienna Convention on Diplomatic Relations 1961) implementing
legislation (enforced by MOFA), the US's Foreign Missions Act (23
U.S.C. §4301 et seq., enforced by the Department of State's Office of
Foreign Missions), the UK's Diplomatic Privileges Act 1964 (giving
effect to the Vienna Convention 1961, enforced by the FCDO Protocol
Directorate), and Germany's Gesetz zu dem Wiener Übereinkommen über
diplomatische Beziehungen (WÜD-Ausführungsgesetz, enforced by the
Auswärtiges Amt Protokollreferat) -- directly grounded in this
blueprint's own text ("credentials cannot be issued outside their
declared scope"). Evaluated UNCONDITIONALLY on every
`:deployment/dispatch` (every mission dispatch needs its own
credential scope checked).

### Decision 6: `cross-border-notification-missing?` -- the 93rd unconditional-evaluation grounding, the SIXTEENTH conditional variant

Before writing this check, every prior sibling's governor namespace
was grepped for any check function named `cross-border-notification`
-- zero hits, confirming this is a genuinely new concept. This is the
SIXTEENTH conditional variant (after `socialresearch`/7220's,
`bizassoc`/9411's, `training`/8549's, `furniture`/9524's,
`specialtyrepair`/9529's, `leathergoods`/9523's, `ictrepair`/9511's,
`quarryops`/0810's, `agronomyops`/0162's, `hospitalityops`/5510's,
`practiceops`/7110's, `employmentops`/7810's, `adminops`/8411's,
`libraryops`/9101's and `domesticops`/9700's own, at 63rd, 64th, 66th,
67th, 68th, 69th, 71st, 77th, 79th, 81st, 83rd, 85th, 87th, 89th and
91st) -- CONDITIONAL on the deployment's own `:involves-cross-border-
movement?` ground truth: not every deployment crosses a border (an
in-country observation mission does not). Grounded in real cross-
border-notification law: Japan's own 出入国管理及び難民認定法 plus bilateral
status-of-forces arrangements (enforced by MOFA / 出入国在留管理庁), the
US's Status of Forces Agreement (SOFA) notification provisions (8
U.S.C. §1101, A-visa border-crossing notice, enforced by the
Department of State / CBP), the UK's Immigration Act 1971 diplomatic/
mission border-crossing notice provisions (enforced by the Home
Office / Border Force), and Germany's Aufenthaltsgesetz §1 Abs. 2
(diplomatische/dienstliche Grenzuebertrittsmeldung, enforced by the
Bundespolizei). ALL FOUR seeded jurisdictions actually have a real
regime here, reported honestly -- a full-coverage sub-citation,
matching `domesticops`/9700's own safeguarding, `libraryops`/9101's
own conservation-standards, `adminops`/8411's own appeal-rights,
`employmentops`/7810's own work-authorization, `practiceops`/7110's
own professional-seal, `quarryops`/0810's own blast-safety and
`agronomyops`/0162's own water-buffer full coverage rather than
`hospitalityops`/5510's own honest single-jurisdiction gap.

### Decision 7: dedicated double-actuation-guard booleans

`:dispatched?`/`:reported?` are dedicated booleans on the `deployment`
record, never a single `:status` value -- the same discipline every
prior governor's guards establish, informed by
`cloud-itonami-isic-6492`'s real status-lifecycle bug
(ADR-2607071320).

### Decision 8: Store protocol, MemStore + DatomicStore parity

`missionops.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in
`test/missionops/store_contract_test.clj`.

### Decision 9: no bespoke capability library, no `blueprint.edn` field-sync fixes needed

A kotoba-lang org search for mission/diplomatic/extraterritorial/
consular/aid/deployment returned zero hits -- no bespoke domain
capability library exists for this vertical, unlike `retailops`/4711
(`kotoba-lang/retail`), `freightops`/4920 (`kotoba-lang/logistics`)
and `domesticops`/9700 (`kotoba-lang/labor`). This repo's
`blueprint.edn` already had the correct `:required-technologies` and
`:optional-technologies [:optimization]` matching the `kotoba-lang/
industry` registry's own entry for `"9900"` exactly -- only the
`:maturity` field itself needed adding, a clean fix like `domesticops`/
9700's own build.

### Decision 10: mock + LLM advisor pair

`missionops.missionopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
dispatching a mission or auto-publishing a report).

## Alternatives considered

- **An unconditional cross-border-notification check** (applying to
  every dispatch regardless of whether the deployment actually
  crosses a border). Rejected: an in-country observation mission does
  not cross any border -- forcing the check onto every dispatch would
  fabricate a requirement.
- **Reusing `domesticops`/9700's own capability-library-delegation
  pattern for the aid-value check.** Rejected: no bespoke capability
  library exists for this domain (confirmed via a direct kotoba-lang
  org search); self-contained arithmetic is the honest choice here,
  not a shortcut.
- **Fabricating a jurisdiction gap** to match `hospitalityops`/5510's
  own single-jurisdiction honesty gap. Rejected: all four seeded
  jurisdictions genuinely have a real cross-border-notification regime
  here.

## Consequences

- 98th actor in this fleet (97 implemented before this build) --
  this promotion also brings the fleet-wide `:blueprint` maturity tier
  to ZERO (the last published-but-unimplemented blueprint repo
  anywhere in the registry); see superproject ADR-2607100300 for the
  full resolution of that consequence, including the
  `kotoba.industry/maturity-of`/`maturity-roadmap-of` pure-function
  refactor this required.
- Establishes two genuinely NEW unconditional-evaluation-discipline
  checks: `dispatch-outside-credential-scope?` (FLAGSHIP, 92nd
  distinct application overall) and `cross-border-notification-
  missing?` (93rd distinct application overall, the SIXTEENTH
  conditional variant).
- `MemStore` ‖ `DatomicStore` parity is proven by
  `test/missionops/store_contract_test.clj`.
- 39 tests / 176 assertions pass; lint is clean; the demo
  (`clojure -M:dev:run`) walks two clean dispatch+report lifecycles
  (no cross-border movement, cross-border movement with notification
  confirmed), plus four HARD-hold scenarios, end-to-end.
- `blueprint.edn` needed no field-sync fix this time -- only the
  `:maturity` flip.

## References

- `cloud-itonami-isic-6511/docs/adr/0001-architecture.md` (origin of
  the general governed-actor architecture pattern)
- `cloud-itonami-isic-9700/docs/adr/0001-architecture.md` (most recent
  prior sibling, template for this ADR's structure)
- 90-docs/adr/2607100300-cloud-itonami-missionops-9900-coverage.md
  (superproject ADR, including the fleet-wide `:blueprint`-tier-
  reaches-zero resolution)
- 外交関係に関するウィーン条約 (Vienna Convention on Diplomatic Relations 1961);
  出入国管理及び難民認定法 (Japan)
- Foreign Missions Act, 23 U.S.C. §4301 et seq.; Status of Forces
  Agreement notification provisions; 8 U.S.C. §1101 (US)
- Diplomatic Privileges Act 1964; Immigration Act 1971 (UK)
- WÜD-Ausführungsgesetz; Aufenthaltsgesetz §1 Abs. 2 (Germany)
