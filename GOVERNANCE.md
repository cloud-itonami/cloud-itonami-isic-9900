# Governance

`cloud-itonami-9900` is an OSS open-business blueprint for community mission
operations support, robotics-premised.

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a robot action the governor refuses is never dispatched to hardware.
- the Mission Operations Governor remains independent of the advisor.
- hard policy violations (out-of-scope credential, force-dispatch, evidenceless report) cannot be overridden by human approval.
- every dispatch, sign-off, credential and report path is auditable.
- sensitive dispatch and personnel data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model, storage contract, public business model, operator certification or license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a separate trust mark and should require security, robot-safety, audit and data-flow review.

Certified operators can lose certification for:
- bypassing robot-safety or credential-scope checks
- mishandling dispatch or personnel data
- misrepresenting certification status
- failing to respond to security or safety incidents
