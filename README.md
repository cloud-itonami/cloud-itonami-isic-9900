# cloud-itonami-isic-9900

Open Business Blueprint for **ISIC Rev.5 9900**: activities of
extraterritorial organizations and bodies (missions, delegations, aid
and relief bodies, standards/observer missions).

This repository publishes a community-mission-operations actor --
deployment intake, per-jurisdiction mission-accreditation/cross-
border-notification regulatory assessment, mission dispatch and
public-report publication -- as an OSS business that any qualified
operator can fork, deploy, run, improve and sell, so a mission
operator never surrenders dispatch and reporting data to a closed
operations platform.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet (97 prior actors) -- here it is
**MissionOps-LLM ⊣ Mission Operations Governor**. This blueprint's own
`:itonami.blueprint/governor` keyword, `:mission-operations-governor`,
is a UNIQUE keyword fleet-wide (grep-verified: no other blueprint
declares it) -- a fresh, independent build.

> **Why an actor layer at all?** An LLM is great at drafting a
> deployment summary, normalizing records, and checking whether a
> claimed aid value actually equals aid-quantity times unit-value --
> but it has **no notion of which jurisdiction's mission-
> accreditation/cross-border-notification law is official, no license
> to dispatch a real mission into the field or publish a real public
> report, and no way to know on its own whether a proposed dispatch
> actually falls within the mission's own declared credential scope or
> whether a deployment that actually crosses a border has actually
> completed a required cross-border notification**. Letting it
> dispatch or publish directly invites fabricated regulatory
> citations, an aid-value mismatch being published, a dispatch outside
> the mission's own accredited scope, and an unnotified border
> crossing -- exposing the mission to real diplomatic/legal liability.
> This project seals the MissionOps-LLM into a single node and wraps
> it with an independent **Mission Operations Governor**, a human
> **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers deployment intake through mission-accreditation/
cross-border-notification regulatory assessment, mission dispatch and
public-report publication. It does **not**, by itself, hold any
operating authority required to conduct extraterritorial mission
operations in a given jurisdiction, and it does not claim to. It also
does not perform the actual physical field work itself, or judge
mission fit -- `missionops.registry/aid-value-matches-claim?` is a
pure ground-truth recompute against the deployment's own recorded
fields, not a fit judgment. Whoever deploys and operates a live
instance (a qualified mission-operations coordinator) supplies any
jurisdiction-specific accreditation, the real service-robot
integration and the real diplomatic-notification integrations, and
bears that jurisdiction's liability -- the software supplies the
governed, spec-cited, audited execution scaffold so that operator does
not have to build the compliance layer from scratch.

### Actuation

