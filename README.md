# Cross-Border Mobility Compliance

A production-oriented reference for global-mobility compliance: A1 / Certificate of Coverage (CoC), Posted Worker Notification (PWN), visa & immigration, permanent-establishment risk, tax exposure, duty of care, and modular Country Packs (e.g. Singapore MOM/IRAS) — the exact dimensions WorkFlex models in its public compliance handbook.

## Stack (exact JD parity)

- **Backend:** Java 17, Spring Boot 3.3 (REST, validation, actuator), JUnit 5
- **Frontend:** Angular 17 (standalone components), TypeScript, HttpClient
- **API style:** OAuth2 client-credentials compatible (`/api/v1/status`, employees, trips, next-actions, compliance-summary) — mirrors the public WorkFlex Open-API (employee provisioning, trip creation, compliance documents)
- **Infra:** Dockerfile (Eclipse Temurin 17 JRE), GitHub Actions CI (backend-test + frontend-build)

## Covered Countries & Country Packs

| Country | Code | Key Authorities & Frameworks Covered | Rules As Of |
|---|---|---|---|
| Singapore | SG | Ministry of Manpower (MOM) Employment Pass (SGD 5,600/mo), COMPASS Framework (40 pts), IRAS 60-day short-term tax exemption | 2026-11-28 |
| European Union & EEA | DE, FR, NL, ES, IT, etc. | A1 / Certificate of Coverage (Regulation (EC) 883/2004), Posted Worker Notification (Directive 96/71/EC) | 2026-11-28 |

### Not covered yet

- Singapore S Pass quota and monthly tier levy allocations are not yet modeled in the rules engine.
- Singapore Tech.Pass and ONE Pass specialized executive criteria are deferred to enterprise immigration counsel.

## Legal Disclaimer

Not legal or tax advice - check the official source.

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
gradle bootJar --no-daemon
# Frontend
cd frontend && npm ci && npm run build
```

## Tests

- 5 engine/ledger/terminology unit tests
- 5 Singapore MOM/IRAS country pack compliance tests
- 2 controller MockMvc tests (status + next-actions)
- 1 OIDC token validator test
