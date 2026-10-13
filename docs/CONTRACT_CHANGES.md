# Contract Change Request Log

This document tracks all proposed modifications to the core contracts (`docs/openapi.yaml`, `docs/schema.sql`, `docs/room-schema.md`, or shared Kotlin interfaces).

**Rules:**
1. Individual feature agents (Agents 1-8) must not alter shared contracts directly.
2. If an agent discovers a required change, it adds a Change Request entry below.
3. Agent 0 (Orchestrator) reviews, approves/rejects, updates the contract files, and notifies the team.

---

## Log of Change Requests

| ID | Date | Requesting Agent | Affected Contract | Summary of Change | Status (PENDING / APPROVED / REJECTED) | Resolution Notes |
|---|---|---|---|---|---|---|
| CR-000 | 2026-09-30 | Agent 0 | Initial Contracts | Baseline contract definitions for Wave 0 | APPROVED | Initialized contracts for Wave 0 review. |

---

## Template for New Requests

```markdown
### CR-[Number]: [Short Title]
- **Date:** YYYY-MM-DD
- **Agent:** Agent X (Role)
- **Target File:** `docs/openapi.yaml` | `docs/schema.sql` | `docs/room-schema.md` | `SharedInterfaces`
- **Description of Change:**
- **Rationale / Justification:**
- **Impact on other agents:**
- **Orchestrator Decision:** PENDING
```