**Dispatching a real mission into the field and publishing a real
public report are never autonomous, at any phase, by construction.**
Two independent layers enforce this (`missionops.governor`'s
`:actuation/dispatch-mission`/`:actuation/publish-report` high-stakes
gate and `missionops.phase`'s phase table, which never puts either op
in any phase's `:auto` set) -- see `missionops.phase`'s docstring and
`test/missionops/phase_test.clj`'s `deployment-dispatch-never-auto-
at-any-phase`/`deployment-report-never-auto-at-any-phase`. The actor
may draft, check and recommend; a human mission-operations coordinator
is always the one who actually dispatches a mission or publishes a
report. Grounded directly in this blueprint's own `docs/business-
model.md` Trust Controls text ("credentials cannot be issued outside
their declared scope; public reports require source evidence") -- a
genuine DUAL-actuation shape, applied SEQUENTIALLY to the SAME
deployment record (dispatch first, report later), matching
`domesticops`/9700's, `libraryops`/9101's, `adminops`/8411's,
`employmentops`/7810's, `practiceops`/7110's, `hospitalityops`/5510's,
`freightops`/4920's, `quarryops`/0810's and `agronomyops`/0162's own
sequential shape rather than `retailops`/4711's own alternative-kind
shape.

## The core contract

```
deployment intake + jurisdiction facts (missionops.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ MissionOps-LLM        │ ─────────────▶ │ Mission Operations Governor  │  (independent system)
   │ (sealed)              │  + citations    │ spec-basis · evidence-       │
   └───────────────────────┘                 │ incomplete · dispatch-        │
          │                 commit ◀┼ outside-credential-scope (FLAGSHIP    │
          │                         │ NEW) · aid-value-mismatch ·                │
    record + ledger        escalate ┼ cross-border-notification-missing         │
          │              (ALWAYS for│ (conditional, NEW) · already-             │
          │       :actuation/       │ dispatched · already-reported             │
          │       dispatch-mission/ │                                            │
          │       :actuation/       │                                            │
          │       publish-report}    │                                            │
          ▼                          └───────────────────────┘
      human approval
```

**The MissionOps-LLM never dispatches a mission or publishes a report
the Mission Operations Governor would reject, and never does so
without a human sign-off.** Hard violations (fabricated regulatory
requirements; unsupported evidence; a dispatch outside credential
scope; an aid-value mismatch; a missing cross-border notification on a
cross-border deployment; a double dispatch/report) force **hold** and
*cannot* be approved past; a clean dispatch/report proposal still
always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk two clean dispatch+report lifecycles (no cross-border movement, cross-border movement with notification confirmed), plus four HARD-hold cases, through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (aid-delivery,
logistics, environmental observation, demining-survey) operate under
the actor, gated by the independent **Mission Operations Governor**.
The governor never dispatches hardware itself; `:high`/`:safety-
critical` actions (cross-border movement, operating near civilians,
handling sensitive cargo) require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Mission Operations Governor, dispatch/report draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`9900`). Implemented by:

- [`kotoba-lang/robotics`](https://github.com/kotoba-lang/robotics) --
  missions, actions, safety-stops, telemetry proofs (the same generic
  cross-cutting robotics contract every cloud-itonami vertical uses;
  no bespoke domain capability library exists for this vertical -- a
  kotoba-lang org search for mission/diplomatic/extraterritorial/
  consular/aid/deployment returned zero hits)

A live sample of the operator console (robotics safety console) is
rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html)
-- pure-data HTML output of `kotoba.robotics.ui`.

## Layout

| File | Role |
|---|---|
| `src/missionops/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + dispatch AND report history (dual history). The double-actuation guard checks dedicated `:dispatched?`/`:reported?` booleans rather than a `:status` value |
| `src/missionops/registry.cljc` | Dispatch/report draft records, plus `aid-value-matches-claim?` -- self-contained ground-truth recompute (no bespoke capability library exists for this domain) |
| `src/missionops/facts.cljc` | Per-jurisdiction mission-accreditation AND cross-border-notification catalog with an official spec-basis citation per entry, honest coverage reporting -- ALL FOUR seeded jurisdictions have a cross-border-notification sub-citation here |
| `src/missionops/missionopsllm.cljc` | **MissionOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/jurisdiction-assessment/dispatch/report proposals |
| `src/missionops/governor.cljc` | **Mission Operations Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · dispatch-outside-credential-scope, FLAGSHIP NEW, the 92nd unconditional-evaluation-discipline grounding · aid-value-mismatch · cross-border-notification-missing, CONDITIONAL, the 93rd grounding) + 2 double-actuation guards + 1 soft (confidence/actuation gate) |
| `src/missionops/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted assess → supervised (dispatch/report always human; deployment intake is the ONLY auto-eligible op, no direct field-facing risk) |
| `src/missionops/operation.cljc` | **OperationActor** -- langgraph StateGraph |
| `src/missionops/sim.cljc` | demo driver |
| `test/missionops/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers deployment intake through mission-accreditation/
cross-border-notification regulatory assessment, mission dispatch and
public-report publication -- the core governed lifecycle this
blueprint's own `docs/business-model.md` names in its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Deployment intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:deployment/intake`/`:jurisdiction/assess`) | Real service-robot integration, real mission-fit judgment (see `missionops.facts`'s docstring) |
| Mission dispatch, HARD-gated on full evidence, credential scope and (when applicable) cross-border notification, plus a double-dispatch guard (`:actuation/dispatch-mission`) | |
| Public-report publication, HARD-gated on full evidence and a matching aid-value claim, plus a double-report guard (`:actuation/publish-report`) | |
| Immutable audit ledger for every intake/assessment/dispatch/report decision | |

Extending coverage is additive: add the next gate (e.g. a sensitive-
cargo-manifest-verification check) as its own governed op with its own
HARD checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world act"
pattern this repo's flagship ops already establish.

## Jurisdiction coverage (honest)

`missionops.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `missionops.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `missionops.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger. Note that the cross-border-notification
sub-citation is FULL coverage rather than a gap: ALL FOUR seeded
jurisdictions (JPN, USA, GBR, DEU) actually have a real cross-border-
notification enforcement regime, reported honestly.

## Maturity

`:implemented` -- `MissionOps-LLM` + `Mission Operations Governor` run
as real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, following the SAME governed-
actor architecture as the 97 other prior actors across this fleet,
with its own distinct, independently-named governor. This promotion
was the LAST `:blueprint`-tier entry anywhere in the `kotoba-lang/
industry` registry -- see `docs/adr/0001-architecture.md` for the
history and design, and the superproject's own ADR-2607100300 for the
fleet-wide consequence.

## License

Code and implementation templates are AGPL-3.0-or-later.
