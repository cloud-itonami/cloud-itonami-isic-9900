# Contributing

`cloud-itonami-9900` accepts contributions to the OSS blueprint, capability
bindings, policy tests, documentation and operator model.

## Development
The capability layer lives in `kotoba-lang/robotics`. This repo holds the
business blueprint and operator contracts.

```bash
clojure -X:test
clojure -M:lint
```

## Rules
- Do not commit real personnel, credential or dispatch data.
- Keep robot dispatch, credentials and reports behind the Mission Operations
  Governor.
- Treat mission workflows as high-risk: add tests for robot-safety gating,
  credential scope, evidence, disclosure and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
