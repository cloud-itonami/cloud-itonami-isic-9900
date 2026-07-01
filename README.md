# cloud-itonami-9900

Open Business Blueprint for **ISIC Rev.5 9900**: activities of
extraterritorial organizations and bodies (missions, delegations, aid and
relief bodies, standards/observer missions).

This repository designs a forkable OSS business for community mission
operations support: credential and dispatch management, robotics-assisted
aid delivery and observation, and public reporting — run by a qualified
operator so a mission keeps its own dispatch and reporting records instead
of renting a closed operations platform.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (aid-delivery, logistics,
environmental observation, demining-survey) operate under an actor that
proposes actions and an independent **Mission Operations Governor** that
gates them. The governor never dispatches hardware itself; `:high`/
`:safety-critical` actions (cross-border movement, operating near civilians,
handling sensitive cargo) require human sign-off.

## Core Contract

```text
intake + identity + credential + robot mission
        |
        v
Mission Advisor -> Mission Operations Governor -> credential, dispatch, report, or human approval
        |
        v
robot actions (gated) + dispatch record + public report + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, issue
a credential outside its scope, or publish a report without governor
approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `9900`). Implemented by:

- [`kotoba-lang/robotics`](https://github.com/kotoba-lang/robotics) — missions, actions, safety-stops, telemetry proofs

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
