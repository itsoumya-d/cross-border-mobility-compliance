# Cross-Border Mobility Compliance

A production-oriented reference for global-mobility compliance: A1 / Certificate of Coverage (CoC), Posted Worker Notification (PWN), visa & immigration, permanent-establishment risk, tax exposure and duty of care — the exact dimensions WorkFlex models in its public compliance handbook.

## Stack (exact JD parity)

- **Backend:** Java 17, Spring Boot 3.3 (REST, validation, actuator), JUnit 5
- **Frontend:** Angular 17 (standalone components), TypeScript, HttpClient
- **API style:** OAuth2 client-credentials compatible (`/api/v1/status`, employees, trips, next-actions, compliance-summary) — mirrors the public WorkFlex Open-API (employee provisioning, trip creation, compliance documents)
- **Infra:** Dockerfile (Eclipse Temurin 17 JRE), GitHub Actions CI (backend-test + frontend-build)

## The `rea` fix — traveler next-action inbox

G2 reviewers (4.8/5, 27 reviews) consistently flag two UX gaps:

1. **Credit / charging logic is not intuitive** on first use.
2. **Next required action is not transparent** for first-time travelers; standardised wording does not match clients' internal terminology.

This repo ships a fix for both:

- `ComplianceEngine.assess()` returns an ordered `NextAction` list with plain-language title, internal → WorkFlex term mapping, owner, due date and document type.
- `TerminologyMap` maps internal vocabulary (`WFA`, `PWN`, `A1 cert`) to canonical WorkFlex terms.
- `CreditLedger.price()` returns a full transparent breakdown (base + compliance + documents) with an explanation string.
- Angular `TravelerChecklistComponent` renders the next-action inbox with badge, due date and required flag.

## Run

```bash
# Backend
gradle test --no-daemon
gradle bootRun --no-daemon
# Frontend
cd frontend && npm ci && npm run build
```

## Tests

- 5 engine/ledger/terminology unit tests
- 2 controller MockMvc tests (status + next-actions)

## Truthfulness boundary

Rule set is deterministic and illustrative — it does not file A1/PWN applications with real authorities and does not constitute legal advice. Production filing requires licensed compliance counsel and integration with national portals (e.g. EasyGov.swiss for Switzerland, Altinn for Norway).
